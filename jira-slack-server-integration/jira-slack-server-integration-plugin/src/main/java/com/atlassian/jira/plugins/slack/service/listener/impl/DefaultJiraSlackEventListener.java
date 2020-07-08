package com.atlassian.jira.plugins.slack.service.listener.impl;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bulkedit.operation.BulkEditTaskContext;
import com.atlassian.jira.event.issue.DelegatingJiraIssueEvent;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.IssueEventBundle;
import com.atlassian.jira.event.issue.JiraIssueEvent;
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
import com.atlassian.jira.plugins.slack.service.task.TaskExecutorService;
import com.atlassian.jira.plugins.slack.settings.JiraSettingsService;
import com.atlassian.jira.task.TaskContext;
import com.atlassian.jira.task.TaskDescriptor;
import com.atlassian.jira.task.TaskManager;
import com.atlassian.plugin.slack.jira.compat.Jira8IssueEventWrapper;
import com.atlassian.plugin.slack.jira.compat.WithJira8;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class DefaultJiraSlackEventListener extends AutoSubscribingEventListener implements JiraSlackEventListener {
    private final TaskExecutorService taskExecutorService;
    private final TaskBuilder taskBuilder;
    private final IssueEventToEventMatcherTypeConverter issueEventToEventMatcherTypeConverter;
    private final DedicatedChannelManager dedicatedChannelManager;
    private final IssueEventProcessorService issueEventProcessorService;
    private final JiraSettingsService jiraSettingsService;
    private final TaskManager taskManager;
    private final PersonalNotificationManager personalNotificationManager;
    private final AnalyticsContextProvider analyticsContextProvider;
    private final AsyncExecutor asyncExecutor;

    @Autowired
    public DefaultJiraSlackEventListener(final EventPublisher eventPublisher,
                                         final TaskExecutorService taskExecutorService,
                                         final TaskBuilder taskBuilder,
                                         final IssueEventToEventMatcherTypeConverter issueEventToEventMatcherTypeConverter,
                                         final DedicatedChannelManager dedicatedChannelManager,
                                         final IssueEventProcessorService issueEventProcessorService,
                                         final JiraSettingsService jiraSettingsService,
                                         final TaskManager taskManager,
                                         final PersonalNotificationManager personalNotificationManager,
                                         final AnalyticsContextProvider analyticsContextProvider,
                                         final AsyncExecutor asyncExecutor) {
        super(eventPublisher);
        this.taskExecutorService = taskExecutorService;
        this.taskBuilder = taskBuilder;
        this.issueEventToEventMatcherTypeConverter = issueEventToEventMatcherTypeConverter;
        this.dedicatedChannelManager = dedicatedChannelManager;
        this.issueEventProcessorService = issueEventProcessorService;
        this.jiraSettingsService = jiraSettingsService;
        this.taskManager = taskManager;
        this.personalNotificationManager = personalNotificationManager;
        this.analyticsContextProvider = analyticsContextProvider;
        this.asyncExecutor = asyncExecutor;
    }

    @EventListener
    public void issueEvent(@Nonnull final IssueEventBundle eventBundle) {
        try {
            Set<EventMatcherType> eventsSeen = new HashSet<>();
            for (JiraIssueEvent event : eventBundle.getEvents()) {
                if (!(event instanceof DelegatingJiraIssueEvent)) {
                    // We can get the IssueEvent only from a DelegatingJiraIssueEvent.
                    continue;
                }
                final IssueEvent issueEvent = ((DelegatingJiraIssueEvent) event).asIssueEvent();

                // check if it's bulk edit event and user selected to skip sending notifications
                final String userKey = issueEvent.getUser().getKey();
                if (jiraSettingsService.areBulkNotificationsMutedForUser(new UserKey(userKey)) && isBulkEdit(issueEvent)) {
                    log.debug("Skipping handling of the event {} for user {} because he chose to suppress notifications from bulk operations",
                            issueEvent.getEventTypeId(), userKey);
                    return;
                }

                // Running processing in main thread because it has some user context attached that is used
                // when matching an issue against JQL;
                // running this operation in background thread causes missing notifications on JSD tickets operations
                asyncExecutor.run(() -> {
                    Issue issue = issueEvent.getIssue();
                    log.debug("Processing event id={} for issue key={}, id={}", issueEvent.getEventTypeId(),
                            issue.getKey(), issue.getId());
                    processProjectNotifications(issueEvent, eventsSeen);
                });
            }
        } catch (Exception e) {
            log.error("Error processing event", e);
        }
    }

    private void processProjectNotifications(final IssueEvent issueEvent, final Set<EventMatcherType> eventsSeen) {
        final Collection<EventMatcherType> eventMatcherTypes = issueEventToEventMatcherTypeConverter.match(issueEvent);

        // Multiple events can be fired for a given {@link IssueEvent}, multiple notifications
        // can be sent as a result so they are filtered below
        for (EventMatcherType eventMatcherType : eventMatcherTypes) {
            if (eventsSeen.contains(eventMatcherType)) {
                break;
            }

            eventsSeen.add(eventMatcherType);
            final DefaultJiraIssueEvent ourEventToProcess = buildIssueEvent(eventMatcherType, issueEvent);
            final Optional<NotificationInfo> dedicatedChannelNotification = dedicatedChannelManager
                    .getNotificationsFor(ourEventToProcess);
            final Collection<NotificationInfo> projectNotifications = issueEventProcessorService
                    .getNotificationsFor(ourEventToProcess);
            final List<NotificationInfo> personalNotifications = personalNotificationManager
                    .getNotificationsFor(ourEventToProcess);

            final List<NotificationInfo> uniqueNotifications = dedupNotificationsByChannel(dedicatedChannelNotification,
                    projectNotifications, personalNotifications);

            if (!uniqueNotifications.isEmpty()) {
                dedicatedChannelNotification.ifPresent(notification ->
                        eventPublisher.publish(new JiraNotificationSentEvent(analyticsContextProvider.bySlackLink(
                                notification.getLink()), eventMatcherType.getDbKey(), Type.DEDICATED)));
                projectNotifications.forEach(notification ->
                        eventPublisher.publish(new JiraNotificationSentEvent(analyticsContextProvider.bySlackLink(
                                notification.getLink()), eventMatcherType.getDbKey(), Type.REGULAR)));
                personalNotifications.forEach(notification ->
                        eventPublisher.publish(new JiraNotificationSentEvent(analyticsContextProvider.bySlackLink(
                                notification.getLink()), eventMatcherType.getDbKey(), Type.PERSONAL)));
                taskBuilder.newSendNotificationTask(ourEventToProcess, uniqueNotifications, taskExecutorService).call();
            }
        }
    }

    private List<NotificationInfo> dedupNotificationsByChannel(final Optional<NotificationInfo> dedicatedChannelNotification,
                                                               final Collection<NotificationInfo> projectNotifications,
                                                               final List<NotificationInfo> personalNotifications) {
        final Map<String, NotificationInfo> notificationByChannel = new HashMap<>();

        Consumer<NotificationInfo> putByChannelId = info -> notificationByChannel.putIfAbsent(info.getChannelId(), info);
        projectNotifications.forEach(putByChannelId);
        personalNotifications.forEach(putByChannelId);
        dedicatedChannelNotification.ifPresent(putByChannelId);

        return new ArrayList<>(notificationByChannel.values());
    }

    private DefaultJiraIssueEvent buildIssueEvent(EventMatcherType eventMatcherType, IssueEvent event) {
        return new DefaultJiraIssueEvent.Builder()
                .setEventMatcher(eventMatcherType)
                .setIssueEvent(event)
                .build();
    }

    private boolean isBulkEdit(final IssueEvent issueEvent) {
        boolean isBulkEdit = false;
        if (WithJira8.isJira8OrGreater()) {
            try {
                Optional<Boolean> isSpanningOperation = WithJira8.withJira8(
                        () -> Jira8IssueEventWrapper.isSpanningOperation(issueEvent));
                isBulkEdit = isSpanningOperation.orElse(false);
            } catch (Exception e) {
                log.warn("Failed to detect spanning operation", e);
            }
        } else {
            Collection<TaskDescriptor<?>> liveTasks = taskManager.getLiveTasks();
            List<TaskContext> contexts = liveTasks.stream()
                    .map(TaskDescriptor::getTaskContext)
                    .collect(Collectors.toList());
            log.trace("Live tasks contexts: {}", contexts);
            if (contexts.stream().anyMatch(context -> context instanceof BulkEditTaskContext)) {
                isBulkEdit = true;
            }
        }

        if (isBulkEdit) {
            log.trace("Bulk operation detected");
        }

        return isBulkEdit;
    }
}
