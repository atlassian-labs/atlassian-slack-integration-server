package com.atlassian.plugins.slack.settings;

import com.atlassian.plugins.slack.api.ConversationKey;

import java.util.List;

public interface SlackSettingService {
    boolean isChannelMuted(ConversationKey conversationKey);

    void muteChannel(ConversationKey conversationKey);

    void unmuteChannel(ConversationKey conversationKey);

    List<ConversationKey> getMutedChannelIds();

    boolean isInstancePublic();

    void setInstancePublic(boolean isPublic);

    boolean isAppWelcomeMessageSent(String slackUserId);

    void markAppWelcomeMessageSent(String slackUserId);

    void putBoolean(String key, boolean value);

    boolean getBoolean(String key);

    void putString(String key, String value);

    String getString(String key);

    void removeOption(String key);

    boolean isPersonalNotificationsDisabled();
}
