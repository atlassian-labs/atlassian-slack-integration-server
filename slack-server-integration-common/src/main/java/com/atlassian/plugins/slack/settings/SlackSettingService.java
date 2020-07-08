package com.atlassian.plugins.slack.settings;

import java.util.List;

public interface SlackSettingService {
    boolean isChannelMuted(String channelId);

    void muteChannel(String channelId);

    void unmuteChannel(String channelId);

    List<String> getMutedChannelIds();

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
