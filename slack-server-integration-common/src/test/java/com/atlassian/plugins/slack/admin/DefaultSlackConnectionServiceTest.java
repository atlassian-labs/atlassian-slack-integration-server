package com.atlassian.plugins.slack.admin;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.github.seratch.jslack.api.methods.response.auth.AuthTestResponse;
import io.atlassian.fugue.Either;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultSlackConnectionServiceTest {
    public static final String TEAM_ID = "someTeamId";
    public static final String TEAM_NAME = "someTeamName";
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;
    @Mock
    private SlackClient slackClient;
    @Mock
    private SlackLink slackLink;
    @Mock
    private SlackUser slackUser;
    @Mock
    private AuthTestResponse authTestResponse;
    @InjectMocks
    private DefaultSlackConnectionService service;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp() {
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(slackClient.testToken()).thenReturn(Either.right(authTestResponse));
        when(authTestResponse.getTeamId()).thenReturn(TEAM_ID);
        when(slackLink.getTeamId()).thenReturn(TEAM_ID);
        when(slackLink.getTeamName()).thenReturn(TEAM_NAME);
    }

    @Test
    public void connectTeam_shouldCreateNewTeam_whenTeamIdIsNotPassed() {
        when(slackLinkManager.getLinkByTeamId(TEAM_ID)).thenReturn(Either.left(new Exception()));
        when(slackLinkManager.saveNew(slackLink)).thenReturn(Either.right(slackLink));

        Either<ErrorResponse, InstallationCompletionData> result = service.connectTeam(slackLink, null);

        assertThat(result.isRight(), is(true));
        InstallationCompletionData successData = result.right().get();
        assertThat(successData.getTeamId(), is(TEAM_ID));
        assertThat(successData.getTeamName(), is(TEAM_NAME));
    }

    @Test
    public void connectTeam_shouldReturnError_whenConnectionToTeamAlreadyExists() {
        when(slackLinkManager.getLinkByTeamId(TEAM_ID)).thenReturn(Either.right(slackLink));
        when(slackLinkManager.saveNew(slackLink)).thenReturn(Either.right(slackLink));

        Either<ErrorResponse, InstallationCompletionData> result = service.connectTeam(slackLink, null);

        assertThat(result.isLeft(), is(true));
        verify(slackLinkManager, never()).saveNew(any());
        verify(slackLinkManager, never()).saveExisting(any());
    }

    @Test
    public void connectTeam_shouldUpdateExistingTeam_whenTeamIdIsPassed() {
        when(slackLinkManager.getLinkByTeamId(TEAM_ID)).thenReturn(Either.left(new Exception()));
        when(slackLinkManager.saveExisting(slackLink)).thenReturn(Either.right(slackLink));

        Either<ErrorResponse, InstallationCompletionData> result = service.connectTeam(slackLink, TEAM_ID);

        assertThat(result.isRight(), is(true));
        InstallationCompletionData successData = result.right().get();
        assertThat(successData.getTeamId(), is(TEAM_ID));
        assertThat(successData.getTeamName(), is(TEAM_NAME));
    }

    @Test
    public void disconnectTeam_shouldDeleteLinkAndUserMappings() {
        when(slackUserManager.getByTeamId(TEAM_ID)).thenReturn(singletonList(slackUser));

        service.disconnectTeam(TEAM_ID);

        verify(slackLinkManager).removeLinkByTeamId(TEAM_ID);
        verify(slackUserManager).delete(slackUser);
    }
}
