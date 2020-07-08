package com.atlassian.plugins.slack.settings;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.atlassian.plugins.slack.settings.DefaultSlackSettingsService.INSTANCE_PUBLIC_OPTION_NAME;
import static com.atlassian.plugins.slack.settings.DefaultSlackSettingsService.MUTED_CHANNEL_IDS_OPTION_NAME;
import static com.atlassian.plugins.slack.settings.DefaultSlackSettingsService.SLACK_SETTINGS_NAMESPACE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultSlackSettingsServiceTest {
    @Mock
    private PluginSettingsFactory pluginSettingsFactory;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private PluginSettings pluginSettings;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultSlackSettingsService target;

    @Before
    public void setUp() throws Exception {
        when(pluginSettingsFactory.createSettingsForKey(SLACK_SETTINGS_NAMESPACE)).thenReturn(pluginSettings);
        when(transactionTemplate.execute(any())).thenAnswer(answer((TransactionCallback callback) -> callback.doInTransaction()));
    }

    @Test
    public void getMutedChannelIds_shouldReturnEmptyWhenStorageIsNull() {
        when(pluginSettings.get(MUTED_CHANNEL_IDS_OPTION_NAME)).thenReturn(null);

        List<String> result = target.getMutedChannelIds();

        assertThat(result, empty());
    }

    @Test
    public void getMutedChannelIds_shouldReturnExpectedValue() {
        List<String> list = new ArrayList<>();
        when(pluginSettings.get(MUTED_CHANNEL_IDS_OPTION_NAME)).thenReturn(list);

        List<String> result = target.getMutedChannelIds();

        assertThat(result, sameInstance(list));

    }

    @Test
    public void isChannelMuted_shouldReturnExpectedValue() {
        List<String> list = Collections.singletonList("C");
        when(pluginSettings.get(MUTED_CHANNEL_IDS_OPTION_NAME)).thenReturn(list);

        boolean result = target.isChannelMuted("C");

        assertThat(result, is(true));
    }


    @Test
    public void isChannelMuted_shouldReturnFalseWhenChannelIsNotMted() {
        List<String> list = Collections.singletonList("C");
        when(pluginSettings.get(MUTED_CHANNEL_IDS_OPTION_NAME)).thenReturn(list);

        boolean result = target.isChannelMuted("C2");

        assertThat(result, is(false));
    }

    @Test
    public void isChannelMuted_shouldReturnFalseWhenChannelIdIsEmpty() {
        boolean result = target.isChannelMuted("");

        assertThat(result, is(false));
    }

    @Test
    public void muteChannel_shouldAddChannel() {
        List<String> list = Collections.singletonList("C");
        when(pluginSettings.get(MUTED_CHANNEL_IDS_OPTION_NAME)).thenReturn(list);

        target.muteChannel("C2");

        verify(pluginSettings).put(eq(MUTED_CHANNEL_IDS_OPTION_NAME), argThat(o -> Matchers.contains("C", "C2").matches(o)));
    }

    @Test
    public void muteChannel_shouldAddChannelWhenListIsNull() {
        when(pluginSettings.get(MUTED_CHANNEL_IDS_OPTION_NAME)).thenReturn(null);

        target.muteChannel("C");

        verify(pluginSettings).put(eq(MUTED_CHANNEL_IDS_OPTION_NAME), argThat(o -> Matchers.contains("C").matches(o)));
    }

    @Test
    public void muteChannel_shouldNotAddChannelWhenAlreadyInTheList() {
        List<String> list = Collections.singletonList("C");
        when(pluginSettings.get(MUTED_CHANNEL_IDS_OPTION_NAME)).thenReturn(list);

        target.muteChannel("C");

        verify(pluginSettings, never()).put(any(), any());
    }


    @Test
    public void unmuteChannel_shouldRemoveChannel() {
        List<String> list = Arrays.asList("C", "C2");
        when(pluginSettings.get(MUTED_CHANNEL_IDS_OPTION_NAME)).thenReturn(list);

        target.unmuteChannel("C2");

        verify(pluginSettings).put(eq(MUTED_CHANNEL_IDS_OPTION_NAME), argThat(o -> Matchers.contains("C").matches(o)));
    }

    @Test
    public void unmuteChannel_shouldDoNothingWhenListIsNull() {
        when(pluginSettings.get(MUTED_CHANNEL_IDS_OPTION_NAME)).thenReturn(null);

        target.unmuteChannel("C");

        verify(pluginSettings, never()).put(any(), any());
    }

    @Test
    public void unmuteChannel_shouldDoNothingWhenNotInTheList() {
        List<String> list = Collections.singletonList("C");
        when(pluginSettingsFactory.createSettingsForKey(SLACK_SETTINGS_NAMESPACE)).thenReturn(pluginSettings);
        when(pluginSettings.get(MUTED_CHANNEL_IDS_OPTION_NAME)).thenReturn(list);

        target.unmuteChannel("C2");
        verify(pluginSettings, never()).put(any(), any());
    }

    @Test
    public void isInstancePublic_shouldReturnFalseForEmptyStorage() {
        assertThat(target.isInstancePublic(), is(false));
    }

    @Test
    public void isInstancePublic_shouldReturnTrue() {
        when(pluginSettings.get(INSTANCE_PUBLIC_OPTION_NAME)).thenReturn("true");

        assertThat(target.isInstancePublic(), is(true));
    }

    @Test
    public void setInstancePublic_shouldSetFlag() {
        target.setInstancePublic(true);

        verify(pluginSettings).put(eq(INSTANCE_PUBLIC_OPTION_NAME), eq("true"));
    }

    @Test
    public void putBoolean_shouldInteractWithStorage() {
        String key = "someKey";

        target.putBoolean(key, true);

        verify(pluginSettings).put(key, "true");
    }

    @Test
    public void getBoolean_shouldInteractWithStorage() {
        String key = "someKey";
        when(pluginSettings.get(key)).thenReturn("true");

        boolean actualValue = target.getBoolean(key);

        assertThat(actualValue, is(true));
    }

    @Test
    public void putString_shouldInteractWithStorage() {
        String key = "someKey";
        String value = "someValue";

        target.putString(key, value);

        verify(pluginSettings).put(key, value);
    }

    @Test
    public void getString_shouldInteractWithStorage() {
        String key = "someKey";
        String expectedValue = "someValue";
        when(pluginSettings.get(key)).thenReturn(expectedValue);

        String actualValue = target.getString(key);

        assertThat(actualValue, is(expectedValue));
    }

    @Test
    public void removeOption_shouldInteractWithStorage() {
        String key = "someKey";
        target.removeOption(key);

        verify(pluginSettings).remove(key);
    }
}
