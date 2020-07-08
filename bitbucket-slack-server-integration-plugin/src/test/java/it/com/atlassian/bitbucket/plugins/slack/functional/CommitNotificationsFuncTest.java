package it.com.atlassian.bitbucket.plugins.slack.functional;

import com.atlassian.bitbucket.content.DiffFileType;
import com.atlassian.bitbucket.content.DiffSegmentType;
import com.atlassian.bitbucket.plugins.slack.notification.RepositoryNotificationTypes;
import com.atlassian.bitbucket.test.DefaultFuncTestData;
import com.atlassian.bitbucket.test.rest.comment.RestCommentUtils;
import com.atlassian.bitbucket.test.rest.commit.RestCommitHelper;
import com.atlassian.bitbucket.test.rest.commit.RestDeleteCommitCommentRequest;
import com.atlassian.bitbucket.test.rest.commit.RestUpdateCommitCommentRequest;
import com.atlassian.plugins.slack.test.RequestMatchers;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import it.com.atlassian.bitbucket.plugins.slack.util.SlackFunctionalTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.atlassian.plugins.slack.test.RequestMatchers.hasHit;
import static com.atlassian.plugins.slack.test.TestChannels.PUBLIC;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class CommitNotificationsFuncTest extends SlackFunctionalTestBase {
    private final String commitId = DefaultFuncTestData.getProject1Repository1Commit1().id;
    private final String initialComment = "Test Comment Version 1";
    private final String updatedComment = "Test Comment Version 2 -- updated";
    private final String replyComment = "Test Reply";
    private RestCommitHelper commitHelper;

    @BeforeEach
    void beforeEach() {
        connectToDummyTeamAndConfirmAdminAccount();
        commitHelper = new RestCommitHelper(
                DefaultFuncTestData.getAdminAuthentication(),
                DefaultFuncTestData.getProject1(),
                DefaultFuncTestData.getProject1Repository1(),
                commitId);
    }

    @Test
    void addCommitComment_withNotificationEnabled_shouldPostNotification() {
        connectRepo1ToPublicChannelWithSingleNotificationEnabled(RepositoryNotificationTypes.COMMENT_ADDED.getKey());

        addCommentAndWaitForNotification();

        assertNotificationSent("commented", initialComment);
    }

    @Test
    void addCommitComment_withNotificationDisabled_shouldNotPostNotification() {
        connectRepo1ToPublicChannelWithSingleNotificationDisabled(RepositoryNotificationTypes.COMMENT_ADDED.getKey());

        server.clearHistoryExecuteAndExpectNoRequests(CHAT_POST_MESSAGE, this::addComment);
    }

    @Test
    void editCommitComment_withNotificationEnabled_shouldPostNotification() {
        connectRepo1ToPublicChannelWithSingleNotificationEnabled(RepositoryNotificationTypes.COMMENT_EDITED.getKey());

        int commentId = addComment();
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                commitHelper.updateComment(new RestUpdateCommitCommentRequest
                        .Builder(commentId, RestCommentUtils.createBodyWithVersion(updatedComment, 0))
                        .build()));

        assertNotificationSent("edited a comment", updatedComment);
    }

    @Test
    void editCommitComment_withNotificationDisabled_shouldNotPostNotification() {
        connectRepo1ToPublicChannelWithSingleNotificationDisabled(RepositoryNotificationTypes.COMMENT_EDITED.getKey());

        int commentId = addCommentAndWaitForNotification();
        server.clearHistoryExecuteAndExpectNoRequests(CHAT_POST_MESSAGE, () ->
                commitHelper.updateComment(new RestUpdateCommitCommentRequest
                        .Builder(commentId, RestCommentUtils.createBodyWithVersion(updatedComment, 0))
                        .build()));
    }

    @Test
    void replyCommitComment_withNotificationEnabled_shouldPostNotification() {
        connectRepo1ToPublicChannelWithSingleNotificationEnabled(RepositoryNotificationTypes.COMMENT_REPLIED.getKey());

        int commentId = addComment();
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                commitHelper.addReply(commentId, replyComment, DefaultFuncTestData.getAdminUser()));

        assertNotificationSent("replied to a comment", replyComment);
    }

    @Test
    void replyCommitComment_withNotificationDisabled_shouldNotPostNotification() {
        connectRepo1ToPublicChannelWithSingleNotificationDisabled(RepositoryNotificationTypes.COMMENT_REPLIED.getKey());

        int commentId = addCommentAndWaitForNotification();
        server.clearHistoryExecuteAndExpectNoRequests(CHAT_POST_MESSAGE, () ->
                commitHelper.addReply(commentId, replyComment, DefaultFuncTestData.getAdminUser()));
    }

    @Test
    void deleteCommitComment_withNotificationEnabled_shouldPostNotification() {
        connectRepo1ToPublicChannelWithSingleNotificationEnabled(RepositoryNotificationTypes.COMMENT_DELETED.getKey());

        int commentId = addComment();
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                commitHelper.deleteComment(new RestDeleteCommitCommentRequest.Builder(commentId).version(0).build()));

        assertNotificationSent("deleted a comment", initialComment);
    }

    @Test
    void deleteCommitComment_withNotificationDisabled_shouldNotPostNotification() {
        connectRepo1ToPublicChannelWithSingleNotificationDisabled(RepositoryNotificationTypes.COMMENT_DELETED.getKey());

        int commentId = addCommentAndWaitForNotification();
        server.clearHistoryExecuteAndExpectNoRequests(CHAT_POST_MESSAGE, () ->
                commitHelper.deleteComment(new RestDeleteCommitCommentRequest.Builder(commentId).version(0).build()));
    }

    private int addCommentAndWaitForNotification() {
        // adding comments would generate a notification, so we need to wait for it
        return server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, this::addComment);
    }

    private int addComment() {
        return commitHelper.addLineComment(
                initialComment,
                DefaultFuncTestData.getProject1Repository1Commit1File1(),
                DefaultFuncTestData.getProject1Repository1Commit1File1(),
                1,
                DiffSegmentType.ADDED,
                DiffFileType.TO);
    }

    private void assertNotificationSent(String action, String commentText) {
        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(allOf(
                RequestMatchers.requestEntityProperty(ChatPostMessageRequest::getText, allOf(
                        containsString("*" + action + "* on commit"),
                        containsString(commitId),
                        containsString(commentText))),
                RequestMatchers.requestEntityProperty(ChatPostMessageRequest::getChannel, is(PUBLIC.getId()))
        ))));
    }
}
