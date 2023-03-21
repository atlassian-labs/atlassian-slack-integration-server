package com.atlassian.jira.plugins.slack.postfunction;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.plugins.slack.model.analytics.JiraNotificationSentEvent;
import com.atlassian.jira.plugins.slack.model.analytics.JiraNotificationSentEvent.Type;
import com.atlassian.jira.plugins.slack.model.event.DefaultJiraPostFunctionEvent;
import com.atlassian.jira.plugins.slack.service.issuefilter.impl.JqlIssueFilter;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.loader.ActionDescriptor;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import io.atlassian.fugue.Functions;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.atlassian.jira.workflow.JiraWorkflow.ACTION_ORIGIN_STEP_ID;

/**
 * Post function for workflows. It will add a post function to the workflow to send a notification to specific Slack
 * channels
 */
@RequiredArgsConstructor
public class SlackPostFunction extends AbstractJiraFunctionProvider {
    private static final Logger logger = LoggerFactory.getLogger(SlackPostFunction.class);
    private static final String DESCRIPTOR = "descriptor";
    private static final String ACTION_ID = "actionId";
    private static final String ORIGINAL_ISSUE_OBJECT = "originalissueobject";

    private final TaskBuilder taskBuilder;
    private final AsyncExecutor asyncExecutor;
    private final JqlIssueFilter issueFilter;
    private final SlackLinkManager slackLinkManager;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    @Override
    public void execute(final Map transientVars, final Map args, final PropertySet ps) {
        try {
            final Map<String, SlackLink> linkByTeamId = slackLinkManager.getLinks().stream().collect(Collectors.toMap(
                    SlackLink::getTeamId,
                    Functions.identity()
            ));

            // Don't bother with all the processing if Slack is not configured.
            if (linkByTeamId.isEmpty()) {
                return;
            }

            final Issue issue = getIssue(transientVars);
            final WorkflowDescriptor descriptor = (WorkflowDescriptor) transientVars.get(DESCRIPTOR);
            final Integer actionId = (Integer) transientVars.get(ACTION_ID);
            final Issue originalIssue = (Issue) transientVars.get(ORIGINAL_ISSUE_OBJECT);
            final ActionDescriptor action = descriptor.getAction(actionId);
            final String firstStepName = originalIssue == null ? "" : originalIssue.getStatus().getName();
            final String endStepName = stayedInCurrentStep(action)
                    ? firstStepName
                    : descriptor.getStep(action.getUnconditionalResult().getStep()).getName();
            final String actionName = action.getName();

            final List<ChannelToNotifyDto> channelsToNotifyIds = ChannelToNotifyDto.fromJson(
                    Strings.nullToEmpty((String) args.get(SlackPostFunctionFactory.CHANNELS_TO_NOTIFY_JSON_PARAM)));

            final Set<ChannelToNotifyDto> uniqueChannels = Sets.newHashSet(channelsToNotifyIds);

            final String owner = (String) args.get(SlackPostFunctionFactory.OWNER_PARAM);
            final List<NotificationInfo> notificationInfos = uniqueChannels.stream()
                    .filter(channel -> linkByTeamId.get(channel.getTeamId()) != null)
                    .map(channel -> new NotificationInfo(
                            linkByTeamId.get(channel.getTeamId()),
                            channel.getChannelId(),
                            null,
                            null,
                            owner,
                            Verbosity.EXTENDED))
                    .collect(Collectors.toList());

            final Optional<ApplicationUser> caller = getUser(transientVars, args);

            final String jql = (String) args.get(SlackPostFunctionFactory.JQL_FILTER_PARAM);
            final String customMessageFormat = (String) args.get(SlackPostFunctionFactory.MESSAGE_FILTER_PARAM);

            try {
                if (hasErrors(issue, jql)) {
                    runSendNotificationTask(issue, caller, firstStepName, endStepName, actionName, notificationInfos,
                            customMessageFormat, true);
                } else if (issueFilter.matchesJql(jql, issue, caller)) {
                    runSendNotificationTask(issue, caller, firstStepName, endStepName, actionName, notificationInfos,
                            customMessageFormat, false);
                }
            } catch (SearchException e) {
                logger.error("Exception running JQL", e);
            }
        } catch (Exception e) {
            // We don't want a failure in this postfunction to block the workflow
            logger.error("Exception running the Slack post function", e);
        }
    }

    private boolean stayedInCurrentStep(final ActionDescriptor action) {
        return action.getUnconditionalResult().getStep() == ACTION_ORIGIN_STEP_ID;
    }

    private void runSendNotificationTask(final Issue issue,
                                         final Optional<ApplicationUser> caller,
                                         final String firstStepName,
                                         final String endStepName,
                                         final String actionName,
                                         final List<NotificationInfo> notificationInfos,
                                         final String customMessageFormat,
                                         final boolean havingError) {
        try {
            DefaultJiraPostFunctionEvent postFunctionEvent =
                    new DefaultJiraPostFunctionEvent.Builder().setIssue(issue)
                            .setActor(caller.orElse(null))
                            .setFirstStepName(firstStepName)
                            .setEndStepName(endStepName)
                            .setActionName(actionName)
                            .setCustomMessageFormat(customMessageFormat)
                            .setHavingErrors(havingError)
                            .build();
            if (!notificationInfos.isEmpty()) {
                notificationInfos.forEach(notification -> {
                    String notificationKey = "MATCHER:ISSUE_TRANSITIONED";
                    if (caller.isPresent()) {
                        eventPublisher.publish(new JiraNotificationSentEvent(analyticsContextProvider.byTeamIdAndUserKey(
                                notification.getLink().getTeamId(), caller.get().getKey()), notificationKey,
                                Type.POST_FUNCTION));
                    } else {
                        eventPublisher.publish(new JiraNotificationSentEvent(analyticsContextProvider.bySlackLink(
                                notification.getLink()), notificationKey, Type.POST_FUNCTION));
                    }
                });
                taskBuilder.newSendNotificationTask(postFunctionEvent, notificationInfos, asyncExecutor).run();
            }
        } catch (Exception e) {
            logger.warn("Exception trying to send information to Slack", e);
        }
    }

    private Optional<ApplicationUser> getUser(final Map transientVars, final Map args) {
        return Optional.ofNullable(getCallerUser(transientVars, args));
    }

    private boolean hasErrors(final Issue issue, final String jql) {
        return !isIssueStoredInDatabase(issue) && !Strings.isNullOrEmpty(jql);
    }

    private boolean isIssueStoredInDatabase(final Issue issue) {
        return issue.getId() != null;
    }
}
