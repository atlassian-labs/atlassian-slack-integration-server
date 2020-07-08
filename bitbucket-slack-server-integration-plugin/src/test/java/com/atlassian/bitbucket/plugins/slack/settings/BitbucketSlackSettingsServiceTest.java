package com.atlassian.bitbucket.plugins.slack.settings;

import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BitbucketSlackSettingsServiceTest {
    @Mock
    SlackSettingService slackSettingService;

    @InjectMocks
    BitbucketSlackSettingsService target;

    @Test
    void setVerbosity_shouldCallGenericSettingsService() {
        int repositoryId = 4;
        String teamId = "someTeamId";
        String channelId = "someChannelId";
        Verbosity verbosity = Verbosity.EXTENDED;

        target.setVerbosity(repositoryId, teamId, channelId, verbosity);

        verify(slackSettingService).putString("verbosity." + repositoryId + "." + teamId + "." + channelId,
                verbosity.name());
    }

    @Test
    void getVerbosity_shouldCallGenericSettingsService() {
        int repositoryId = 4;
        String teamId = "someTeamId";
        String channelId = "someChannelId";
        Verbosity expectedVerbosity = Verbosity.BASIC;
        when(slackSettingService.getString("verbosity." + repositoryId + "." + teamId + "." + channelId))
                .thenReturn(expectedVerbosity.name());

        Verbosity actualVerbosity = target.getVerbosity(repositoryId, teamId, channelId);

        assertThat(actualVerbosity, Matchers.is(expectedVerbosity));
    }

    @Test
    void clearVerbosity_shouldDeleteValueFromDatabase() {
        int repositoryId = 4;
        String teamId = "someTeamId";
        String channelId = "someChannelId";

        target.clearVerbosity(repositoryId, teamId, channelId);

        verify(slackSettingService).removeOption("verbosity." + repositoryId + "." + teamId + "." + channelId);
    }
}
