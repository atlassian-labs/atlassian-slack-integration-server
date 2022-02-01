package com.atlassian.bitbucket.plugins.slack.notification.configuration.ao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ModelVersion;

import com.atlassian.plugins.slack.settings.DefaultSlackSettingsService;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


public class MutedChannelIdUpgradeTask001Test {

    private static final String SLACK_SETTINGS_NAMESPACE = "com.atlassian.slack";
    private static final String MUTED_CHANNEL_IDS_OPTION_NAME = "muted.channel.ids";
    private static final String TEAM_ID = "T";

    @Mock
    PluginSettingsFactory pluginSettingsFactory;
    @Mock
    ActiveObjects ao;
    @Mock
    PluginSettings pluginSettings;
    @Mock
    AoNotificationConfiguration aoNotificationConfiguration;
    @Mock
    AoNotificationConfiguration aoNotificationConfiguration1;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT);
    @Captor
    ArgumentCaptor<String> valueCaptor;
    @Captor
    ArgumentCaptor<List<String>> valueCaptor1;


    private MutedChannelIdUpgradeTask001 target;

    private ModelVersion version = ModelVersion.valueOf("0");

    @Before
    public void setUp() throws Exception {
        target = new MutedChannelIdUpgradeTask001(pluginSettingsFactory);
    }

    @Test
    public void getModelReturnsExpectedValue() {
        ModelVersion version = target.getModelVersion();

        assertThat(version.toString(), equalTo("3"));
    }

    @Test
    public void upgrade() {
        List<String> channelIds = Arrays.asList("C", "C1");
        List<String> conversationKey = Arrays.asList("T:C", "T:C1");

        when(ao.find(eq(AoNotificationConfiguration.class), eq(AoNotificationConfiguration.CHANNEL_ID_COLUMN + " = ?"), eq("C")))
                .thenReturn(new AoNotificationConfiguration[]{aoNotificationConfiguration}, new AoNotificationConfiguration[]{});
        when(ao.find(eq(AoNotificationConfiguration.class), eq(AoNotificationConfiguration.CHANNEL_ID_COLUMN + " = ?"), eq("C1")))
                .thenReturn(new AoNotificationConfiguration[]{aoNotificationConfiguration1}, new AoNotificationConfiguration[]{});
        when(aoNotificationConfiguration.getChannelId()).thenReturn("C");
        when(aoNotificationConfiguration.getTeamId()).thenReturn(TEAM_ID);
        when(aoNotificationConfiguration1.getChannelId()).thenReturn("C1");
        when(aoNotificationConfiguration1.getTeamId()).thenReturn(TEAM_ID);
        when(pluginSettingsFactory.createSettingsForKey(SLACK_SETTINGS_NAMESPACE)).thenReturn(pluginSettings);
        when(pluginSettings.get(MUTED_CHANNEL_IDS_OPTION_NAME)).thenReturn(channelIds);

        target.upgrade(version, ao);

        verify(pluginSettings).put(valueCaptor.capture(), valueCaptor1.capture());

        assertEquals(DefaultSlackSettingsService.MUTED_CHANNEL_IDS_OPTION_NAME, valueCaptor.getValue());
        assertEquals(conversationKey, valueCaptor1.getValue());
    }
}