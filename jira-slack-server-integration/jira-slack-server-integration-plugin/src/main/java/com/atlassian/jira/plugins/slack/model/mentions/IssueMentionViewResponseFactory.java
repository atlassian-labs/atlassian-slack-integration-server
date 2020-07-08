package com.atlassian.jira.plugins.slack.model.mentions;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.plugins.slack.mentions.storage.cache.MentionChannelCacheManager;
import com.atlassian.jira.plugins.slack.mentions.storage.cache.MentionUserCacheManager;
import com.atlassian.jira.plugins.slack.model.ChannelKeyImpl;
import com.atlassian.jira.plugins.slack.model.UserIdImpl;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atlassian.plugins.slack.util.SlackHelper.escapeSignsForSlackLink;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;

/**
 * Json storage object containing all the Slack mentions for a Jira issue in one channel
 */
@Service
public class IssueMentionViewResponseFactory {
    private final MentionChannelCacheManager channelCache;
    private final MentionUserCacheManager userCache;
    private final DateTimeFormatter dateTimeFormatter;

    @Autowired
    public IssueMentionViewResponseFactory(final MentionChannelCacheManager channelCache,
                                           final MentionUserCacheManager userCache,
                                           final DateTimeFormatter dateTimeFormatter) {
        this.channelCache = checkNotNull(channelCache);
        this.userCache = checkNotNull(userCache);
        this.dateTimeFormatter = dateTimeFormatter.forLoggedInUser().withStyle(DateTimeStyle.RSS_RFC822_DATE_TIME);
    }

    public List<IssueMentionViewItem> createResponse(
            final List<IssueMention> issueMentions,
            final String userKey) {
        return issueMentions.stream()
                .sorted(new IssueMentionComparator())
                .flatMap(issueMention -> fetchDetails(issueMention, userKey))
                .collect(Collectors.toList());
    }

    private Stream<IssueMentionViewItem> fetchDetails(final IssueMention issueMention,
                                                      final String userKey) {
        final Optional<MentionChannel> mentionChannel = channelCache.get(new ChannelKeyImpl(
                userKey,
                issueMention.getTeamId(),
                issueMention.getChannelId()));
        final MentionUser mentionUser = userCache.get(new UserIdImpl(
                issueMention.getTeamId(),
                issueMention.getUserId()))
                .orElse(MentionUser.DELETED_USER);

        if (!mentionChannel.isPresent()) {
            return Stream.empty();
        }

        final String localizedMessageDate = dateTimeFormatter.format(issueMention.getDateTime());

        // enrich message with user mentions
        final Pattern p = Pattern.compile("<@([A-Z0-9]+)>", Pattern.CASE_INSENSITIVE);
        final Matcher m = p.matcher(issueMention.getMessageText());
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String userId = m.group(1);

            String mentionedUserName;
            try {
                mentionedUserName = userCache
                        .get(new UserIdImpl(issueMention.getTeamId(), userId))
                        .map(user -> "|" + escapeSignsForSlackLink(user.getDisplayName()))
                        .orElse("");
            } catch (Exception e) {
                mentionedUserName = "";
            }
            m.appendReplacement(sb, "<@" + userId + mentionedUserName + ">");
        }
        m.appendTail(sb);

        return Stream.of(new IssueMentionViewItem(
                localizedMessageDate,
                new MentionMessage(
                        issueMention.getTeamId(),
                        issueMention.getChannelId(),
                        sb.toString(),
                        issueMention.getUserId(),
                        issueMention.getMessageId()
                ),
                mentionChannel.get(),
                mentionUser));
    }

    private static class IssueMentionComparator implements Comparator<IssueMention> {
        @Override
        public int compare(IssueMention im1, IssueMention im2) {
            int result = -im1.getDateTime().compareTo(im2.getDateTime());
            if (result != 0) {
                return result;
            }
            result = im1.getChannelId().compareTo(im2.getChannelId());
            if (result != 0) {
                return result;
            }
            return im1.getMessageId().compareTo(im2.getMessageId());
        }
    }

    /**
     * An issue mention with all details returned from the REST resource
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(fieldVisibility = ANY)
    public static class IssueMentionViewItem {
        private final MentionMessage message;
        private final MentionChannel channel;
        private final MentionUser user;
        private final String localizedMessageDate;

        IssueMentionViewItem(String localizedMessageDate,
                             MentionMessage message,
                             MentionChannel channel,
                             MentionUser user) {
            checkArgument(!isNullOrEmpty(localizedMessageDate));
            this.localizedMessageDate = localizedMessageDate;
            this.message = checkNotNull(message);
            this.channel = checkNotNull(channel);
            this.user = checkNotNull(user);
        }

        public MentionMessage getMessage() {
            return message;
        }

        public MentionChannel getChannel() {
            return channel;
        }

        public MentionUser getUser() {
            return user;
        }

        @SuppressWarnings("unused")
        public String getLocalizedMessageDate() {
            return localizedMessageDate;
        }
    }
}
