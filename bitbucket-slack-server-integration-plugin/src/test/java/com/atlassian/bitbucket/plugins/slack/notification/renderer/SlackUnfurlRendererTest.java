package com.atlassian.bitbucket.plugins.slack.notification.renderer;

import com.atlassian.bitbucket.comment.Comment;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitListMergeFilter;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.commit.CommitsRequest;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.pull.PullRequestParticipantStatus;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.pull.PullRequestSearchRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.bitbucket.pull.PullRequestState;
import com.atlassian.bitbucket.pull.PullRequestTaskSearchRequest;
import com.atlassian.bitbucket.repository.Branch;
import com.atlassian.bitbucket.repository.Ref;
import com.atlassian.bitbucket.repository.RefService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryBranchesRequest;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.repository.Tag;
import com.atlassian.bitbucket.scm.ScmService;
import com.atlassian.bitbucket.task.TaskCount;
import com.atlassian.bitbucket.task.TaskState;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackUnfurlRendererTest {
    private final String AUTHOR_NAME = "User";
    private final String PR_TITLE = "My PR";
    private final String USER_AVATAR = "/userAvatar";
    private final String AUTHOR_LINK = "/userUrl";
    private final String PROJECT_AVATAR = "/projectAvatar";
    private final String REPO_URL = "/rep";
    private final String TARGET_REPO_URL = "/rep-target";

    @Mock
    I18nResolver i18nResolver;
    @Mock
    PullRequestService pullRequestService;
    @Mock
    SlackLinkRenderer slackLinkRenderer;
    @Mock
    ScmService scmService;
    @Mock
    RepositoryService repositoryService;
    @Mock
    RefService refService;
    @Mock
    CommitService commitService;
    @Mock
    SlackSettingService slackSettingService;

    @Mock
    Repository repository;
    @Mock
    Repository targetRepository;
    @Mock
    Project project;
    @Mock
    PullRequest pullRequest;
    @Mock
    ApplicationUser applicationUser;
    @Mock
    ApplicationUser author;
    @Mock
    ApplicationUser reviewer1;
    @Mock
    ApplicationUser reviewer2;
    @Mock
    PullRequestParticipant participantAuthor;
    @Mock
    PullRequestParticipant participantRev1;
    @Mock
    PullRequestParticipant participantRev2;
    @Mock
    PullRequestRef fromRef;
    @Mock
    PullRequestRef toRef;
    @Mock
    TaskCount taskCount;
    @Mock
    Comment comment;
    @Mock
    Commit commit;
    @Mock
    Page<Branch> branchPage;
    @Mock
    Page<Repository> repoPage;
    @Mock
    Page<Commit> commitPage;
    @Mock
    Branch branch;
    @Mock
    Tag tag;

    @InjectMocks
    SlackUnfurlRenderer renderer;

    @BeforeEach
    protected void beforeEach() {
        lenient().when(slackSettingService.isInstancePublic()).thenReturn(true);
    }

    @Test
    void pullRequest_shouldReturnPullRequestUnfurl() {
        mockPullRequest();

        when(pullRequest.getUpdatedDate()).thenReturn(new Date(11000L));
        // used inside PullRequestTaskSearchRequest.Builder
        when(toRef.getRepository()).thenReturn(targetRepository);
        when(toRef.getDisplayId()).thenReturn("toid");
        when(fromRef.getDisplayId()).thenReturn("fromid");

        when(pullRequest.getAuthor()).thenReturn(participantAuthor);
        when(pullRequest.getReviewers()).thenReturn(ImmutableSet.of(participantRev1, participantRev2));
        when(participantAuthor.getUser()).thenReturn(author);
        when(participantRev1.getUser()).thenReturn(reviewer1);
        when(reviewer1.getDisplayName()).thenReturn("B");
        when(reviewer2.getDisplayName()).thenReturn("A");
        when(participantRev1.getStatus()).thenReturn(PullRequestParticipantStatus.APPROVED);
        when(participantRev2.getUser()).thenReturn(reviewer2);
        when(participantRev2.getStatus()).thenReturn(PullRequestParticipantStatus.NEEDS_WORK);

        when(pullRequestService.countTasks(argThat(new PullRequestTaskSearchRequestMatcher(pullRequest)))).thenReturn(taskCount);
        when(taskCount.getCount(TaskState.OPEN)).thenReturn(2L);

        when(slackLinkRenderer.pullRequestLink(pullRequest)).thenReturn("/pr");
        lenient().when(slackLinkRenderer.userLink(reviewer1)).thenReturn("/rev1");
        when(slackLinkRenderer.userLink(reviewer2)).thenReturn("/rev2");

        lenient().when(i18nResolver.getText(
                "bitbucket.plugins.slack.messages.unfurl.pr.title",
                "/pr",
                PullRequestState.OPEN.name(),
                "/from",
                "/to")
        ).thenReturn("title");
        lenient().when(i18nResolver.getText(
                "bitbucket.plugins.slack.messages.unfurl.pr.fallback",
                PR_TITLE,
                PullRequestState.OPEN.name(),
                "fromid",
                "toid")
        ).thenReturn("fallback");
        lenient().when(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.pr.field.reviewers.title")).thenReturn("rt");
        lenient().when(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.pr.field.tasks.title")).thenReturn("tt");
        when(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.pr.field.tasks.value", 2L)).thenReturn("2t");

        Attachment result = renderer.pullRequest(pullRequest, null, null);

        assertThat(result, is(Attachment.builder()
                .text("title")
                .fallback("fallback")
                .authorName(AUTHOR_NAME)
                .authorIcon(USER_AVATAR)
                .authorLink(AUTHOR_LINK)
                .footer(TARGET_REPO_URL)
                .footerIcon(PROJECT_AVATAR)
                .ts("11")
                .fields(Arrays.asList(
                        Field.builder()
                                .title("rt")
                                .value(":warning: /rev2, :white_check_mark: /rev1")
                                .valueShortEnough(true)
                                .build(),
                        Field.builder()
                                .title("tt")
                                .value("2t")
                                .valueShortEnough(true)
                                .build()
                ))
                .mrkdwnIn(asList("text", "fields"))
                .build()));
    }

    @Test
    void pullRequest_shouldReturnCommentUnfurl() {
        mockPullRequest();

        when(comment.getId()).thenReturn(123L);
        when(comment.getAuthor()).thenReturn(author);
        when(comment.getText()).thenReturn("cmt");
        when(comment.getUpdatedDate()).thenReturn(new Date(11000L));
        when(slackLinkRenderer.replyPullRequestCommentUrl(pullRequest, 123L)).thenReturn("/commentUrl");

        when(i18nResolver.getText(
                "bitbucket.plugins.slack.messages.unfurl.pr.title.comment.on",
                "/commentUrl",
                PR_TITLE,
                PullRequestState.OPEN.name(),
                "/from",
                "/to")
        ).thenReturn("title");

        Attachment result = renderer.pullRequest(pullRequest, comment, commit);

        assertThat(result, is(Attachment.builder()
                .text("title\ncmt")
                .fallback("\ncmt")
                .authorName(AUTHOR_NAME)
                .authorIcon(USER_AVATAR)
                .authorLink(AUTHOR_LINK)
                .footer(TARGET_REPO_URL)
                .footerIcon(PROJECT_AVATAR)
                .ts("11")
                .mrkdwnIn(asList("text", "fields"))
                .build()));
    }

    @Test
    void pullRequestUnfurl_shouldReturnCommitUnfurl() {
        mockPullRequest();

        when(commit.getDisplayId()).thenReturn("123");
        when(commit.getAuthor()).thenReturn(author);
        when(commit.getMessage()).thenReturn("cmt");
        when(commit.getAuthorTimestamp()).thenReturn(new Date(11000L));
        when(slackLinkRenderer.pullRequestCommitUrl(pullRequest, commit)).thenReturn("/commitUrl");

        when(i18nResolver.getText(
                "bitbucket.plugins.slack.messages.unfurl.pr.title.commit.on",
                "/commitUrl",
                "123",
                PR_TITLE,
                PullRequestState.OPEN.name(),
                "/from",
                "/to")
        ).thenReturn("title");

        Attachment result = renderer.pullRequest(pullRequest, null, commit);

        assertThat(result, is(Attachment.builder()
                .text("title\ncmt")
                .fallback("\ncmt")
                .authorName(AUTHOR_NAME)
                .authorIcon(USER_AVATAR)
                .authorLink(AUTHOR_LINK)
                .footer(TARGET_REPO_URL)
                .footerIcon(PROJECT_AVATAR)
                .ts("11")
                .mrkdwnIn(asList("text", "fields"))
                .build()));
    }

    @Test
    void repository_shouldReturnUnfurl() {
        when(repository.getId()).thenReturn(12);
        when(repository.getName()).thenReturn("repo");
        when(pullRequestService.count(argThat(new PullRequestSearchRequestMatcher(12, PullRequestState.OPEN))))
                .thenReturn(7L);
        when(scmService.getScmName(repository)).thenReturn("scm");
        when(refService.getBranches(argThat(new RepositoryBranchesRequestMatcher(repository)), any(PageRequest.class)))
                .thenReturn(branchPage);
        when(branchPage.getIsLastPage()).thenReturn(false);
        when(branchPage.getSize()).thenReturn(10);
        when(repositoryService.findByOrigin(eq(repository), any(PageRequest.class))).thenReturn(repoPage);
        when(repoPage.getIsLastPage()).thenReturn(false);
        when(repoPage.getSize()).thenReturn(20);

        lenient().when(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.repo.field.open.prs.title"))
                .thenReturn("prt");
        lenient().when(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.repo.field.branches.title"))
                .thenReturn("brt");
        lenient().when(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.repo.field.forks.title"))
                .thenReturn("frt");
        lenient().when(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.repo.field.scm.title"))
                .thenReturn("scmt");

        when(repository.getProject()).thenReturn(project);
        when(slackLinkRenderer.repoLink(repository)).thenReturn(REPO_URL);
        when(slackLinkRenderer.projectAvatarUrl(project)).thenReturn(PROJECT_AVATAR);
        when(slackLinkRenderer.repoUrl(repository)).thenReturn(REPO_URL);

        Attachment result = renderer.repository(repository);

        assertThat(result, is(Attachment.builder()
                .title("repo")
                .titleLink(REPO_URL)
                .footer(REPO_URL)
                .footerIcon(PROJECT_AVATAR)
                .fields(Arrays.asList(
                        Field.builder()
                                .title("prt")
                                .value("7")
                                .valueShortEnough(true)
                                .build(),
                        Field.builder()
                                .title("brt")
                                .value("10+")
                                .valueShortEnough(true)
                                .build(),
                        Field.builder()
                                .title("frt")
                                .value("20+")
                                .valueShortEnough(true)
                                .build(),
                        Field.builder()
                                .title("scmt")
                                .value("scm")
                                .valueShortEnough(true)
                                .build()
                ))
                .mrkdwnIn(asList("text", "fields"))
                .build()));
    }

    @Test
    void branchOrTag_shouldReturnBranchUnfurl() {
        when(branch.getType()).thenReturn(StandardRefType.BRANCH);
        shouldReturnRefUnfurl(branch, "branch");
    }

    @Test
    void branchOrTag_shouldReturnTagUnfurl() {
        when(tag.getType()).thenReturn(StandardRefType.TAG);
        shouldReturnRefUnfurl(tag, "tag");
    }

    void shouldReturnRefUnfurl(Ref ref, String type) {
        mockAuthorAndRepo();

        when(repository.getName()).thenReturn("repo");
        when(ref.getDisplayId()).thenReturn("123d");
        when(ref.getId()).thenReturn("123");
        when(commitService.getCommits(
                argThat(new CommitsRequestMatcher(repository, "123", 1000, CommitListMergeFilter.EXCLUDE)),
                any(PageRequest.class)
        )).thenReturn(commitPage);
        when(commitPage.stream()).thenReturn(Stream.of(commit));

        when(commit.getAuthor()).thenReturn(author);
        when(commit.getAuthorTimestamp()).thenReturn(new Date(11000L));

        when(slackLinkRenderer.formatCommitList(Collections.singletonList(commit))).thenReturn("cmts");
        when(slackLinkRenderer.refLink(repository, ref)).thenReturn("/refLink");

        lenient().when(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl." + type + ".title", "/refLink", REPO_URL))
                .thenReturn("title");
        lenient().when(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl." + type + ".title.fallback", "123d", "repo"))
                .thenReturn("fallback");
        lenient().when(i18nResolver.getText("bitbucket.plugins.slack.messages.unfurl.ref.commits"))
                .thenReturn("commits");

        Attachment result = renderer.branchOrTag(ref, repository);

        assertThat(result, is(Attachment.builder()
                .text("title\ncommits\ncmts")
                .fallback("fallback")
                .authorName(AUTHOR_NAME)
                .authorIcon(USER_AVATAR)
                .authorLink(AUTHOR_LINK)
                .footer(REPO_URL)
                .footerIcon(PROJECT_AVATAR)
                .ts("11")
                .mrkdwnIn(asList("text", "fields"))
                .build()));
    }

    @Test
    void commit_shouldReturnUnfurl() {
        mockAuthorAndRepo();

        when(commit.getRepository()).thenReturn(repository);
        when(commit.getAuthor()).thenReturn(author);
        when(commit.getMessage()).thenReturn("cmt");
        when(commit.getAuthorTimestamp()).thenReturn(new Date(11000L));
        when(slackLinkRenderer.commitLink(commit)).thenReturn("/commitUrl");

        when(i18nResolver.getText(
                "bitbucket.plugins.slack.messages.unfurl.commit.title",
                "/commitUrl",
                REPO_URL)
        ).thenReturn("title");

        Attachment result = renderer.commit(commit, null);

        assertThat(result, is(Attachment.builder()
                .text("title\ncmt")
                .fallback("\ncmt")
                .authorName(AUTHOR_NAME)
                .authorIcon(USER_AVATAR)
                .authorLink(AUTHOR_LINK)
                .footer(REPO_URL)
                .footerIcon(PROJECT_AVATAR)
                .ts("11")
                .mrkdwnIn(asList("text", "fields"))
                .build()));
    }

    @Test
    void commit_shouldReturnCommentUnfurl() {
        mockAuthorAndRepo();

        when(commit.getRepository()).thenReturn(repository);
        when(comment.getId()).thenReturn(123L);
        when(comment.getAuthor()).thenReturn(author);
        when(comment.getText()).thenReturn("cmt");
        when(comment.getUpdatedDate()).thenReturn(new Date(11000L));
        when(slackLinkRenderer.commitCommentUrl(commit, 123L)).thenReturn("/commitCommentUrl");
        when(slackLinkRenderer.commitLink(commit)).thenReturn("/commitUrl");

        when(i18nResolver.getText(
                "bitbucket.plugins.slack.messages.unfurl.commit.title.with.comment",
                "/commitCommentUrl",
                "/commitUrl",
                REPO_URL)
        ).thenReturn("title");

        Attachment result = renderer.commit(commit, comment);

        assertThat(result, is(Attachment.builder()
                .text("title\ncmt")
                .fallback("\ncmt")
                .authorName(AUTHOR_NAME)
                .authorIcon(USER_AVATAR)
                .authorLink(AUTHOR_LINK)
                .footer(REPO_URL)
                .footerIcon(PROJECT_AVATAR)
                .ts("11")
                .mrkdwnIn(asList("text", "fields"))
                .build()));
    }

    private void mockPullRequest() {
        when(pullRequest.getTitle()).thenReturn(PR_TITLE);
        when(pullRequest.getFromRef()).thenReturn(fromRef);
        when(pullRequest.getToRef()).thenReturn(toRef);
        when(pullRequest.getState()).thenReturn(PullRequestState.OPEN);
        when(toRef.getRepository()).thenReturn(targetRepository);
        when(slackLinkRenderer.projectAvatarUrl(project)).thenReturn(PROJECT_AVATAR);
        when(slackLinkRenderer.userName(author)).thenReturn(AUTHOR_NAME);
        when(slackLinkRenderer.userUrl(author)).thenReturn(AUTHOR_LINK);
        when(slackLinkRenderer.userAvatarUrl(author)).thenReturn(USER_AVATAR);
        when(targetRepository.getProject()).thenReturn(project);
        lenient().when(slackLinkRenderer.repoLink(repository)).thenReturn(REPO_URL);
        lenient().when(slackLinkRenderer.repoLink(targetRepository)).thenReturn(TARGET_REPO_URL);
        lenient().when(slackLinkRenderer.refLink(targetRepository, fromRef)).thenReturn("/from");
        lenient().when(slackLinkRenderer.refLink(targetRepository, toRef)).thenReturn("/to");
    }

    private void mockAuthorAndRepo() {
        when(repository.getProject()).thenReturn(project);
        when(slackLinkRenderer.projectAvatarUrl(project)).thenReturn(PROJECT_AVATAR);
        when(slackLinkRenderer.userName(author)).thenReturn(AUTHOR_NAME);
        when(slackLinkRenderer.userUrl(author)).thenReturn(AUTHOR_LINK);
        when(slackLinkRenderer.userAvatarUrl(author)).thenReturn(USER_AVATAR);
        when(slackLinkRenderer.repoLink(repository)).thenReturn(REPO_URL);
    }

    static class PullRequestTaskSearchRequestMatcher implements ArgumentMatcher<PullRequestTaskSearchRequest> {
        private final PullRequest pullRequest;

        PullRequestTaskSearchRequestMatcher(final PullRequest pullRequest) {
            this.pullRequest = pullRequest;
        }

        @Override
        public boolean matches(final PullRequestTaskSearchRequest argument) {
            return argument != null && pullRequest.equals(argument.getPullRequest());
        }
    }

    static class PullRequestSearchRequestMatcher implements ArgumentMatcher<PullRequestSearchRequest> {
        private final Integer repoId;
        private final PullRequestState state;

        PullRequestSearchRequestMatcher(final Integer repoId, final PullRequestState state) {
            this.repoId = repoId;
            this.state = state;
        }

        @Override
        public boolean matches(final PullRequestSearchRequest argument) {
            return argument != null && repoId.equals(argument.getFromRepositoryId())
                    && state.equals(argument.getState());
        }
    }

    static class RepositoryBranchesRequestMatcher implements ArgumentMatcher<RepositoryBranchesRequest> {
        private final Repository repository;

        RepositoryBranchesRequestMatcher(final Repository repository) {
            this.repository = repository;
        }

        @Override
        public boolean matches(final RepositoryBranchesRequest argument) {
            return argument != null && repository.equals(argument.getRepository());
        }
    }

    static class CommitsRequestMatcher implements ArgumentMatcher<CommitsRequest> {
        private final Repository repository;
        private final String refId;
        private final Integer maxLengthMessage;
        private final CommitListMergeFilter commitListMergeFilter;

        CommitsRequestMatcher(final Repository repository,
                              final String refId,
                              final Integer maxLengthMessage,
                              final CommitListMergeFilter commitListMergeFilter) {
            this.repository = repository;
            this.refId = refId;
            this.maxLengthMessage = maxLengthMessage;
            this.commitListMergeFilter = commitListMergeFilter;
        }

        @Override
        public boolean matches(final CommitsRequest argument) {
            return argument != null
                    && repository.equals(argument.getRepository())
                    && refId.equals(argument.getCommitId())
                    && maxLengthMessage.equals(argument.getMaxMessageLength())
                    && commitListMergeFilter.equals(argument.getMerges());
        }
    }
}
