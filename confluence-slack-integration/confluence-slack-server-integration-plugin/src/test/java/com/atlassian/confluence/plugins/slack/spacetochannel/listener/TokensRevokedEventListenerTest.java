package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.crowd.event.user.UserDeletedEvent;
import com.atlassian.crowd.event.user.UserEditedEvent;
import com.atlassian.crowd.model.user.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.webhooks.SlackEvent;
import com.atlassian.plugins.slack.api.webhooks.TokensRevokedSlackEvent;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TokensRevokedEventListenerTest {
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackUserManager slackUserManager;

    @Mock
    private TokensRevokedSlackEvent tokensRevokedSlackEvent;
    @Mock
    private UserDeletedEvent userDeletedEvent;
    @Mock
    private UserEditedEvent userEditedEvent;
    @Mock
    private SlackEvent slackEvent;
    @Mock
    private ConfluenceUser user;
    @Mock
    private SlackUser slackUser;
    @Mock
    private User newUser;
    @Mock
    private User oldUser;

    @InjectMocks
    private TokensRevokedEventListener target;

    @Test
    public void tokensWereRevoked() {
        when(tokensRevokedSlackEvent.getUserIds()).thenReturn(Collections.singletonList("A"));
        when(tokensRevokedSlackEvent.getBotIds()).thenReturn(Collections.singletonList("X"));
        when(tokensRevokedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getTeamId()).thenReturn("T");

        target.tokensWereRevoked(tokensRevokedSlackEvent);

        verify(slackUserManager).revokeToken("A");
        verify(slackLinkManager).revokeToken("T");
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
