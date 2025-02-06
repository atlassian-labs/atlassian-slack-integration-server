package com.atlassian.plugins.slack.settings;

import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.sal.api.usersettings.UserSettingsService;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SlackUserSettingsServiceTest {
    @Mock
    UserSettingsService userSettingsService;
    @Mock
    UserManager userManager;

    @Mock
    UserKey userKey;
    @Mock
    UserProfile userProfile;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @InjectMocks
    SlackUserSettingsService target;

    @Test
    public void putBoolean_doesntCallUserSettingsService_whenUserDoesntExist() {
        when(userManager.getUserProfile(userKey)).thenReturn(userProfile);

        target.putBoolean(userKey, "someOptionKey", true);

        verify(userSettingsService, never()).updateUserSettings(any(UserKey.class), any(Function.class));
    }

    @Test
    public void getBoolean_doesntCreateUserSettingService_whenUserDoesntExist() {
        when(userManager.getUserProfile(userKey)).thenReturn(userProfile);

        target.getBoolean(userKey, "someOptionKey");

        verify(userSettingsService, never()).getUserSettings(any(UserKey.class));
    }

    @Test
    public void putString_doesntCallUserSettingsService_whenUserDoesntExist() {
        when(userManager.getUserProfile(userKey)).thenReturn(userProfile);

        target.putString(userKey, "someOptionKey", "someStringValue");

        verify(userSettingsService, never()).updateUserSettings(any(UserKey.class), any(Function.class));
    }

    @Test
    public void getString_doesntCreateUserSettingService_whenUserDoesntExist() {
        when(userManager.getUserProfile(userKey)).thenReturn(userProfile);

        target.getString(userKey, "someOptionKey");

        verify(userSettingsService, never()).getUserSettings(any(UserKey.class));
    }

    @Test
    public void removeOption_doesntCallUserSettingsService_whenUserDoesntExist() {
        when(userManager.getUserProfile(userKey)).thenReturn(userProfile);

        target.removeOption(userKey, "someOptionKey");

        verify(userSettingsService, never()).updateUserSettings(any(UserKey.class), any(Function.class));
    }
}
