package com.atlassian.jira.plugins.slack.model.mentions;

import com.atlassian.jira.plugins.slack.storage.StorableEntity;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * A single issue mention
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = ANY)
public class IssueMention implements StorableEntity<String> {
    private final long issueId;
    private final String teamId;
    private final String channelId;
    private final String messageId;
    private final String messageText;
    private final String userId;
    private final Date dateTime;

    @JsonCreator
    public IssueMention(
            @JsonProperty("issueId") long issueId,
            @JsonProperty("teamId") String teamId,
            @JsonProperty("channelId") @Nonnull String channelId,
            @JsonProperty("messageId") @Nonnull String messageId,
            @JsonProperty("messageText") @Nonnull String messageText,
            @JsonProperty("userId") @Nonnull String userId,
            @JsonProperty("dateTime") @Nonnull Date dateTime) {
        checkArgument(!isNullOrEmpty(teamId), "teamId is null");
        checkArgument(!isNullOrEmpty(channelId), "channelId is null");
        checkArgument(!isNullOrEmpty(messageId), "message id is null");

        this.issueId = issueId;
        this.teamId = teamId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.messageText = messageText;
        this.userId = userId;
        this.dateTime = checkNotNull(dateTime);
    }

    public long getIssueId() {
        return issueId;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getMessageText() {
        return messageText;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public String getKey() {
        return channelId + messageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IssueMention)) {
            return false;
        }

        IssueMention that = (IssueMention) o;

        if (!teamId.equals(that.teamId)) {
            return false;
        }
        if (!messageId.equals(that.messageId)) {
            return false;
        }
        if (!channelId.equals(that.channelId)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = channelId.hashCode();
        result = 31 * result + messageId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "IssueMention{" +
                "channelId='" + channelId + '\'' +
                ", teamId='" + teamId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", userId='" + userId + '\'' +
                ", dateTime=" + dateTime +
                '}';
    }
}
