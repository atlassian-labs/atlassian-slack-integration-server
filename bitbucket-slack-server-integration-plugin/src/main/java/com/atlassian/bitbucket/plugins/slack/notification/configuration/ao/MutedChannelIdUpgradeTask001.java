package com.atlassian.bitbucket.plugins.slack.notification.configuration.ao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.ao.AoNotificationConfiguration;
import com.atlassian.plugins.slack.settings.DefaultSlackSettingsService;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MutedChannelIdUpgradeTask001 implements ActiveObjectsUpgradeTask {

    private final PluginSettingsFactory pluginSettingsFactory;

    @Override
    public ModelVersion getModelVersion() {
        return ModelVersion.valueOf("1");
    }

    @Override
    public void upgrade(ModelVersion modelVersion, ActiveObjects activeObjects) {
        PluginSettings settings = pluginSettingsFactory.createSettingsForKey(DefaultSlackSettingsService.SLACK_SETTINGS_NAMESPACE);
        List<String> channelIds = (List<String>) settings.get(DefaultSlackSettingsService.MUTED_CHANNEL_IDS_OPTION_NAME);
        if (channelIds != null) {
            List<String> conversationKeys = channelIds.stream().flatMap(channelId -> {
                AoNotificationConfiguration config = getProjectConfigurationForChannel(channelId, activeObjects);
                return config != null ? Stream.of(config.getTeamId() + ":" + channelId) : Stream.empty();
            }).collect(Collectors.toList());
            settings.put(DefaultSlackSettingsService.MUTED_CHANNEL_IDS_OPTION_NAME, conversationKeys);
        }
    }

    private AoNotificationConfiguration getProjectConfigurationForChannel(String channelId, ActiveObjects ao) {
        AoNotificationConfiguration[] configs = ao.find(AoNotificationConfiguration.class,
                AoNotificationConfiguration.CHANNEL_ID_COLUMN + " = ?", channelId);
        if (configs.length != 0) {
            return configs[0];
        } else {
            return null;
        }
    }
}
