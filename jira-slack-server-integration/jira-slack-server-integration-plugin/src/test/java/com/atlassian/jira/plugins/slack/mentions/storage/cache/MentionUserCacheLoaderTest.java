package com.atlassian.jira.plugins.slack.mentions.storage.cache;

import com.atlassian.jira.plugins.slack.model.UserId;
import com.atlassian.jira.plugins.slack.model.UserIdImpl;
import com.atlassian.jira.plugins.slack.model.mentions.MentionUser;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.github.seratch.jslack.api.model.User;
import io.atlassian.fugue.Either;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class MentionUserCacheLoaderTest {
    @Mock
    private SlackClientProvider slackClientProvider;

    @Mock
    private SlackLink slackLink;
    @Mock
    private User user;
    @Mock
    private User.Profile profile;
    @Mock
    private SlackClient slackClient;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private MentionUserCacheLoader target;

    @Test
    public void load() {
        UserId userId = new UserIdImpl("T", "U");
        when(user.getId()).thenReturn("U");
        when(user.getTeamId()).thenReturn("T");
        when(user.getName()).thenReturn("N");
        when(user.getRealName()).thenReturn("DN");
        when(user.isBot()).thenReturn(true);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getAvatarHash()).thenReturn("AH");
        when(profile.getImage48()).thenReturn("I");
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(slackClient));
        when(slackClient.getUserInfo("U")).thenReturn(Either.right(user));

        Optional<MentionUser> result = target.load(userId);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getKey(), is(userId));
        assertThat(result.get().getTeamId(), is("T"));
        assertThat(result.get().getId(), is("U"));
        assertThat(result.get().getName(), is("N"));
        assertThat(result.get().getDisplayName(), is("DN"));
        assertThat(result.get().isBot(), is(true));
        assertThat(result.get().getAvatarHash(), is("AH"));
        assertThat(result.get().getIcon(), is("I"));
    }

    @Test
    public void load_shouldReturnEmptyIfUserIsNotFound() {
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(slackClient));
        when(slackClient.getUserInfo("U")).thenReturn(Either.left(new ErrorResponse(new Exception())));

        Optional<MentionUser> result = target.load(new UserIdImpl("T", "U"));

        assertThat(result.isPresent(), is(false));
    }
}
