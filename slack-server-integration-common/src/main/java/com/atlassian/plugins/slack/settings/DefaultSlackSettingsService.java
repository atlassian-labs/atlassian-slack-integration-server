package com.atlassian.plugins.slack.settings;

import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class DefaultSlackSettingsService implements SlackSettingService {
    public static final String SLACK_SETTINGS_NAMESPACE = "com.atlassian.slack";
    public static final String MUTED_CHANNEL_IDS_OPTION_NAME = "muted.channel.ids";
    public static final String INSTANCE_PUBLIC_OPTION_NAME = "instance.public";
    public static final String WELCOME_SENT_OPTION_NAME = "welcome.sent.";
    public static final String DISABLE_PERSONAL_NOTIFICATIONS_SYSTEM_PROPERTY = "slack.personal.notifications.disabled";

    private final PluginSettingsFactory pluginSettingsFactory;
    private final TransactionTemplate transactionTemplate;

    @Override
    public boolean isChannelMuted(final ConversationKey conversationKey) {
        boolean isMuted = false;
        if (StringUtils.isNotBlank(conversationKey.toString())) {
            List<String> mutedChannelIds = getMutedChannelIds();
            isMuted = mutedChannelIds.contains(conversationKey);
        }
        return isMuted;
    }

    @Override
    public void muteChannel(final String channelId) {
        if (StringUtils.isNotBlank(channelId)) {
            transactionTemplate.execute(() -> {
                List<String> channelIds = getMutedChannelIds();
                Set<String> channelIdsWithNewOne = new HashSet<>(channelIds);
                boolean channelWasntMutedPreviously = channelIdsWithNewOne.add(channelId);
                if (channelWasntMutedPreviously) {
                    log.debug("Muting notifications for channel {}", channelId);
                    getStorage().put(MUTED_CHANNEL_IDS_OPTION_NAME, new ArrayList<>(channelIdsWithNewOne));
                }

                return null;
            });
        }
    }

    @Override
    public void unmuteChannel(final String channelId) {
        if (StringUtils.isNotBlank(channelId)) {
            transactionTemplate.execute(() -> {
                List<String> channelIds = getMutedChannelIds();
                List<String> channelIdsWithoutSpecified = channelIds != null ? new ArrayList<>(channelIds) : new ArrayList<>();
                boolean channelWasMutedPreviously = channelIdsWithoutSpecified.remove(channelId);
                if (channelWasMutedPreviously) {
                    log.debug("Unmuting notifications for channel {}", channelId);
                    getStorage().put(MUTED_CHANNEL_IDS_OPTION_NAME, channelIdsWithoutSpecified);
                }

                return null;
            });
        }
    }

    @Override
    public List<String> getMutedChannelIds() {
        try {
            List<String> channelIds = (List<String>) getStorage().get(MUTED_CHANNEL_IDS_OPTION_NAME);
            return channelIds != null ? channelIds : Collections.emptyList();
        } catch (Throwable e) {
            log.debug("Could not get muted channels list", e);
            return Collections.emptyList();
        }
    }

    private PluginSettings getStorage() {
        return pluginSettingsFactory.createSettingsForKey(SLACK_SETTINGS_NAMESPACE);
    }

    @Override
    public boolean isInstancePublic() {
        return getBoolean(INSTANCE_PUBLIC_OPTION_NAME);
    }

    @Override
    public void setInstancePublic(final boolean isPublic) {
        putBoolean(INSTANCE_PUBLIC_OPTION_NAME, isPublic);
    }

    @Override
    public boolean isAppWelcomeMessageSent(final String slackUserId) {
        return getBoolean(WELCOME_SENT_OPTION_NAME + slackUserId);
    }

    @Override
    public void markAppWelcomeMessageSent(final String slackUserId) {
        putBoolean(WELCOME_SENT_OPTION_NAME + slackUserId, true);
    }

    @Override
    public void putBoolean(final String key, final boolean value) {
        putString(key, Boolean.toString(value));
    }

    @Override
    public boolean getBoolean(final String key) {
        return Boolean.parseBoolean(getString(key));
    }

    @Override
    public void putString(final String key, final String value) {
        getStorage().put(key, value);
    }

    @Override
    public String getString(final String key) {
        return (String) getStorage().get(key);
    }

    @Override
    public void removeOption(final String key) {
        getStorage().remove(key);
    }

    @Override
    public boolean isPersonalNotificationsDisabled() {
        return Boolean.getBoolean(DISABLE_PERSONAL_NOTIFICATIONS_SYSTEM_PROPERTY);
    }
}
