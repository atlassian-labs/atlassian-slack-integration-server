package com.atlassian.bitbucket.plugins.slack.profile;

import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.plugins.slack.rest.model.SlackUserDto;
import com.atlassian.plugins.slack.user.SlackUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserProfilePageContextProviderTest {
    @Mock
    ApplicationUser applicationUser;
    @Mock
    SlackUserService slackUserService;

    @InjectMocks
    UserProfilePageContextProvider target;

    @Test
    public void getContextMap_shouldAddUsersToContext() {
        String username = "someUsername";
        SlackUserDto user = new SlackUserDto("key", "id", "team", username);
        when(applicationUser.getName()).thenReturn(username);
        when(slackUserService.getSlackUsersByUsername(username)).thenReturn(singletonList(user));

        Map<String, Object> context = target.getContextMap(singletonMap("profileUser", applicationUser));

        assertThat(context.get("profileUser"), is(applicationUser));
        List<SlackUserDto> slackUsers = (List<SlackUserDto>) context.get("slackUsers");
        assertThat(slackUsers, contains(user));
    }
}
