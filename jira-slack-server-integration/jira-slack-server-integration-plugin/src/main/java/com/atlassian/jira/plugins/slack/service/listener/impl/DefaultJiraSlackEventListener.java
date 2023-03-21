package com.atlassian.jira.plugins.slack.service.listener.impl;

import com.atlassian.annotations.VisibleForTesting;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.DelegatingJiraIssueEvent;
import com.atlassian.jira.event.issue.IssueChangedEvent;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.IssueEventBundle;
import com.atlassian.jira.event.issue.JiraIssueEvent;
import com.atlassian.jira.event.operation.SpanningOperation;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.manager.DedicatedChannelManager;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.analytics.JiraNotificationSentEvent;
import com.atlassian.jira.plugins.slack.model.analytics.JiraNotificationSentEvent.Type;
import com.atlassian.jira.plugins.slack.model.event.DefaultJiraIssueEvent;
import com.atlassian.jira.plugins.slack.service.listener.IssueEventToEventMatcherTypeConverter;
import com.atlassian.jira.plugins.slack.service.listener.JiraSlackEventListener;
import com.atlassian.jira.plugins.slack.service.notification.IssueEventProcessorService;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.notification.PersonalNotificationManager;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.plugins.slack.settings.JiraSettingsService;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogExtractor;
import com.atlassian.jira.plugins.slack.util.changelog.ChangeLogItem;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.atlassian.sal.api.user.UserKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Service
@Slf4j
public class DefaultJiraSlackEventListener extends AutoSubscribingEventListener implements JiraSlackEventListener {
    private final TaskBuilder taskBuilder;
    private final IssueEventToEventMatcherTypeConverter issueEventToEventMatcherTypeConverter;
    private final DedicatedChannelManager dedicatedChannelManager;
    private final IssueEventProcessorService issueEventProcessorService;
    private final JiraSettingsService jiraSettingsService;
    private final PersonalNotificationManager personalNotificationManager;
    private final AnalyticsContextProvider analyticsContextProvider;
    private final AsyncExecutor asyncExecutor;
    private final ChangeLogExtractor changeLogExtractor;

    @Autowired
    public DefaultJiraSlackEventListener(final EventPublisher eventPublisher,
                                         final TaskBuilder taskBuilder,
                                         final IssueEventToEventMatcherTypeConverter issueEventToEventMatcherTypeConverter,
                                         final DedicatedChannelManager dedicatedChannelManager,
                                         final IssueEventProcessorService issueEventProcessorService,
                                         final JiraSettingsService jiraSettingsService,
                                         final PersonalNotificationManager personalNotificationManager,
                                         final AnalyticsContextProvider analyticsContextProvider,
                                         final AsyncExecutor asyncExecutor,
                                         final ChangeLogExtractor changeLogExtractor) {
        super(eventPublisher);
        this.taskBuilder = taskBuilder;
        this.issueEventToEventMatcherTypeConverter = issueEventToEventMatcherTypeConverter;
        this.dedicatedChannelManager = dedicatedChannelManager;
        this.issueEventProcessorService = issueEventProcessorService;
        this.jiraSettingsService = jiraSettingsService;
        this.personalNotificationManager = personalNotificationManager;
        this.analyticsContextProvider = analyticsContextProvider;
        this.asyncExecutor = asyncExecutor;
        this.changeLogExtractor = changeLogExtractor;
    }

    @EventListener
    public void issueEvent(@Nonnull final IssueEventBundle eventBundle) {
        try {
            Set<EventMatcherType> eventsSeen = new HashSet<>();
            for (JiraIssueEvent event : eventBundle.getEvents()) {
                if (!(event instanceof DelegatingJiraIssueEvent)) {
                    // We can get the IssueEvent only from a DelegatingJiraIssueEvent.
                    log.debug("Skipping processing event {} since it's not a DelegatingJiraIssueEvent", event);
                    continue;
                }
                final IssueEvent issueEvent = ((DelegatingJiraIssueEvent) event).asIssueEvent();

                // check if it's bulk edit event and user selected to skip sending notifications
                final String userKey = issueEvent.getUser().getKey();
                if (jiraSettingsService.areBulkNotificationsMutedForUser(new UserKey(userKey)) && isBulkEdit(issueEvent.getSpanningOperation())) {
                    log.debug("Skipping handling of the event {} for user {} because he chose to suppress notifications from bulk operations",
                            issueEvent.getEventTypeId(), userKey);
                    return;
                }

                // Running processing in main thread because it has some user context attached that is used
                // when matching an issue against JQL;
                // running this operation in background thread causes missing notifications on JSD tickets operations
                asyncExecutor.run(taskBuilder.newThreadLocalAwareTask(() -> {
                    Issue issue = issueEvent.getIssue();
                    log.debug("Processing eventTypeId={} for issue key={}, id={}", issueEvent.getEventTypeId(),
                            issue.getKey(), issue.getId());
                    processIssueEvent(issueEvent, eventsSeen);
                }));
            }
        } catch (Exception e) {
            log.error("Error processing event", e);
        }
    }

    private void processIssueEvent(final IssueEvent issueEvent, final Set<EventMatcherType> eventsSeen) {
        final Collection<EventMatcherType> eventMatcherTypes = issueEventToEventMatcherTypeConverter.match(issueEvent);
        final Issue issue = issueEvent.getIssue();

        log.debug("Event matcher types for eventTypeId={}, issue key={}, id={}: {}", issueEvent.getEventTypeId(), issue.getKey(),
                issue.getId(), eventMatcherTypes);

        // Multiple events can be fired for a given {@link IssueEvent}, multiple notifications
        // can be sent as a result so they are filtered below
        for (EventMatcherType eventMatcherType : eventMatcherTypes) {
            if (eventsSeen.contains(eventMatcherType)) {
                log.debug("Skipping processing matcher {} since it has been already processed for eventTypeId={}, issue key={}, id={}",
                        eventMatcherType, issueEvent.getEventTypeId(), issue.getKey(), issue.getId());
                break;
            }

            eventsSeen.add(eventMatcherType);
            List<ChangeLogItem> changeLog = changeLogExtractor.getChanges(issueEvent);
            final DefaultJiraIssueEvent internalEventWrapper = DefaultJiraIssueEvent.of(eventMatcherType, issueEvent, changeLog);
            sendNotifications(internalEventWrapper);
        }
    }

    private void sendNotifications(final DefaultJiraIssueEvent internalIssueEvent) {
        final Optional<NotificationInfo> dedicatedChannelNotification = dedicatedChannelManager
                .getNotificationsFor(internalIssueEvent);
        final Collection<NotificationInfo> projectNotifications = issueEventProcessorService
                .getNotificationsFor(internalIssueEvent);
        final List<NotificationInfo> personalNotifications = personalNotificationManager
                .getNotificationsFor(internalIssueEvent);

        final List<NotificationInfo> uniqueNotifications = dedupNotificationsByChannel(dedicatedChannelNotification,
                projectNotifications, personalNotifications);
        final Issue issue = internalIssueEvent.getIssue();
        log.debug("Unique notifications to send for issue key={}, id={}, source={}: {}", issue.getKey(), issue.getId(),
                internalIssueEvent.getSource(), uniqueNotifications.size());

        if (!uniqueNotifications.isEmpty()) {
            String notificationKey = internalIssueEvent.getEventMatcher().getDbKey();
            dedicatedChannelNotification.ifPresent(notification ->
                    eventPublisher.publish(new JiraNotificationSentEvent(analyticsContextProvider.bySlackLink(
                            notification.getLink()), notificationKey, Type.DEDICATED)));
            projectNotifications.forEach(notification ->
                    eventPublisher.publish(new JiraNotificationSentEvent(analyticsContextProvider.bySlackLink(
                            notification.getLink()), notificationKey, Type.REGULAR)));
            personalNotifications.forEach(notification ->
                    eventPublisher.publish(new JiraNotificationSentEvent(analyticsContextProvider.bySlackLink(
                            notification.getLink()), notificationKey, Type.PERSONAL)));
            taskBuilder.newSendNotificationTask(internalIssueEvent, uniqueNotifications, asyncExecutor).run();
        }
    }

    @VisibleForTesting
    List<NotificationInfo> dedupNotificationsByChannel(final Optional<NotificationInfo> dedicatedChannelNotification,
                                                       final Collection<NotificationInfo> projectNotifications,
                                                       final List<NotificationInfo> personalNotifications) {
        final Map<String, NotificationInfo> notificationByChannel = new HashMap<>();

        Consumer<NotificationInfo> putByChannelId = info -> notificationByChannel.putIfAbsent(info.getChannelId(), info);
        projectNotifications.forEach(putByChannelId);
        dedicatedChannelNotification.ifPresent(putByChannelId);
        personalNotifications.forEach(notificationInfo -> {
            // channel ID for personal notifications is not known at this point,
            // it will be retrieved from Slack using conversations.open method in Slack client;
            // so dedup by user ID instead
            String channelIdStub = "dm-" + notificationInfo.getUserId();
            notificationByChannel.putIfAbsent(channelIdStub, notificationInfo);
        });

        return new ArrayList<>(notificationByChannel.values());
    }

    private boolean isBulkEdit(Optional<SpanningOperation> spanningOperation) {
        boolean isBulkEdit = spanningOperation.isPresent();
        if (isBulkEdit) {
            log.trace("Bulk operation detected");
        }

        return isBulkEdit;
    }

    @EventListener
    public void onIssueChangeEvent(IssueChangedEvent issueChangedEvent) {
        // check if it's bulk edit event and user selected to skip sending notifications
        final String userKey = issueChangedEvent.getAuthor().map(ApplicationUser::getKey).orElse(null);
        if (userKey != null
                && jiraSettingsService.areBulkNotificationsMutedForUser(new UserKey(userKey))
                && isBulkEdit(issueChangedEvent.getSpanningOperation())) {
            log.debug("Skipping handling of the IssueChangedEvent for user {} because he chose to suppress notifications from bulk operations",
                    userKey);
            return;
        }

        asyncExecutor.run(taskBuilder.newThreadLocalAwareTask(() -> {
            Issue issue = issueChangedEvent.getIssue();
            log.debug("Processing IssueChangedEvent for issue key={}, id={}", issue.getKey(), issue.getId());

            Collection<EventMatcherType> matchers = issueEventToEventMatcherTypeConverter.match(issueChangedEvent);
            log.debug("Event matcher types for issue key={}: {}", issue.getKey(), matchers);

            for (EventMatcherType matcher : matchers) {
                DefaultJiraIssueEvent internalEventWrapper = DefaultJiraIssueEvent.of(matcher, issueChangedEvent);
                sendNotifications(internalEventWrapper);
            }
        }));
    }
}
