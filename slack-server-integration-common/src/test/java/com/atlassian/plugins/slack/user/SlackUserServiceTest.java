package com.atlassian.plugins.slack.user;

import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.SlackUserDto;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import io.atlassian.fugue.Either;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class SlackUserServiceTest {
    @Mock
    UserManager userManager;
    @Mock
    SlackUserManager slackUserManager;
    @Mock
    SlackLinkManager slackLinkManager;

    @Mock
    UserProfile userProfile;
    @Mock
    SlackUser slackUser1;
    @Mock
    SlackUser slackUser2;
    @Mock
    SlackLink slackLink1;
    @Mock
    SlackLink slackLink2;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    SlackUserService target;

    @Test
    public void getSlackUsersByUsername_shouldRetrieveExpectedUsers() {
        String username = "someUsername";
        String userKeyStr = "5";
        UserKey userKey = new UserKey(userKeyStr);
        String teamId1 = "someTeamId1";
        String teamId2 = "someTeamId2";
        String teamName1 = "someTeamName1";
        String teamName2 = "someTeamName2";
        String slackUserId1 = "userIdInTeam1";
        String slackUserId2 = "userIdInTeam2";

        when(userManager.getUserProfile(username)).thenReturn(userProfile);
        when(userProfile.getUserKey()).thenReturn(userKey);
        when(slackUserManager.getByUserKey(userKey)).thenReturn(asList(slackUser1, slackUser2));
        when(slackUser1.getUserKey()).thenReturn(userKeyStr);
        when(slackUser1.getSlackTeamId()).thenReturn(teamId1);
        when(slackUser1.getSlackUserId()).thenReturn(slackUserId1);
        when(slackUser2.getUserKey()).thenReturn(userKeyStr);
        when(slackUser2.getSlackTeamId()).thenReturn(teamId2);
        when(slackUser2.getSlackUserId()).thenReturn(slackUserId2);
        when(slackLinkManager.getLinkByTeamId(teamId1)).thenReturn(Either.right(slackLink1));
        when(slackLinkManager.getLinkByTeamId(teamId2)).thenReturn(Either.right(slackLink2));
        when(slackLink1.getTeamName()).thenReturn(teamName1);
        when(slackLink2.getTeamName()).thenReturn(teamName2);

        List<SlackUserDto> users = target.getSlackUsersByUsername(username);

        assertThat(users, containsInAnyOrder(new SlackUserDto(userKeyStr, slackUserId1, teamId1, teamName1),
                new SlackUserDto(userKeyStr, slackUserId2, teamId2, teamName2)));
    }
}
