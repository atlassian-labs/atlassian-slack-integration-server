package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.annotations.VisibleForTesting;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.plugins.slack.manager.DedicatedChannelManager;
import com.atlassian.jira.plugins.slack.manager.IssueDetailsMessageManager;
import com.atlassian.jira.plugins.slack.manager.PluginConfigurationManager;
import com.atlassian.jira.plugins.slack.manager.ProjectConfigurationManager;
import com.atlassian.jira.plugins.slack.mentions.storage.cache.MentionChannelCacheManager;
import com.atlassian.jira.plugins.slack.model.ChannelKey;
import com.atlassian.jira.plugins.slack.model.ChannelKeyImpl;
import com.atlassian.jira.plugins.slack.model.DedicatedChannel;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.analytics.DedicatedChannelIssueMentionedEvent;
import com.atlassian.jira.plugins.slack.model.analytics.JiraNotificationSentEvent;
import com.atlassian.jira.plugins.slack.model.analytics.JiraNotificationSentEvent.Type;
import com.atlassian.jira.plugins.slack.model.event.IssueMentionedEvent;
import com.atlassian.jira.plugins.slack.model.event.JiraCommandEvent;
import com.atlassian.jira.plugins.slack.model.event.ShowIssueEvent;
import com.atlassian.jira.plugins.slack.model.event.UnauthorizedUnfurlEvent;
import com.atlassian.jira.plugins.slack.model.mentions.MentionChannel;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.github.seratch.jslack.api.model.Conversation;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Event listener for Slack Jira issue mention events
 */
@Service
@Slf4j
public class SlackEventHandlerService {
    private final EventPublisher eventPublisher;
    private final AsyncExecutor asyncExecutor;
    private final TaskBuilder taskBuilder;
    private final IssueManager issueManager;
    private final ApplicationProperties applicationProperties;
    private final ProjectConfigurationManager projectConfigurationManager;
    private final IssueDetailsMessageManager issueDetailsMessageManager;
    private final DedicatedChannelManager dedicatedChannelManager;
    private final SlackLinkManager slackLinkManager;
    private final MentionChannelCacheManager mentionChannelCacheManager;
    private final PluginConfigurationManager pluginConfigurationManager;
    private final SlackUserManager slackUserManager;
    private final UserManager userManager;
    private final PermissionManager permissionManager;
    private final AnalyticsContextProvider analyticsContextProvider;

    private final boolean skipStoringPrivateIssueMentions;

    /**
     * This string is the regex used to extract issue keys and Jira path from webhook messages.
     * If matches, it capture 2 groups:
     * 1. Jira URL before the issue key (https://some.jira.com/browse/); this part is optional and may match to empty string
     * 2. Issue key (TST-12)
     */
    private static final String ISSUE_REF_REGEX = "([^\\s<(\\[,|!]*?(?:^|\\s|[^A-Z_0-9a-z-]))([A-Za-z][A-Za-z_0-9]*-\\d+)(?![A-Z_0-9a-z-])";
    private static final Pattern ISSUE_REF_PATTERN = Pattern.compile(ISSUE_REF_REGEX);
    /**
     * This pattern extracts a Jira URL from the group matched by the first capture in the regex above.
     */
    private static final Pattern JIRA_URL_PATTERN = Pattern.compile("(?:https|http)://\\S*/(?:browse|issues)/");

    @Autowired
    SlackEventHandlerService(final EventPublisher eventPublisher,
                             final AsyncExecutor asyncExecutor,
                             final TaskBuilder taskBuilder,
                             final IssueManager issueManager,
                             final ApplicationProperties applicationProperties,
                             final ProjectConfigurationManager projectConfigurationManager,
                             final IssueDetailsMessageManager issueDetailsMessageManager,
                             final DedicatedChannelManager dedicatedChannelManager,
                             final SlackLinkManager slackLinkManager,
                             final MentionChannelCacheManager mentionChannelCacheManager,
                             final PluginConfigurationManager pluginConfigurationManager,
                             final SlackUserManager slackUserManager,
                             @Qualifier("jiraUserManager") final UserManager userManager,
                             final PermissionManager permissionManager,
                             final AnalyticsContextProvider analyticsContextProvider) {
        this.eventPublisher = eventPublisher;
        this.asyncExecutor = asyncExecutor;
        this.taskBuilder = taskBuilder;
        this.issueManager = issueManager;
        this.applicationProperties = applicationProperties;
        this.projectConfigurationManager = projectConfigurationManager;
        this.issueDetailsMessageManager = issueDetailsMessageManager;
        this.dedicatedChannelManager = dedicatedChannelManager;
        this.slackLinkManager = slackLinkManager;
        this.mentionChannelCacheManager = mentionChannelCacheManager;
        this.pluginConfigurationManager = pluginConfigurationManager;
        this.slackUserManager = slackUserManager;
        this.userManager = userManager;
        this.permissionManager = permissionManager;
        this.analyticsContextProvider = analyticsContextProvider;
        this.skipStoringPrivateIssueMentions = Boolean.getBoolean("slack.skip.private.mention");
    }

    public boolean handleMessage(@Nonnull final SlackIncomingMessage message) {
        checkNotNull(message, "Null slackEvent received.");

        // skip if the bot is the author itself
        if (message.getSlackLink().getBotUserId().equals(message.getUser())) {
            return false;
        }

        final Set<String> previousIssueKeys = extractIssueKeys(message.getPreviousText(), Collections.emptyList()).stream()
                .map(IssueReference::getKey)
                .collect(Collectors.toSet());
        boolean hasFoundAnyIssue = false;

        final Optional<SlackUser> slackUser = findSlackUser(message.getUser());

        // build information about source channel
        String teamId = message.getTeamId();
        String channelId = message.getChannelId();
        ConversationKey conversationKey = new ConversationKey(teamId, channelId);
        ChannelKey cacheKey = new ChannelKeyImpl(slackUser.map(SlackUser::getUserKey).orElse(""), message.getTeamId(), channelId);
        Optional<Conversation> conversation = mentionChannelCacheManager.get(cacheKey)
                .map(MentionChannel::getConversation);
        // if no conversation is found then consider it private
        boolean isPrivate = conversation
                .map(conv -> conv.isPrivate() || conv.isIm() || conv.isMpim())
                .orElse(true);
        boolean handlingLinkSharedEvent = message.isLinkShared() && !isPrivate;
        boolean shouldUseLinkUnfurl = slackLinkManager.shouldUseLinkUnfurl(message.getTeamId());
        boolean allowLinkUnfurling = handlingLinkSharedEvent || (!message.isLinkShared() && isPrivate) || !shouldUseLinkUnfurl;
        log.debug("Unfurling details: conversation={} handlingLinkSharedEvent={} shouldUseLinkUnfurl={} allowLinkUnfurling={}",
                conversation.orElse(null), handlingLinkSharedEvent, shouldUseLinkUnfurl, allowLinkUnfurling);

        // We iterate over all the matching cause maybe there are more than one issue in the message
        List<IssueReference> issueReferences = extractIssueKeys(message.getText(), message.getLinks());
        log.debug("Found {} issue references", issueReferences.size());

        final List<Pair<JiraCommandEvent, NotificationInfo>> unfurlNotificationInfos = new ArrayList<>();
        boolean isInvitationToUserSent = false;
        for (IssueReference issueReference : issueReferences) {
            String key = issueReference.getKey();
            log.debug("Processing issue key {}", key);
            final MutableIssue issue = issueManager.getIssueByCurrentKey(key);

            if (issue == null) {
                log.debug("No issue found for key {}", key);
                continue;
            }

            // if bot or connected user is in the channel, posted link to issue cause 2 webhooks to be triggered:
            // 'link_shared' and 'message'. in order to not send duplicate notifications we handle
            // * 'link_shared' event only in public channels
            // * 'message' event in private channel (requires bot to be invited)
            if (issueReference.hasUrl() && !allowLinkUnfurling) {
                log.debug("Skipping link unfurling for issue {}", key);
                continue;
            }
            final boolean isProjectAutoConvertEnabled = projectConfigurationManager.isProjectAutoConvertEnabled(issue.getProjectObject());
            // user will get just one invite even if there are multiple issue references in the message
            boolean isUserAllowedToSeeIssue = true;
            if (!isSenderAllowedToSeeIssue(message.getChannelId(), slackUser, issue)) {
                if (!isInvitationToUserSent && !slackUser.isPresent()) {
                    if (isProjectAutoConvertEnabled) {
                        inviteUserToConnectToSlack(message, issue);
                        isInvitationToUserSent = true;
                    }
                }
                isUserAllowedToSeeIssue = false;
            }

            boolean isExternallyShared = conversation
                    .map(conv -> conv.isExtShared() || conv.isPendingExtShared())
                    .orElse(false);
            boolean isMutedExternallyShared = conversation
                    .map(conv -> isExternallyShared && !pluginConfigurationManager.isIssuePreviewForGuestChannelsEnabled())
                    .orElse(false);
            boolean willPostPublicNotification = !message.isSlashCommand() && !isPrivate && !isMutedExternallyShared;
            boolean shouldStoreMention = !skipStoringPrivateIssueMentions && isNotBlank(message.getUser())
                    && !message.isSlashCommand() && !isMutedExternallyShared;

            // save or update issue mention message
            if (shouldStoreMention) {
                asyncExecutor.run(taskBuilder.newProcessIssueMentionTask(issue, message));
            }

            // do not send repeated edits
            if (!message.isMessageEdit() || !previousIssueKeys.contains(key)) {
                final Optional<DedicatedChannel> dedicatedChannel = dedicatedChannelManager.getDedicatedChannel(issue);


                if (isProjectAutoConvertEnabled && !isMutedExternallyShared && isUserAllowedToSeeIssue) {
                    hasFoundAnyIssue = true;

                    // for link unfurling do not send a separate notification for each parsed issue link
                    // in this case unfurling attachment will be overwritten
                    // instead collect all the notification information and send in one request in the end of the method
                    if (handlingLinkSharedEvent) {
                        final NotificationInfo notificationInfo = new NotificationInfo(
                                message.getSlackLink(),
                                channelId,
                                message.getResponseUrl(),
                                message.getThreadTs(),
                                null,
                                message.getTs(),
                                message.getUser(),
                                issueReference.getUrl(),
                                Verbosity.EXTENDED);
                        final JiraCommandEvent event = new ShowIssueEvent(issue, dedicatedChannel.orElse(null));

                        unfurlNotificationInfos.add(Pair.of(event, notificationInfo));
                    } else {
                        final NotificationInfo notificationInfo = new NotificationInfo(
                                message.getSlackLink(),
                                channelId,
                                message.getResponseUrl(),
                                message.getThreadTs(),
                                null,
                                null,
                                message.getUser(),
                                null,
                                Verbosity.EXTENDED);
                        issueDetailsMessageManager.sendIssueDetailsMessageToChannel(
                                notificationInfo,
                                issue,
                                dedicatedChannel.orElse(null));
                    }
                }

                if (willPostPublicNotification) {
                    final boolean showDedicatedChannel = dedicatedChannelManager.isNotSameChannel(
                            conversationKey,
                            dedicatedChannel);

                    if (dedicatedChannel.isPresent() && showDedicatedChannel) {
                        slackLinkManager.getLinkByTeamId(dedicatedChannel.get().getTeamId())
                                .forEach(link -> sendMessageToDedicatedChannel(
                                        dedicatedChannel.get(),
                                        message,
                                        new NotificationInfo(
                                                link,
                                                dedicatedChannel.get().getChannelId(),
                                                null,
                                                null,
                                                dedicatedChannel.get().getCreator(),
                                                Verbosity.EXTENDED),
                                        slackUser.orElse(null)));
                    }
                }
            }
        }

        // send one unfurling notification for all parsed issue links
        if (handlingLinkSharedEvent && !unfurlNotificationInfos.isEmpty()) {
            unfurlNotificationInfos.forEach(info -> eventPublisher.publish(
                    new JiraNotificationSentEvent(analyticsContextProvider.byTeamIdAndSlackUserId(
                            info.getRight().getLink().getTeamId(), info.getRight().getMessageAuthorId()), null, Type.UNFURLING)));
            asyncExecutor.run(taskBuilder.newUnfurlIssueLinksTask(unfurlNotificationInfos));
        }

        return hasFoundAnyIssue;
    }

    private void sendMessageToDedicatedChannel(final DedicatedChannel dedicatedChannel,
                                               final SlackIncomingMessage message,
                                               final NotificationInfo notificationInfo,
                                               final SlackUser slackUser) {
        eventPublisher.publish(new JiraNotificationSentEvent(AnalyticsContext.fromSlackUser(slackUser),
                null, Type.DEDICATED));

        final JiraCommandEvent event = new IssueMentionedEvent(message, dedicatedChannel.getIssueId());
        asyncExecutor.run(taskBuilder.newSendNotificationTask(event, notificationInfo, asyncExecutor));
        eventPublisher.publish(new DedicatedChannelIssueMentionedEvent(AnalyticsContext.fromSlackUser(slackUser),
                message.getChannelId(), dedicatedChannel.getChannelId(), dedicatedChannel.getIssueId()));
    }

    @VisibleForTesting
    List<IssueReference> extractIssueKeys(final String message, final List<String> urls) {
        final ImmutableSet.Builder<IssueReference> processedRefs = ImmutableSet.builder();

        if (!isBlank(message)) {
            final Matcher issueRefMatcher = ISSUE_REF_PATTERN.matcher(message);
            while (issueRefMatcher.find()) {
                String issueLinkOrKey = issueRefMatcher.group(0);
                String prefix = issueRefMatcher.group(1);
                Matcher urlMatcher = JIRA_URL_PATTERN.matcher(prefix);
                boolean hasUrl = urlMatcher.find();
                if (!hasUrl || prefixIsOurJIRAUrl(urlMatcher.group(0))) {
                    String issueKey = issueRefMatcher.group(2).toUpperCase();
                    processedRefs.add(new IssueReference(issueKey, hasUrl ? issueLinkOrKey : null));
                }
            }
        }

        urls.stream()
                .filter(this::prefixIsOurJIRAUrl)
                .filter(url -> url.contains("/browse/") || url.contains("/issues/"))
                .forEach(url -> {
                    final Matcher issueRefMatcher = ISSUE_REF_PATTERN.matcher(url);
                    if (issueRefMatcher.find()) {
                        processedRefs.add(new IssueReference(issueRefMatcher.group(2).toUpperCase(), url));
                    }
                });

        return new ArrayList<>(processedRefs.build());
    }

    private boolean prefixIsOurJIRAUrl(final String match) {
        return match.startsWith(applicationProperties.getBaseUrl(UrlMode.CANONICAL) + "/");
    }

    private boolean isSenderAllowedToSeeIssue(final String channelId,
                                              final Optional<SlackUser> slackUserOptional,
                                              final Issue issue) {
        boolean itIsDedicatedChannelForTheIssue = dedicatedChannelManager.getDedicatedChannel(issue)
                .map(channel -> channelId.equals(channel.getChannelId()))
                .orElse(false);
        if (itIsDedicatedChannelForTheIssue) {
            return true;
        }

        boolean userHasPermissionsToSeeIssue = slackUserOptional
                .map(slackUser -> userManager.getUserByKey(slackUser.getUserKey()))
                .map(user -> permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user))
                .orElse(false);

        return userHasPermissionsToSeeIssue;
    }

    private Optional<SlackUser> findSlackUser(final String slackUserId) {
        return slackUserManager.getBySlackUserId(slackUserId);
    }

    private void inviteUserToConnectToSlack(final SlackIncomingMessage message,
                                            final Issue issue) {
        NotificationInfo notificationInfo = new NotificationInfo(
                message.getSlackLink(),
                message.getChannelId(),
                message.getResponseUrl(),
                message.getThreadTs(),
                null,
                message.getTs(),
                message.getUser(),
                null,
                Verbosity.EXTENDED);
        AnalyticsContext context = analyticsContextProvider.byTeamIdAndSlackUserId(message.getTeamId(), message.getUser());
        UnauthorizedUnfurlEvent event = new UnauthorizedUnfurlEvent(context, issue.getProjectId(), issue.getKey(),
                message.getChannelId(), null);
        asyncExecutor.run(taskBuilder.newDirectMessageTask(event, notificationInfo));
    }
}
