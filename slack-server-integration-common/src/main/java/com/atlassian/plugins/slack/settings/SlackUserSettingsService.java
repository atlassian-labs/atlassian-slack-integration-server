package com.atlassian.plugins.slack.settings;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.events.PersonalNotificationConfiguredEvent;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.sal.api.usersettings.UserSettings;
import com.atlassian.sal.api.usersettings.UserSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SlackUserSettingsService {
    public static final String SLACK_SETTINGS_NAMESPACE = "com.atlassian.slack.";
    public static final String TEAM_ID_CONFIG_NAME = "personal.notification.team";

    private final UserSettingsService userSettingsService;
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;
    private final UserManager userManager;

    private String prefixedKey(final String key) {
        return SLACK_SETTINGS_NAMESPACE + key;
    }

    /**
     * Save boolean option to user settings.
     *
     * @param userKey key of the user
     * @param key option key
     * @param value option value
     * @return true if value was saved; false otherwise (in case user doesn't exist)
     */
    public boolean putBoolean(final UserKey userKey, final String key, final boolean value) {
        if (userExist(userKey)) {
            userSettingsService.updateUserSettings(userKey, builder -> builder.put(prefixedKey(key), value).build());
            return true;
        }

        return false;
    }

    public boolean getBoolean(final UserKey userKey, final String key) {
        final String prefixedKey = prefixedKey(key);
        return getUserSettingsByUserKey(userKey)
                .map(settings -> UserSettingsCompatibilityHelper.getBoolean(settings, prefixedKey))
                .orElse(false);
    }

    /**
     * Save string option to user settings.
     *
     * @param userKey key of the user
     * @param key option key
     * @param value option value
     * @return true if value was saved; false otherwise (in case user doesn't exist)
     */
    public boolean putString(final UserKey userKey, final String key, final String value) {
        if (userExist(userKey)) {
            userSettingsService.updateUserSettings(userKey, builder -> builder.put(prefixedKey(key), value).build());
            return true;
        }

        return false;
    }

    public String getString(final UserKey userKey, final String key) {
        final String prefixedKey = prefixedKey(key);
        return getUserSettingsByUserKey(userKey)
                .map(settings -> UserSettingsCompatibilityHelper.getString(settings, prefixedKey, null))
                .orElse(null);
    }

    /**
     * Remove option from user settings.
     *
     * @param userKey key of the user
     * @param key option key
     * @return true if value was removed; false otherwise (in case user doesn't exist)
     */
    public boolean removeOption(final UserKey userKey, final String key) {
        if (userExist(userKey)) {
            userSettingsService.updateUserSettings(userKey, builder -> builder.remove(prefixedKey(key)).build());
            return true;
        }

        return false;
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
        if (putBoolean(userKey, keyStr, true)) {
            eventPublisher.publish(new PersonalNotificationConfiguredEvent(analyticsContextProvider.current(), keyStr, true));
        }
    }

    public void disablePersonalNotificationType(final UserKey userKey, final Enum key) {
        String keyStr = key.name().toLowerCase();
        if (removeOption(userKey, keyStr)) {
            eventPublisher.publish(new PersonalNotificationConfiguredEvent(analyticsContextProvider.current(), keyStr, false));
        }
    }

    /**
     * Retrieve user setting for user with specified key.
     *
     * @param userKey of the user to get settings for
     * @return Optional with UserSettings or empty Optional, if user with specified user key doesn't exist
     */
    private Optional<UserSettings> getUserSettingsByUserKey(final UserKey userKey) {
        if (userExist(userKey)) {
            return Optional.of(userSettingsService.getUserSettings(userKey));
        }

        return Optional.empty();
    }

    private boolean userExist(final UserKey userKey) {
        UserProfile userProfile = userManager.getUserProfile(userKey);
        Principal principal = userProfile != null ? userManager.resolve(userProfile.getUsername()) : null;

        return principal != null;
    }
}
