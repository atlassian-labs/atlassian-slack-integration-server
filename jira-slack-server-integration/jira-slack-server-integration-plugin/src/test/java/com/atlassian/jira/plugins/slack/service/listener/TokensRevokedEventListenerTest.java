package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.crowd.event.user.UserDeletedEvent;
import com.atlassian.crowd.event.user.UserEditedEvent;
import com.atlassian.crowd.model.user.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.flag.FlagDismissalService;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.webhooks.SlackEvent;
import com.atlassian.plugins.slack.api.webhooks.TokensRevokedSlackEvent;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserKey;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Optional;

import static com.atlassian.jira.plugins.slack.service.listener.TokensRevokedEventListener.DISCONNECTED_FLAG;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class TokensRevokedEventListenerTest {
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private UserManager userManager;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private FlagDismissalService flagDismissalService;

    @Mock
    private TokensRevokedSlackEvent tokensRevokedSlackEvent;
    @Mock
    private UserDeletedEvent userDeletedEvent;
    @Mock
    private UserEditedEvent userEditedEvent;
    @Mock
    private SlackEvent slackEvent;
    @Mock
    private ApplicationUser user;
    @Mock
    private SlackUser slackUser;
    @Mock
    private User newUser;
    @Mock
    private User oldUser;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private TokensRevokedEventListener target;

    @Test
    public void tokensWereRevoked() {
        when(tokensRevokedSlackEvent.getUserIds()).thenReturn(Collections.singletonList("A"));
        when(tokensRevokedSlackEvent.getBotIds()).thenReturn(Collections.singletonList("X"));
        when(tokensRevokedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getTeamId()).thenReturn("T");
        when(slackUserManager.getBySlackUserId("A")).thenReturn(Optional.of(slackUser));
        when(slackUser.getUserKey()).thenReturn("U");
        when(userManager.getUserByKey("U")).thenReturn(user);

        target.tokensWereRevoked(tokensRevokedSlackEvent);

        verify(slackUserManager).revokeToken("A");
        verify(slackLinkManager).revokeToken("T");
        verify(flagDismissalService).removeDismissFlagForUser(DISCONNECTED_FLAG, user);
    }

    @Test
    public void userWasRemoved() {
        UserKey k = new UserKey("U");
        when(userDeletedEvent.getUsername()).thenReturn("U");
        when(slackUserManager.getByUserKey(k)).thenReturn(Collections.singletonList(slackUser));

        target.userWasRemoved(userDeletedEvent);

        verify(slackUserManager).delete(slackUser);
    }

    @Test
    public void userWasEdited() {
        UserKey k = new UserKey("U");
        when(userEditedEvent.getOriginalUser()).thenReturn(oldUser);
        when(userEditedEvent.getUser()).thenReturn(newUser);
        when(newUser.getName()).thenReturn("U");
        when(oldUser.getName()).thenReturn("U");
        when(newUser.isActive()).thenReturn(false);
        when(oldUser.isActive()).thenReturn(true);
        when(slackUserManager.getByUserKey(k)).thenReturn(Collections.singletonList(slackUser));

        target.userWasEdited(userEditedEvent);

        verify(slackUserManager).delete(slackUser);
    }
}
