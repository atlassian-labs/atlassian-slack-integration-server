package com.atlassian.bitbucket.plugins.slack.notification.renderer;

import com.atlassian.bitbucket.avatar.AvatarRequest;
import com.atlassian.bitbucket.avatar.AvatarService;
import com.atlassian.bitbucket.comment.Comment;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.nav.DummyNavBuilder;
import com.atlassian.bitbucket.nav.NavBuilder;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Ref;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.Person;
import com.atlassian.sal.api.message.I18nResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackLinkRenderer.CONFIGURE_PATH;
import static com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackLinkRenderer.OAUTH_SESSIONS_PATH;
import static com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackLinkRenderer.SLACK_PATH;
import static com.atlassian.plugins.slack.util.LinkHelper.decorateWithOrigin;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlackLinkRendererTest {
    private static final String PR_TITLE = "My PR";
    private static final String REPO_NAME = "Repo 1";
    private static final String PROJECT_NAME = "Prj 1";

    @Spy
    NavBuilder navBuilder = DummyNavBuilder.create();
    @Mock
    I18nResolver i18nResolver;
    @Mock
    AvatarService avatarService;

    @Mock
    Comment comment;
    @Mock
    Repository repository;
    @Mock
    Project project;
    @Mock
    Commit commit;
    @Mock
    Commit commit2;
    @Mock
    PullRequest pullRequest;
    @Mock
    ApplicationUser applicationUser;
    @Mock
    PullRequestRef fromRef;
    @Mock
    Person person;
    @Mock
    Ref ref;

    @InjectMocks
    SlackLinkRenderer renderer;

    @Test
    public void userLink_shouldReturnExpectedValue() {
        when(applicationUser.getDisplayName()).thenReturn("Usr");

        String result = renderer.userLink(applicationUser);

        assertThat(result, is("<" + decorateWithOrigin(navBuilder.user(applicationUser).buildAbsolute(), "user") + "|Usr>"));
    }

    @Test
    public void userLink_shouldReturnAnonymousValue() {
        when(i18nResolver.getText("plugins.slack.common.anonymous")).thenReturn("anonymous");

        String result = renderer.userLink(null);

        assertThat(result, is("anonymous"));
    }

    @Test
    public void userUrl_shouldReturnExpectedValue() {
        String result = renderer.userUrl(applicationUser);

        assertThat(result, is(decorateWithOrigin(navBuilder.user(applicationUser).buildAbsolute(), "user")));
    }

    @Test
    public void userName_shouldReturnExpectedValueForPerson() {
        when(person.getName()).thenReturn("p");

        String result = renderer.userName(person);

        assertThat(result, is("p"));
    }

    @Test
    public void userName_shouldReturnExpectedValueForUser() {
        when(applicationUser.getDisplayName()).thenReturn("usr");

        String result = renderer.userName(applicationUser);

        assertThat(result, is("usr"));
    }

    @Test
    public void userAvatarUrl_shouldReturnExpectedValue() {
        when(avatarService.getUrlForPerson(eq(applicationUser), any(AvatarRequest.class))).thenReturn("url");

        String result = renderer.userAvatarUrl(applicationUser);

        assertThat(result, is("url"));
    }

    @Test
    public void repoLink_shouldReturnExpectedValue() {
        when(repository.getName()).thenReturn(REPO_NAME);
        when(repository.getProject()).thenReturn(project);
        when(project.getName()).thenReturn(PROJECT_NAME);

        String result = renderer.repoLink(repository);

        assertThat(result, is("<" + decorateWithOrigin(navBuilder.repo(repository).buildAbsolute(), "repository") + "|"
                + PROJECT_NAME + "/" + REPO_NAME + ">"));
    }

    @Test
    public void projectAvatarUrl_shouldReturnExpectedValue() {
        String result = renderer.projectAvatarUrl(project);

        assertThat(result, is(navBuilder.project(project).avatar(24).buildAbsolute()));
    }

    @Test
    public void commitLink_shouldReturnExpectedValue() {
        when(commit.getRepository()).thenReturn(repository);
        when(commit.getId()).thenReturn("cmt");
        when(commit.getDisplayId()).thenReturn("Cmt");

        String result = renderer.commitLink(commit);

        assertThat(result, is("`<" + decorateWithOrigin(navBuilder.repo(repository).commit("cmt").buildAbsolute(), "commit") + "|Cmt>`"));
    }

    @Test
    public void commitCommentUrl_shouldReturnExpectedValue() {
        when(commit.getRepository()).thenReturn(repository);
        when(commit.getId()).thenReturn("cmt");

        String result = renderer.commitCommentUrl(commit, 12L);

        assertThat(result, is(decorateWithOrigin(navBuilder
                .repo(repository)
                .commit("cmt")
                .comment(12L)
                .buildAbsolute(), "comment")));
    }

    @Test
    public void refUrl_shouldReturnExpectedValue() {
        when(ref.getType()).thenReturn(StandardRefType.BRANCH);

        String result = renderer.refUrl(repository, ref);

        assertThat(result, is(decorateWithOrigin(navBuilder
                .repo(repository)
                .browse()
                .atRevision(ref)
                .buildAbsolute(), "branch")));
    }

    @Test
    public void refLink_shouldReturnExpectedValue() {
        when(ref.getDisplayId()).thenReturn("hash");
        when(ref.getType()).thenReturn(StandardRefType.TAG);

        String result = renderer.refLink(repository, ref);

        assertThat(result, is("`<" + decorateWithOrigin(navBuilder.repo(repository).browse().atRevision(ref).buildAbsolute(), "tag") + "|hash>`"));
    }

    @Test
    public void pullRequestLink_shouldReturnExpectedValue() {
        when(pullRequest.getId()).thenReturn(123L);
        when(pullRequest.getTitle()).thenReturn(PR_TITLE);
        when(pullRequest.getToRef()).thenReturn(fromRef);
        when(fromRef.getRepository()).thenReturn(repository);

        String result = renderer.pullRequestLink(pullRequest);

        assertThat(result, is("<" + decorateWithOrigin(navBuilder.repo(repository).pullRequest(123L).buildAbsolute(), "pullRequest") + "|"
                + PR_TITLE + ">"));
    }

    @Test
    public void replyPullRequestCommentUrl_shouldReturnExpectedValue() {
        when(pullRequest.getId()).thenReturn(123L);
        when(pullRequest.getToRef()).thenReturn(fromRef);
        when(fromRef.getRepository()).thenReturn(repository);

        String result = renderer.replyPullRequestCommentUrl(pullRequest, 12L);

        assertThat(result, is(decorateWithOrigin(navBuilder
                .repo(repository)
                .pullRequest(123L)
                .comment(12L)
                .reply()
                .buildAbsolute(), "pullRequestComment")));
    }

    @Test
    public void pullRequestCommentUrl_shouldReturnExpectedValue() {
        when(pullRequest.getId()).thenReturn(123L);
        when(pullRequest.getToRef()).thenReturn(fromRef);
        when(fromRef.getRepository()).thenReturn(repository);

        String result = renderer.pullRequestCommentUrl(pullRequest, 12L);

        assertThat(result, is(decorateWithOrigin(navBuilder
                .repo(repository)
                .pullRequest(123L)
                .comment(12L)
                .buildAbsolute(), "pullRequestComment")));
    }

    @Test
    public void pullRequestCommitUrl_shouldReturnExpectedValue() {
        when(pullRequest.getId()).thenReturn(123L);
        when(pullRequest.getToRef()).thenReturn(fromRef);
        when(fromRef.getRepository()).thenReturn(repository);
        when(commit.getId()).thenReturn("hash");

        String result = renderer.pullRequestCommitUrl(pullRequest, commit);

        assertThat(result, is(decorateWithOrigin(navBuilder
                .repo(repository)
                .pullRequest(123L)
                .commit("hash")
                .buildAbsolute(), "pullRequestCommit")));
    }

    @Test
    public void oAuthSessionsPageUrl_shouldReturnExpectedValue() {
        String result = renderer.oAuthSessionsPageUrl();

        assertThat(result, is(navBuilder
                .pluginServlets()
                .path(SLACK_PATH, OAUTH_SESSIONS_PATH)
                .buildAbsolute()));
    }

    @Test
    public void adminConfigurationPage_shouldReturnExpectedValue() {
        // DummyNavBuilder fails to mock `withParam(name, value)` so that mocking manually
        NavBuilder.PluginServlets pluginServlets = mock(NavBuilder.PluginServlets.class);
        doReturn(pluginServlets).when(navBuilder).pluginServlets();
        when(pluginServlets.path(SLACK_PATH, CONFIGURE_PATH)).thenReturn(pluginServlets);
        when(pluginServlets.withParam("teamId", "T")).thenReturn(pluginServlets);

        renderer.adminConfigurationPage("T");

        verify(navBuilder).pluginServlets();
        verify(pluginServlets).path(SLACK_PATH, CONFIGURE_PATH);
        verify(pluginServlets).withParam("teamId", "T");
        verify(pluginServlets).buildAbsolute();
    }

    @Test
    public void formatCommitList_shouldReturnExpectedValue() {
        when(commit.getRepository()).thenReturn(repository);
        when(commit.getId()).thenReturn("cmt");
        when(commit.getDisplayId()).thenReturn("Cmt");
        when(commit.getMessage()).thenReturn("CmtMsg");
        when(commit2.getRepository()).thenReturn(repository);
        when(commit2.getId()).thenReturn("cmt2");
        when(commit2.getDisplayId()).thenReturn("Cmt2");
        when(commit2.getMessage()).thenReturn("CmtMsg2");

        String result = renderer.formatCommitList(Arrays.asList(commit, commit2));

        assertThat(result, is(
                "`<" + decorateWithOrigin(navBuilder.repo(repository).commit("cmt").buildAbsolute(), "commit") + "|Cmt>` CmtMsg\n" +
                        "`<" + decorateWithOrigin(navBuilder.repo(repository).commit("cmt2").buildAbsolute(), "commit") + "|Cmt2>` CmtMsg2"));
    }

    @Test
    public void homePage_shouldReturnExpectedValue() {
        String result = renderer.homePage();

        assertThat(result, is(decorateWithOrigin(navBuilder.dashboard().buildAbsolute(), "site")));
    }

    @Test
    public void slackMultilineQuote_shouldReturnExpectedValue() {
        String result = renderer.slackMultilineQuote("aaa");

        assertThat(result, is(">>>aaa"));
    }
}
