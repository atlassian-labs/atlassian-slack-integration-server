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
            List<ConversationKey> mutedChannelIds = getMutedChannelIds();
            isMuted = mutedChannelIds.contains(conversationKey);
        }
        return isMuted;
    }

    @Override
    public void muteChannel(final ConversationKey conversationKey) {
        if (StringUtils.isNotBlank(conversationKey.toString())) {
            transactionTemplate.execute(() -> {
                List<String> conversationKeys = toStringList();
                Set<String> conversationKeysWithNewOne = conversationKeys == null ? new HashSet<>() : new HashSet<>(conversationKeys);
                boolean channelWasntMutedPreviously = conversationKeysWithNewOne.add(conversationKey.toStringKey());
                if (channelWasntMutedPreviously) {
                    log.debug("Muting notifications for channel {}", conversationKey);
                    getStorage().put(MUTED_CHANNEL_IDS_OPTION_NAME, new ArrayList<>(conversationKeysWithNewOne));
                }
                return null;
            });
        }
    }

    @Override
    public void unmuteChannel(final ConversationKey conversationKey) {
        if (StringUtils.isNotBlank(conversationKey.toString())) {
            transactionTemplate.execute(() -> {
                List<String> conversationKeys = toStringList();
                List<String> conversationKeysWithoutSpecified = conversationKeys != null ? new ArrayList<>(conversationKeys) : new ArrayList<>();
                boolean channelWasMutedPreviously = conversationKeysWithoutSpecified.remove(conversationKey.toStringKey());
                if (channelWasMutedPreviously) {
                    log.debug("Unmuting notifications for channel {}", conversationKey);
                    getStorage().put(MUTED_CHANNEL_IDS_OPTION_NAME, conversationKeysWithoutSpecified);
                }
                return null;
            });
        }
    }

    @Override
    public List<ConversationKey> getMutedChannelIds() {
        try {
            List<String> conversationKeyStr = toStringList();
            if (conversationKeyStr != null) {
                List<ConversationKey> conversationKeyList = new ArrayList<>();
                for (String convKey : conversationKeyStr) {
                    conversationKeyList.add(ConversationKey.fromStringKey(convKey));
                }
                return conversationKeyList;
            }
            return Collections.emptyList();
        } catch (Throwable e) {
            log.debug("Could not get muted channels list", e);
            return Collections.emptyList();
        }
    }

    private List<String> toStringList() {
        return (List<String>) getStorage().get(MUTED_CHANNEL_IDS_OPTION_NAME);
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
