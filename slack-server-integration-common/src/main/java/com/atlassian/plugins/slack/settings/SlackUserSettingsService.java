package com.atlassian.plugins.slack.settings;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.events.PersonalNotificationConfiguredEvent;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.usersettings.UserSettings;
import com.atlassian.sal.api.usersettings.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SlackUserSettingsService {
    public static final String SLACK_SETTINGS_NAMESPACE = "com.atlassian.slack.";
    public static final String TEAM_ID_CONFIG_NAME = "personal.notification.team";

    private final UserSettingsService userSettingsService;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;

    private String prefixedKey(final String key) {
        return SLACK_SETTINGS_NAMESPACE + key;
    }

    public void putBoolean(final UserKey userKey, final String key, final boolean value) {
        userSettingsService.updateUserSettings(userKey, builder -> builder.put(prefixedKey(key), value).build());
    }

    public boolean getBoolean(final UserKey userKey, final String key) {
        final UserSettings userSettings = userSettingsService.getUserSettings(userKey);
        final String prefixedKey = prefixedKey(key);
        return UserSettingsCompatibilityHelper.getBoolean(userSettings, prefixedKey);
    }

    public void putString(final UserKey userKey, final String key, final String value) {
        userSettingsService.updateUserSettings(userKey, builder -> builder.put(prefixedKey(key), value).build());
    }

    public String getString(final UserKey userKey, final String key) {
        final UserSettings userSettings = userSettingsService.getUserSettings(userKey);
        final String prefixedKey = prefixedKey(key);
        return UserSettingsCompatibilityHelper.getString(userSettings, prefixedKey, null);
    }

    public void removeOption(final UserKey userKey, final String key) {
        userSettingsService.updateUserSettings(userKey, builder -> builder.remove(prefixedKey(key)).build());
    }

    public void setNotificationTeamId(final UserKey userKey, final String value) {
        putString(userKey, TEAM_ID_CONFIG_NAME, value);
    }

    public void removeNotificationTeamId(final UserKey userKey) {
        removeOption(userKey, TEAM_ID_CONFIG_NAME);
    }

    public String getNotificationTeamId(final UserKey userKey) {
        return getString(userKey, TEAM_ID_CONFIG_NAME);
    }

    public boolean isPersonalNotificationTypeEnabled(final UserKey userKey, final Enum key) {
        return getBoolean(userKey, key.name().toLowerCase());
    }

    public void enablePersonalNotificationType(final UserKey userKey, final Enum key) {
        String keyStr = key.name().toLowerCase();
        putBoolean(userKey, keyStr, true);
        eventPublisher.publish(new PersonalNotificationConfiguredEvent(analyticsContextProvider.current(), keyStr, true));
    }

    public void disablePersonalNotificationType(final UserKey userKey, final Enum key) {
        String keyStr = key.name().toLowerCase();
        eventPublisher.publish(new PersonalNotificationConfiguredEvent(analyticsContextProvider.current(), keyStr, false));
        removeOption(userKey, keyStr);
    }
}
