package com.atlassian.confluence.plugins.slack.spacetochannel.ao.upgrade.v1;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.confluence.plugins.slack.spacetochannel.ao.AOEntityToChannelMapping;
import com.atlassian.plugins.slack.settings.DefaultSlackSettingsService;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
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
        log.info("old channel ids of muted channel: " + channelIds);
        if (channelIds != null) {
            List<String> conversationKeys = channelIds.stream().flatMap(channelId -> {
                AOEntityToChannelMapping config = getProjectConfigurationForChannel(channelId, activeObjects);
                return config != null ? Stream.of(config.getTeamId() + ":" + channelId) : Stream.empty();
            }).collect(Collectors.toList());
            log.info("new format for muted channel ids: " + conversationKeys);
            settings.put(DefaultSlackSettingsService.MUTED_CHANNEL_IDS_OPTION_NAME, conversationKeys);
        }
    }

    private AOEntityToChannelMapping getProjectConfigurationForChannel(String channelId, ActiveObjects ao) {
        AOEntityToChannelMapping[] configs = ao.find(AOEntityToChannelMapping.class,
                AOEntityToChannelMapping.CHANNEL_ID_COLUMN + " = ?", channelId);
        if (configs.length != 0) {
            return configs[0];
        } else {
            return null;
        }
    }
}
