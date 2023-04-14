package com.atlassian.bitbucket.plugins.slack.notification.renderer;

import com.atlassian.bitbucket.comment.Comment;
import com.atlassian.bitbucket.comment.CommentAction;
import com.atlassian.bitbucket.comment.CommentSeverity;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitDiscussion;
import com.atlassian.bitbucket.commit.CommitRequest;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.commit.CommitsBetweenRequest;
import com.atlassian.bitbucket.event.commit.CommitDiscussionCommentEvent;
import com.atlassian.bitbucket.event.pull.PullRequestCommentEvent;
import com.atlassian.bitbucket.event.pull.PullRequestDeclinedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestEvent;
import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestParticipantApprovedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestParticipantReviewedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestParticipantUnapprovedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReopenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestRescopedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReviewersUpdatedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestUpdatedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryForkedEvent;
import com.atlassian.bitbucket.event.repository.RepositoryRefsChangedEvent;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.plugins.slack.event.RepositoryLinkedEvent;
import com.atlassian.bitbucket.plugins.slack.notification.TaskNotificationTypes;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestAction;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.pull.PullRequestParticipantStatus;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.MinimalRef;
import com.atlassian.bitbucket.repository.Ref;
import com.atlassian.bitbucket.repository.RefChange;
import com.atlassian.bitbucket.repository.RefChangeType;
import com.atlassian.bitbucket.repository.RefService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.StandardRefType;
import com.atlassian.bitbucket.server.ApplicationPropertiesService;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.EscalatedSecurityContext;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Operation;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder;
import com.github.seratch.jslack.api.model.block.ActionsBlock;
import com.github.seratch.jslack.api.model.block.LayoutBlock;
import com.github.seratch.jslack.api.model.block.SectionBlock;
import com.github.seratch.jslack.api.model.block.composition.MarkdownTextObject;
import com.github.seratch.jslack.api.model.block.composition.PlainTextObject;
import com.github.seratch.jslack.api.model.block.element.BlockElement;
import com.github.seratch.jslack.api.model.block.element.ButtonElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Answer1;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.atlassian.bitbucket.plugins.slack.util.TestUtil.getFirstSectionText;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SlackNotificationRendererTest {
    public static final String COMMIT_ID = "someCommitId";
    public static final long PR_ID = 4353L;
    public static final String USER_PROFILE_LINK = "/usr/url";
    public static final String USER_SLACK_LINK = "</usr/url|User>";
    public static final String REPO_SLACK_LINK = "</repo/1|Prj 1/Repo 1>";
    public static final String SESSIONS_PAGE_LINK = "/oauth-page";
    public static final String PR_SLACK_LINK = "</repo/1/pr/123|My PR>";
    public static final String COMMIT_LINK = "/commit";
    public static final String FORMATTED_COMMIT = "/formattedCommit";
    public static final String BRANCH_LINK = "/branch5";
    public static final String PR_FROM_BRANCH_LINK = "/from-branch";
    public static final String PR_TO_BRANCH_LINK = "/to-branch";

    @Mock
    CommitService commitService;
    @Mock
    SecurityService securityService;
    @Mock
    I18nResolver i18nResolver;
    @Mock
    SlackSettingService slackSettingService;
    @Mock
    SlackLinkRenderer slackLinkRenderer;
    @Mock
    RefService refService;
    @Mock
    ApplicationPropertiesService applicationPropertiesService;

    @Mock
    RepositoryForkedEvent repositoryForkedEvent;
    @Mock
    RepositoryLinkedEvent repositoryLinkedEvent;
    @Mock
    RepositoryRefsChangedEvent repositoryRefsChangedEvent;
    @Mock
    CommitDiscussionCommentEvent commitDiscussionCommentEvent;
    @Mock
    PullRequestOpenedEvent pullRequestOpenedEvent;
    @Mock
    PullRequestParticipantApprovedEvent pullRequestApprovedEvent;
    @Mock
    PullRequestParticipantUnapprovedEvent pullRequestUnapprovedEvent;
    @Mock
    PullRequestParticipantReviewedEvent pullRequestReviewedEvent;
    @Mock
    PullRequestDeclinedEvent pullRequestDeclinedEvent;
    @Mock
    PullRequestReopenedEvent pullRequestReopenedEvent;
    @Mock
    PullRequestUpdatedEvent pullRequestUpdatedEvent;
    @Mock
    PullRequestRescopedEvent pullRequestRescopedEvent;
    @Mock
    PullRequestCommentEvent pullRequestCommentEvent;
    @Mock
    PullRequestReviewersUpdatedEvent pullRequestReviewersUpdatedEvent;
    @Mock
    CommitDiscussion discussion;
    @Mock
    Comment comment;
    @Mock
    PullRequestParticipant prParticipant;
    @Mock
    ApplicationUser user;
    @Mock
    Repository repository;
    @Mock
    Repository origin;
    @Mock
    Project project;
    @Mock
    RefChange refChange;
    @Mock
    Page<Commit> page;
    @Mock
    Commit commit;
    @Mock
    Commit commit2;
    @Mock
    PullRequest pullRequest;
    @Mock
    PullRequestRef pullRequestFromRef;
    @Mock
    PullRequestRef pullRequestToRef;
    @Mock
    EscalatedSecurityContext securityContext;
    @Mock
    MinimalRef minimalRef;
    @Mock
    Ref ref;

    @InjectMocks
    SlackNotificationRenderer renderer;

    @SuppressWarnings("RedundantThrows")
    @BeforeEach
    void setUp() throws Throwable {
        when(slackLinkRenderer.userUrl(user)).thenReturn(USER_PROFILE_LINK);
        when(slackLinkRenderer.userLink(user)).thenReturn(USER_SLACK_LINK);
        when(slackLinkRenderer.repoLink(repository)).thenReturn(REPO_SLACK_LINK);
        when(slackLinkRenderer.pullRequestLink(pullRequest)).thenReturn(PR_SLACK_LINK);
        when(slackLinkRenderer.oAuthSessionsPageUrl()).thenReturn(SESSIONS_PAGE_LINK);
        when(slackLinkRenderer.slackMultilineQuote(anyString())).thenAnswer(answer(line -> ">>>" + line));
        when(slackLinkRenderer.commitLink(commit)).thenReturn(COMMIT_LINK);
        when(slackLinkRenderer.refLink(repository, ref)).thenReturn(BRANCH_LINK);
        when(slackLinkRenderer.refLink(repository, pullRequestFromRef)).thenReturn(PR_FROM_BRANCH_LINK);
        when(slackLinkRenderer.refLink(repository, pullRequestToRef)).thenReturn(PR_TO_BRANCH_LINK);

        when(i18nResolver.getText(anyString())).thenAnswer(answer(key -> key));
        when(i18nResolver.getText(anyString(), any()))
                .thenAnswer((Answer<String>) invocation -> join(invocation.getArguments()));
        when(commitService.getCommitsBetween(any(CommitsBetweenRequest.class), any(PageRequest.class))).thenReturn(page);
        when(refService.resolveRef(any())).thenReturn(ref);
        when(slackSettingService.isInstancePublic()).thenReturn(true);
        when(project.getName()).thenReturn("some project name");
        when(repository.getName()).thenReturn("some repo name");
        when(repository.getProject()).thenReturn(project);
        when(commit.getId()).thenReturn(COMMIT_ID);
        when(commit.getRepository()).thenReturn(repository);
        when(commit2.getId()).thenReturn(COMMIT_ID + "2");
        when(commit2.getRepository()).thenReturn(repository);
        when(pullRequest.getId()).thenReturn(PR_ID);
        when(pullRequest.getAuthor()).thenReturn(prParticipant);
        when(pullRequest.getFromRef()).thenReturn(pullRequestFromRef);
        when(pullRequest.getToRef()).thenReturn(pullRequestToRef);
        when(pullRequest.getTitle()).thenReturn("some PR title");
        when(pullRequestToRef.getRepository()).thenReturn(repository);
        when(prParticipant.getUser()).thenReturn(user);
        when(user.getDisplayName()).thenReturn("User Powerfull");
        when(refChange.getFromHash()).thenReturn("fromHash");
        when(refChange.getToHash()).thenReturn("toHash");
        when(refChange.getRef()).thenReturn(minimalRef);
        when(minimalRef.getId()).thenReturn("branch5");

        //noinspection unchecked
        when(securityContext.call(any(Operation.class))).thenAnswer(answer((Answer1<Object, Operation>) Operation::perform));
    }

    @Test
    void getRepositoryLinkedMessage_shouldReturnExpectedValue() throws IllegalAccessException {
        final RepositoryLinkedEvent event = new RepositoryLinkedEvent(new Object(), repository, "T", "C");
        FieldUtils.writeField(event, "user", user, true);

        ChatPostMessageRequestBuilder result = renderer.getRepositoryLinkedMessage(event);

        // asserts channel then removes it for standard message assertions
        assertThat(result.build().getChannel(), is("C"));
        result.channel(null);
        assertMessage(result.build(), join("slack.notification.channel-linked", USER_SLACK_LINK, REPO_SLACK_LINK));
    }

    @Test
    void getAccountMessage_shouldReturnExpectedValue() {
        String result = renderer.getAccountMessage(user);

        assertThat(result, is(join("slack.user.link.account.details.message", USER_SLACK_LINK, SESSIONS_PAGE_LINK)));
    }

    @Test
    void getAccountMessage_shouldReturnExpectedValueForNullUser() {
        when(slackLinkRenderer.oAuthSessionsPageUrl()).thenReturn(SESSIONS_PAGE_LINK);

        String result = renderer.getAccountMessage(null);

        assertThat(result, is(join("slack.user.link.no.account.message", SESSIONS_PAGE_LINK)));
    }

    @Test
    void getPleaseAuthenticateMessage_shouldReturnExpectedValue() {
        when(slackLinkRenderer.oAuthSessionsPageUrl()).thenReturn(SESSIONS_PAGE_LINK);

        String result = renderer.getPleaseAuthenticateMessage();

        assertThat(result, is(join("slack.user.link.confirm.account.message", SESSIONS_PAGE_LINK)));
    }

    @Test
    void getHelpMessage_shouldReturnExpectedValue() {
        when(applicationPropertiesService.getDisplayName()).thenReturn("BB");
        when(slackLinkRenderer.homePage()).thenReturn("/home");

        String result = renderer.getHelpMessage("bot", "/cmd");

        assertThat(result, is(join("slack.command.show.help", "<@bot>", "/home|BB", "/cmd")));
    }

    @Test
    void getInvalidCommandMessage_shouldReturnExpectedValue() {
        String result = renderer.getInvalidCommandMessage();

        assertThat(result, is("slack.command.invalid"));
    }

    @Test
    void getWelcomeMessageMessage_shouldReturnExpectedValue() {
        when(applicationPropertiesService.getDisplayName()).thenReturn("BB");
        when(slackLinkRenderer.homePage()).thenReturn("/home");
        when(slackLinkRenderer.adminConfigurationPage("T")).thenReturn("/admin");

        String result = renderer.getWelcomeMessage("T");

        assertThat(result, is(join("slack.notification.workspace.connected.welcome", "/home|BB", "/admin")));
    }

    @Test
    void getRepositoryForkedMessage_shouldReturnExpectedValue() {
        when(repositoryForkedEvent.getUser()).thenReturn(user);
        when(repositoryForkedEvent.getRepository()).thenReturn(repository);
        when(repository.getOrigin()).thenReturn(origin);
        when(slackLinkRenderer.repoLink(origin)).thenReturn("/origin");

        ChatPostMessageRequest result = renderer.getRepositoryForkedMessage(repositoryForkedEvent).build();

        assertMessage(result, join("slack.activity.repository.forked", USER_SLACK_LINK, "/origin", REPO_SLACK_LINK));
    }

    @Test
    void getRepositoryLinkedMessage_shouldBuildCorrectSlackMessage() {
        when(repositoryLinkedEvent.getUser()).thenReturn(user);
        when(repositoryLinkedEvent.getRepository()).thenReturn(repository);

        ChatPostMessageRequest result = renderer.getRepositoryLinkedMessage(repositoryLinkedEvent).build();

        assertMessage(result, join("slack.notification.channel-linked", USER_SLACK_LINK, REPO_SLACK_LINK));
    }

    @Test
    void getCommitPushMessage_shouldBuildCorrectSlackMessage() {
        when(repositoryRefsChangedEvent.getUser()).thenReturn(user);
        when(repositoryRefsChangedEvent.getRepository()).thenReturn(repository);
        when(refChange.getType()).thenReturn(RefChangeType.ADD);
        when(page.getValues()).thenReturn(singletonList(commit));
        when(page.getIsLastPage()).thenReturn(true);
        when(slackLinkRenderer.formatCommitList(singletonList(commit))).thenReturn(FORMATTED_COMMIT);

        ChatPostMessageRequest result = renderer.getPushMessage(repositoryRefsChangedEvent, refChange, Verbosity.EXTENDED)
                .build();

        String expectedText = join("slack.activity.commit.pushed.one", USER_SLACK_LINK, BRANCH_LINK, REPO_SLACK_LINK);
        assertMessage(result, expectedText, expectedText + "\n>>>" + FORMATTED_COMMIT);
    }

    @Test
    void getCommitPushMessage_shouldBuildShortSlackMessageBasedOnVerbosity() {
        when(repositoryRefsChangedEvent.getUser()).thenReturn(user);
        when(repositoryRefsChangedEvent.getRepository()).thenReturn(repository);
        when(refChange.getType()).thenReturn(RefChangeType.ADD);
        when(page.getValues()).thenReturn(singletonList(commit));
        when(page.getIsLastPage()).thenReturn(true);
        when(slackLinkRenderer.formatCommitList(singletonList(commit))).thenReturn(FORMATTED_COMMIT);

        ChatPostMessageRequest result = renderer.getPushMessage(repositoryRefsChangedEvent, refChange, Verbosity.BASIC)
                .build();

        String expectedText = join("slack.activity.commit.pushed.one", USER_SLACK_LINK, BRANCH_LINK, REPO_SLACK_LINK);
        assertMessage(result, expectedText, expectedText);
    }

    @Test
    void getCommitPushMessage_shouldBuildCorrectSlackMessageForMultipleCommits() {
        when(repositoryRefsChangedEvent.getUser()).thenReturn(user);
        when(repositoryRefsChangedEvent.getRepository()).thenReturn(repository);
        when(refChange.getType()).thenReturn(RefChangeType.ADD);
        when(page.getValues()).thenReturn(Arrays.asList(commit, commit2));
        when(page.getIsLastPage()).thenReturn(false);
        when(slackLinkRenderer.formatCommitList(Arrays.asList(commit, commit2))).thenReturn(FORMATTED_COMMIT);

        ChatPostMessageRequest result = renderer.getPushMessage(repositoryRefsChangedEvent, refChange, Verbosity.EXTENDED)
                .build();

        String expectedText = join("slack.activity.commit.pushed.multiple", USER_SLACK_LINK, "2+", BRANCH_LINK, REPO_SLACK_LINK);
        assertMessage(result, expectedText, expectedText + "\n>>>" + FORMATTED_COMMIT);
    }

    @Test
    void getCommitPushMessage_shouldBuildCorrectSlackMessageForTagCreated() {
        when(repositoryRefsChangedEvent.getUser()).thenReturn(user);
        when(repositoryRefsChangedEvent.getRepository()).thenReturn(repository);
        when(refChange.getType()).thenReturn(RefChangeType.ADD);
        when(minimalRef.getType()).thenReturn(StandardRefType.TAG);

        ChatPostMessageRequest result = renderer.getPushMessage(repositoryRefsChangedEvent, refChange, Verbosity.EXTENDED)
                .build();

        assertMessage(result, join("slack.activity.commit.tag.add", USER_SLACK_LINK, BRANCH_LINK, REPO_SLACK_LINK));
    }

    @Test
    void getCommitPushMessage_shouldBuildCorrectSlackMessageForTagDeleted() {
        when(repositoryRefsChangedEvent.getUser()).thenReturn(user);
        when(repositoryRefsChangedEvent.getRepository()).thenReturn(repository);
        when(refChange.getType()).thenReturn(RefChangeType.DELETE);
        when(minimalRef.getType()).thenReturn(StandardRefType.TAG);
        when(minimalRef.getDisplayId()).thenReturn("MyTag");

        ChatPostMessageRequest result = renderer.getPushMessage(repositoryRefsChangedEvent, refChange, Verbosity.EXTENDED)
                .build();

        assertMessage(result, join("slack.activity.commit.tag.delete", USER_SLACK_LINK, "`MyTag`", REPO_SLACK_LINK));
    }

    @Test
    void getCommitPushMessage_shouldBuildCorrectSlackMessageWithoutCommits() {
        when(repositoryRefsChangedEvent.getUser()).thenReturn(user);
        when(repositoryRefsChangedEvent.getRepository()).thenReturn(repository);
        when(refChange.getType()).thenReturn(RefChangeType.UPDATE);
        when(minimalRef.getType()).thenReturn(StandardRefType.BRANCH);
        when(page.getValues()).thenReturn(emptyList());
        when(page.getIsLastPage()).thenReturn(true);

        ChatPostMessageRequest result = renderer.getPushMessage(repositoryRefsChangedEvent, refChange, Verbosity.EXTENDED)
                .build();

        assertMessage(result, join("slack.activity.commit.branch.update", USER_SLACK_LINK, BRANCH_LINK, REPO_SLACK_LINK));
    }

    @Test
    void getCommitDiscussionMessage_shouldBuildCorrectSlackMessage() {
        when(commitDiscussionCommentEvent.getRepository()).thenReturn(repository);
        when(commitDiscussionCommentEvent.getDiscussion()).thenReturn(discussion);
        when(commitDiscussionCommentEvent.getComment()).thenReturn(comment);
        when(commitDiscussionCommentEvent.getCommentAction()).thenReturn(CommentAction.ADDED);
        when(discussion.getCommitId()).thenReturn("someCommitId");
        when(comment.getId()).thenReturn(79L);
        when(comment.getAuthor()).thenReturn(user);
        String commentBody = "awesome comment here";
        when(comment.getText()).thenReturn(commentBody);
        when(commitService.getCommit(any(CommitRequest.class))).thenReturn(commit);
        when(securityService.withPermission(eq(Permission.REPO_READ), anyString())).thenReturn(securityContext);

        ChatPostMessageRequestBuilder result = renderer.getCommitDiscussionMessage(commitDiscussionCommentEvent);

        ChatPostMessageRequest message = result.build();
        List<LayoutBlock> blocks = message.getBlocks();
        assertThat(blocks, hasSize(2));
        assertThat(getFirstSectionText(message), allOf(containsString("slack.activity.commit.comment.added"),
                containsString(USER_SLACK_LINK), containsString(COMMIT_LINK), containsString(commentBody)));
        assertThat(message.getText(), allOf(containsString("slack.activity.commit.comment.added"),
                containsString(USER_SLACK_LINK), containsString(commentBody)));
        List<BlockElement> actions = ((ActionsBlock) blocks.get(1)).getElements();
        assertThat(actions.size(), is(2));
        assertThat(((ButtonElement) actions.get(0)).getText().getText(), is("slack.activity.commit.action.reply"));
        assertThat(((ButtonElement) actions.get(1)).getText().getText(), is("slack.activity.commit.action.view.comment"));
    }

    @Test
    void getCommitDiscussionMessageDeleted_shouldBuildCorrectSlackMessage() {
        when(commitDiscussionCommentEvent.getRepository()).thenReturn(repository);
        when(commitDiscussionCommentEvent.getDiscussion()).thenReturn(discussion);
        when(commitDiscussionCommentEvent.getComment()).thenReturn(comment);
        when(commitDiscussionCommentEvent.getCommentAction()).thenReturn(CommentAction.DELETED);
        when(discussion.getCommitId()).thenReturn("someCommitId");
        when(comment.getId()).thenReturn(79L);
        when(comment.getAuthor()).thenReturn(user);
        String commentBody = "awesome comment here";
        when(comment.getText()).thenReturn(commentBody);
        when(commitService.getCommit(any(CommitRequest.class))).thenReturn(commit);
        when(securityService.withPermission(eq(Permission.REPO_READ), anyString())).thenReturn(securityContext);

        ChatPostMessageRequestBuilder result = renderer.getCommitDiscussionMessage(commitDiscussionCommentEvent);

        ChatPostMessageRequest message = result.build();
        List<LayoutBlock> blocks = message.getBlocks();
        assertThat(blocks, hasSize(1));
        assertThat(getFirstSectionText(message), allOf(containsString("slack.activity.commit.comment.deleted"),
                containsString(USER_SLACK_LINK), containsString(COMMIT_LINK), containsString(commentBody)));
        assertThat(message.getText(), allOf(containsString("slack.activity.commit.comment.deleted"),
                containsString(USER_SLACK_LINK), containsString(commentBody)));
    }

    private void testGenericPullRequestMessage(PullRequestEvent event, PullRequestAction action, String suffix) {
        when(event.getPullRequest()).thenReturn(pullRequest);
        when(event.getAction()).thenReturn(action);
        when(event.getUser()).thenReturn(user);

        ChatPostMessageRequest result = renderer.getPullRequestMessage(event).build();

        assertMessage(result, join("slack.activity.pr." + action.name().toLowerCase() + suffix + ".long",
                USER_SLACK_LINK, PR_SLACK_LINK, REPO_SLACK_LINK, PR_FROM_BRANCH_LINK, PR_TO_BRANCH_LINK));
    }

    private void testGenericPullRequestMessage(PullRequestEvent event, PullRequestAction action) {
        testGenericPullRequestMessage(event, action, "");
    }

    @Test
    void getPullRequestOpenedMessage_shouldBuildCorrectSlackMessage() {
        testGenericPullRequestMessage(pullRequestOpenedEvent, PullRequestAction.OPENED);
    }

    @Test
    void getPullRequestUpdatedMessage_shouldBuildCorrectSlackMessage() {
        testGenericPullRequestMessage(pullRequestUpdatedEvent, PullRequestAction.UPDATED);
    }

    @Test
    void getPullRequestRescopedMessage_shouldBuildCorrectSlackMessage() {
        testGenericPullRequestMessage(pullRequestRescopedEvent, PullRequestAction.RESCOPED);
    }

    @Test
    void getPullRequestApprovedMessage_shouldBuildCorrectSlackMessage() {
        testGenericPullRequestMessage(pullRequestApprovedEvent, PullRequestAction.APPROVED);
    }

    @Test
    void getPullRequestUnapprovedMessage_shouldBuildCorrectSlackMessage() {
        testGenericPullRequestMessage(pullRequestUnapprovedEvent, PullRequestAction.UNAPPROVED);
    }

    @Test
    void getPullRequestMarkedNeedsWorkMessage_shouldBuildCorrectSlackMessage() {
        testGenericPullRequestMessage(pullRequestReviewedEvent, PullRequestAction.REVIEWED);
    }

    @Test
    void getPullRequestRemovedNeedsWorkMessage_shouldBuildCorrectSlackMessage() {
        when(pullRequestUnapprovedEvent.getPreviousStatus()).thenReturn(PullRequestParticipantStatus.NEEDS_WORK);
        testGenericPullRequestMessage(pullRequestUnapprovedEvent, PullRequestAction.UNAPPROVED, ".needswork");
    }

    @Test
    void getPullRequestDeclinedMessage_shouldBuildCorrectSlackMessage() {
        testGenericPullRequestMessage(pullRequestDeclinedEvent, PullRequestAction.DECLINED);
    }

    @Test
    void getPullRequestReopenedMessage_shouldBuildCorrectSlackMessage() {
        testGenericPullRequestMessage(pullRequestReopenedEvent, PullRequestAction.REOPENED);
    }

    @Test
    void getReviewersPullRequestMessage_shouldBuildCorrectSlackMessageWhenUserJoined() {
        when(pullRequestReviewersUpdatedEvent.getPullRequest()).thenReturn(pullRequest);
        when(pullRequestReviewersUpdatedEvent.getUser()).thenReturn(user);
        when(pullRequestReviewersUpdatedEvent.getAddedReviewers()).thenReturn(Collections.singleton(user));

        ChatPostMessageRequest result = renderer.getReviewersPullRequestMessage(pullRequest, user, true, true).build();

        assertMessage(result, join("slack.activity.pr.reviewers.joined.long", USER_SLACK_LINK, PR_SLACK_LINK, REPO_SLACK_LINK,
                PR_FROM_BRANCH_LINK, PR_TO_BRANCH_LINK));
    }

    @Test
    void getReviewersPullRequestMessage_shouldBuildCorrectSlackMessageWhenUserRemovedHimself() {
        when(pullRequestReviewersUpdatedEvent.getPullRequest()).thenReturn(pullRequest);
        when(pullRequestReviewersUpdatedEvent.getUser()).thenReturn(user);
        when(pullRequestReviewersUpdatedEvent.getRemovedReviewers()).thenReturn(Collections.singleton(user));

        ChatPostMessageRequest result = renderer.getReviewersPullRequestMessage(pullRequest, user, false, false).build();

        assertMessage(result, join("slack.activity.pr.reviewers.removed.short", USER_SLACK_LINK, PR_SLACK_LINK, REPO_SLACK_LINK,
                PR_FROM_BRANCH_LINK, PR_TO_BRANCH_LINK));
    }

    @Test
    void getPullRequestCommentAddedMessage_shouldBuildCorrectSlackMessage() {
        when(pullRequestCommentEvent.getPullRequest()).thenReturn(pullRequest);
        when(pullRequestCommentEvent.getComment()).thenReturn(comment);
        when(pullRequestCommentEvent.getCommentAction()).thenReturn(CommentAction.ADDED);
        when(comment.getId()).thenReturn(79L);
        when(comment.getAuthor()).thenReturn(user);
        String commentBody = "awesome comment here";
        when(comment.getText()).thenReturn(commentBody);
        when(slackLinkRenderer.replyPullRequestCommentUrl(pullRequest, 79L)).thenReturn("/pr-comment");

        ChatPostMessageRequest result = renderer.getPullRequestMessage(pullRequestCommentEvent).build();

        String expectedText = join("slack.activity.pr.comment.added.long", USER_SLACK_LINK, PR_SLACK_LINK,
                REPO_SLACK_LINK, PR_FROM_BRANCH_LINK, PR_TO_BRANCH_LINK);
        assertMessage(result, expectedText + ": " + commentBody, expectedText + "\n>>>" + commentBody,
                Arrays.asList(
                        ButtonElement.builder()
                                .text(PlainTextObject.builder()
                                        .text("slack.activity.pr.action.reply")
                                        .build())
                                .value("{\"objectType\":\"pullRequest\",\"objectId\":\"4353\",\"repositoryId\":0,\"commentId\":79}")
                                .build(),
                        ButtonElement.builder()
                                .text(PlainTextObject.builder()
                                        .text("slack.activity.pr.action.view.comment")
                                        .build())
                                .url("/pr-comment")
                                .build()
                ));
    }

    @Test
    void getPullRequestCommentDeletedMessage_shouldBuildCorrectSlackMessage() {
        when(pullRequestCommentEvent.getPullRequest()).thenReturn(pullRequest);
        when(pullRequestCommentEvent.getComment()).thenReturn(comment);
        when(pullRequestCommentEvent.getCommentAction()).thenReturn(CommentAction.DELETED);
        when(comment.getAuthor()).thenReturn(user);

        ChatPostMessageRequest result = renderer.getPullRequestMessage(pullRequestCommentEvent).build();

        String expectedText = join("slack.activity.pr.comment.deleted.long", USER_SLACK_LINK, PR_SLACK_LINK,
                REPO_SLACK_LINK, PR_FROM_BRANCH_LINK, PR_TO_BRANCH_LINK);
        assertMessage(result, expectedText);
    }

    @Test
    void getTaskMessage_shouldBuildCorrectSlackMessage() {
        String taskText = "Do something";
        when(slackLinkRenderer.pullRequestCommentUrl(pullRequest, 7L)).thenReturn("/pr-comment");
        when(comment.getId()).thenReturn(7L);
        when(comment.getSeverity()).thenReturn(CommentSeverity.BLOCKER);
        when(comment.getText()).thenReturn(taskText);

        ChatPostMessageRequest result = renderer.getPullRequestTaskMessage(pullRequest, TaskNotificationTypes.CREATED, comment, user).build();

        String expectedText = join("slack.activity.pr.task.created.long", USER_SLACK_LINK, "/pr-comment", PR_SLACK_LINK,
                REPO_SLACK_LINK, PR_FROM_BRANCH_LINK, PR_TO_BRANCH_LINK);
        assertMessage(result, expectedText + ": " + taskText, expectedText + "\n>>>" + taskText);
    }

    static String join(Object... args) {
        return StringUtils.joinWith(",", (Object[]) args);
    }

    void assertMessage(ChatPostMessageRequest message, String expectedText) {
        assertMessage(message, expectedText, expectedText);
    }

    void assertMessage(ChatPostMessageRequest message, String expectedText, String expectedBlock) {
        assertThat(message.toString(), is(ChatPostMessageRequest.builder()
                .text(expectedText)
                .blocks(singletonList(SectionBlock.builder()
                        .text(MarkdownTextObject.builder().text(expectedBlock).build())
                        .build()))
                .build()
                .toString()));
    }

    void assertMessage(ChatPostMessageRequest message,
                       String expectedText,
                       String expectedBlock,
                       List<BlockElement> buttons) {
        assertThat(message.toString(), is(ChatPostMessageRequest.builder()
                .text(expectedText)
                .blocks(asList(
                        SectionBlock.builder().text(MarkdownTextObject.builder().text(expectedBlock).build()).build(),
                        ActionsBlock.builder().elements(buttons).build()))
                .build()
                .toString()));
    }
}
