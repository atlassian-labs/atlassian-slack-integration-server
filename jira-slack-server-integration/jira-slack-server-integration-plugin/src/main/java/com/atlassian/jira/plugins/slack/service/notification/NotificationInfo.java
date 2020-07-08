package com.atlassian.jira.plugins.slack.service.notification;

import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import lombok.Value;

@Value
public class NotificationInfo {
    SlackLink link;
    String channelId;
    String userId;
    String responseUrl;
    String threadTimestamp;
    String configurationOwner;
    String messageTimestamp;
    String messageAuthorId;
    String issueUrl;
    boolean isPersonal;
    boolean isEphemeral;
    Verbosity verbosity;

    public NotificationInfo(final SlackLink link,
                            final String channelId,
                            final String responseUrl,
                            final String threadTimestamp,
                            final String configurationOwner,
                            final Verbosity verbosity) {
        this(link, channelId, null, responseUrl, threadTimestamp, configurationOwner, null, null, null, false, false, verbosity);
    }

    public NotificationInfo(final SlackLink link,
                            final String channelId,
                            final String userId,
                            final boolean isEphemeral) {
        this(link, channelId, userId, null, null, null, null, null, null, false, isEphemeral, Verbosity.EXTENDED);
    }

    public NotificationInfo(final SlackLink link,
                            final String channelId,
                            final String responseUrl,
                            final String threadTimestamp,
                            final String configurationOwner,
                            final String messageTimestamp,
                            final String messageAuthorId,
                            final String issueUrl,
                            final Verbosity verbosity) {
        this(link, channelId, null, responseUrl, threadTimestamp, configurationOwner, messageTimestamp, messageAuthorId, issueUrl, false, false, verbosity);
    }

    public NotificationInfo(final SlackLink link,
                            final String channelId,
                            final String userId,
                            final String responseUrl,
                            final String threadTimestamp,
                            final String configurationOwner,
                            final String messageTimestamp,
                            final String messageAuthorId,
                            final String issueUrl,
                            final boolean isPersonal,
                            final boolean isEphemeral,
                            final Verbosity verbosity) {
        this.link = link;
        this.channelId = channelId;
        this.userId = userId;
        this.responseUrl = responseUrl;
        this.threadTimestamp = threadTimestamp;
        this.configurationOwner = configurationOwner;
        this.messageTimestamp = messageTimestamp;
        this.messageAuthorId = messageAuthorId;
        this.issueUrl = issueUrl;
        this.isPersonal = isPersonal;
        this.isEphemeral = isEphemeral;
        this.verbosity = verbosity;
    }
}
