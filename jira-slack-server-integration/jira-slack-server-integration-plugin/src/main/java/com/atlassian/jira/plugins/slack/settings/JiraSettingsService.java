package com.atlassian.jira.plugins.slack.settings;

import com.atlassian.plugins.slack.settings.SlackUserSettingsService;
import com.atlassian.sal.api.user.UserKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JiraSettingsService {
    private static final String CONFIGURATION_KEY = "bulk.edit.muted.%s";
    private final SlackUserSettingsService slackUserSettingsService;

    @Autowired
    public JiraSettingsService(final SlackUserSettingsService slackUserSettingsService) {
        this.slackUserSettingsService = slackUserSettingsService;
    }

    public void muteBulkOperationNotificationsForUser(final UserKey userKey) {
        slackUserSettingsService.putBoolean(userKey, CONFIGURATION_KEY, true);
    }

    public void unmuteBulkOperationNotificationsForUser(final UserKey userKey) {
        slackUserSettingsService.removeOption(userKey, CONFIGURATION_KEY);
    }

    public boolean areBulkNotificationsMutedForUser(final UserKey userKey) {
        return slackUserSettingsService.getBoolean(userKey, CONFIGURATION_KEY);
    }
}
