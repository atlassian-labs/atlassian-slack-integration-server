package com.atlassian.bitbucket.plugins.slack.settings;

import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class BitbucketSlackSettingsService {
    private final SlackSettingService slackSettingService;

    public Verbosity getVerbosity(final int repositoryId, final String teamId, final String channelId) {
        String rawValue = slackSettingService.getString(verbosityKey(repositoryId, teamId, channelId));
        return Verbosity.valueOf(StringUtils.defaultString(rawValue, Verbosity.EXTENDED.name()));
    }

    public void setVerbosity(final int repositoryId, final String teamId, final String channelId,
                             final Verbosity verbosity) {
        slackSettingService.putString(verbosityKey(repositoryId, teamId, channelId), verbosity.name());
    }

    public void clearVerbosity(final int repositoryId, final String teamId, final String channelId) {
        slackSettingService.removeOption(verbosityKey(repositoryId, teamId, channelId));
    }

    private String verbosityKey(final int repositoryId, final String teamId, final String channelId) {
        return String.format("verbosity.%d.%s.%s", repositoryId, teamId, channelId);
    }
}
