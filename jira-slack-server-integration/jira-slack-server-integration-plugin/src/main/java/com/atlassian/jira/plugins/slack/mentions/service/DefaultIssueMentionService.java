package com.atlassian.jira.plugins.slack.mentions.service;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.slack.mentions.storage.cache.MentionChannelCacheManager;
import com.atlassian.jira.plugins.slack.mentions.storage.json.IssueMentionStore;
import com.atlassian.jira.plugins.slack.model.SlackIncomingMessage;
import com.atlassian.jira.plugins.slack.model.mentions.IssueMention;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.events.SlackUserMappedEvent;
import com.atlassian.plugins.slack.api.events.SlackUserUnmappedEvent;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService;
import com.atlassian.plugins.slack.api.webhooks.ChannelDeletedSlackEvent;
import com.atlassian.plugins.slack.event.SlackTeamUnlinkedEvent;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import io.atlassian.fugue.Either;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.substringBefore;

/**
 * Cached implementation of the {@link com.atlassian.jira.plugins.slack.service.mentions.IssueMentionService}
 */
@Service
public class DefaultIssueMentionService extends AutoSubscribingEventListener implements IssueMentionService {
    private final IssueMentionStore issueMentionStore;
    private final SlackLinkManager slackLinkManager;
    private final MentionChannelCacheManager channelCache;

    @Autowired
    public DefaultIssueMentionService(final IssueMentionStore issueMentionStore,
                                      final MentionChannelCacheManager channelCache,
                                      final SlackLinkManager slackLinkManager,
                                      final EventPublisher eventPublisher) {
        super(eventPublisher);
        this.channelCache = channelCache;
        this.issueMentionStore = checkNotNull(issueMentionStore);
        this.slackLinkManager = checkNotNull(slackLinkManager);
    }

    /**
     * When a channel is deleted we will remove all the mentions made in that channel from the IssueMentionStore,
     * remove the cached channel from the MentionChannelCacheManager and
     * then remove the messages which contained the mentions from the MentionMessageCacheManager
     */
    @EventListener
    public void onChannelDeletedEvent(final ChannelDeletedSlackEvent e) {
        final String channelId = e.getChannel();
        final String teamId = e.getSlackEvent().getTeamId();
        channelCache.deleteAll();
        issueMentionStore.deleteAllByPropertyKey(new ConversationKey(teamId, channelId).toStringKey());
    }

    @EventListener
    public void onUserLinked(final SlackUserMappedEvent e) {
        channelCache.deleteAll();
    }

    @EventListener
    public void onUserUnlinked(final SlackUserUnmappedEvent e) {
        channelCache.deleteAll();
    }

    @EventListener
    public void onTeamDisconnection(final SlackTeamUnlinkedEvent event) {
        String teamId = event.getTeamId();
        List<IssueMention> mentions = issueMentionStore.findByPredicate(mention -> teamId.equals(mention.getTeamId()));
        mentions.forEach(mention -> issueMentionStore.deleteAllByPropertyKey(mention.getKey()));
    }

    @Override
    public void deleteMessageMention(@Nonnull final ConversationKey conversationKey, final String messageTimestamp) {
        checkNotNull(conversationKey, "conversationKey is null.");
        checkNotNull(messageTimestamp, "messageTimestamp is null.");

        issueMentionStore.deleteAllByPropertyKey(conversationKey.toStringKey() + messageTimestamp);
    }

    @Override
    public void issueMentioned(@Nonnull final Issue issue, @Nonnull final SlackIncomingMessage message) {
        checkNotNull(issue, "issue is null.");
        checkNotNull(message, "message is null.");
        checkNotNull(message.getUser(), "message.user is null.");
        checkNotNull(message.getTs(), "message.ts is null.");

        final Date messageDateTime = new Date(Long.parseLong(cleanDateTimeString(message.getTs())) * 1000L);
        String text = message.getText();
        if (StringUtils.isBlank(text)) {
            text = message.getLinks().stream()
                    .collect(Collectors.joining(", "));
        }
        final IssueMention mention = new IssueMention(
                issue.getId(),
                message.getTeamId(),
                message.getChannelId(),
                message.getTs(),
                text,
                message.getUser(),
                messageDateTime);

        issueMentionStore.put(issue.getId(), mention.getKey(), Optional.of(mention));
    }

    private static String cleanDateTimeString(final String dateTimeStr) {
        return substringBefore(dateTimeStr, ".");
    }

    @Override
    @Nonnull
    public Either<Throwable, List<IssueMention>> getIssueMentions(final long issueId) {
        if (!slackLinkManager.isAnyLinkDefined()) {
            return Either.left(
                    new IllegalStateException("Slack link not installed."));
        }

        try {
            List<IssueMention> mentions = issueMentionStore.getAll(issueId);
            return Either.right(mentions);
        } catch (RuntimeException ex) {
            return Either.left(ex);
        }
    }
}
