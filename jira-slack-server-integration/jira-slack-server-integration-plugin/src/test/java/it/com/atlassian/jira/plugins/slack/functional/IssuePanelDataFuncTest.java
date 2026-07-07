package it.com.atlassian.jira.plugins.slack.functional;

import com.atlassian.jira.functest.rule.SkipCacheCheck;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.web.rest.IssuePanelResource.IssuePanelData;
import com.atlassian.plugins.slack.test.RequestMatchers;
import com.atlassian.plugins.slack.test.client.RestException;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData;
import it.com.atlassian.jira.plugins.slack.util.SlackFunctionalTestBase;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static com.atlassian.plugins.slack.test.RequestMatchers.hasHit;
import static com.atlassian.plugins.slack.test.TestChannels.PUBLIC;
import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.ISSUE_KEY;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.SAMPLE_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

/**
 * Functional coverage for the issue-view Slack panel against a Slack mock server, in both directions:
 * - inbound - the panel data REST endpoint the issue view calls to render the panel;
 * - outbound - an issue event producing a Slack notification to a mapped channel.
 */
@SkipCacheCheck
public class IssuePanelDataFuncTest extends SlackFunctionalTestBase {

    @Before
    public void beforeEach() {
        backdoor.restoreDataFromResource(SAMPLE_DATA);
    }

    @Test
    public void panelDataReturnsIssuePayloadWhenTeamConnected() {
        connectToDummyTeamAndConfirmAdminAccount();

        final IssuePanelData panelData = client.admin().issuePanel().fetchData(ISSUE_KEY);

        assertThat(panelData.getIssueKey(), is(ISSUE_KEY));
        assertThat(panelData.getProjectKey(), is(JiraFuncTestData.PROJECT_KEY));
    }

    @Test
    public void panelDataReturnsIssuePayloadWhenNoTeamConnected() {
        final IssuePanelData panelData = client.admin().issuePanel().fetchData(ISSUE_KEY);

        assertThat(panelData.getIssueKey(), is(ISSUE_KEY));
        assertThat(panelData.getProjectKey(), is(JiraFuncTestData.PROJECT_KEY));
    }

    @Test
    public void panelDataReturnsNotFoundForNonExistentIssue() {
        final RestException exception = assertThrows(RestException.class,
                () -> client.admin().issuePanel().fetchData("PRO-999999"));

        assertThat(exception.code(), is(404));
    }

    @Test
    public void issueCreationPostsNotificationToMappedChannel() {
        connectToDummyTeamAndConfirmAdminAccount();
        connectProjectToPublicChannel();
        enableNotification(EventMatcherType.ISSUE_CREATED);

        final AtomicReference<String> issueKey = new AtomicReference<>();
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                issueKey.set(backdoor.issues()
                        .createIssue(JiraFuncTestData.PROJECT_KEY, "Panel outbound notification issue").key));

        // The default mapping verbosity is EXTENDED, which renders the "created" verb in the message text and
        // the issue itself in the attachment, so we assert the identity (issue key) on the attachment
        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(allOf(
                RequestMatchers.requestEntityProperty(ChatPostMessageRequest::getChannel, is(PUBLIC.getId())),
                RequestMatchers.requestEntityProperty(ChatPostMessageRequest::getText, containsString("*created*")),
                RequestMatchers.requestEntityProperty(
                        (ChatPostMessageRequest req) -> req.getAttachments().getFirst().getFallback(),
                        containsString(issueKey.get()))
        ))));
    }

    @Test
    public void issueCommentPostsNotificationToMappedChannel() {
        connectToDummyTeamAndConfirmAdminAccount();
        connectProjectToPublicChannel();
        enableNotification(EventMatcherType.ISSUE_COMMENTED);

        final String issueKey = backdoor.issues()
                .createIssue(JiraFuncTestData.PROJECT_KEY, "Panel outbound comment issue").key;
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                backdoor.issues().commentIssueWithVisibility(issueKey, "A comment that should be notified", null, null));

        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(allOf(
                RequestMatchers.requestEntityProperty(ChatPostMessageRequest::getChannel, is(PUBLIC.getId())),
                RequestMatchers.requestEntityProperty(ChatPostMessageRequest::getText, containsString("*commented*")),
                RequestMatchers.requestEntityProperty(
                        (ChatPostMessageRequest req) -> req.getAttachments().getFirst().getTitle(),
                        containsString(issueKey))
        ))));
    }

    @Test
    public void issueCreationPostsNoNotificationWhenProjectNotMapped() {
        connectToDummyTeamAndConfirmAdminAccount();

        server.clearHistoryExecuteAndExpectNoRequests(CHAT_POST_MESSAGE, () ->
                backdoor.issues().createIssue(JiraFuncTestData.PROJECT_KEY, "Unmapped project issue"));
    }

    private void connectProjectToPublicChannel() {
        client.admin().notifications().createNew(
                JiraFuncTestData.CONFIGURATION_GROUP_ID,
                JiraFuncTestData.PROJECT_ID,
                JiraFuncTestData.PROJECT_KEY,
                DUMMY_TEAM.getTeamId(),
                PUBLIC.getId());
    }

    private void enableNotification(final EventMatcherType matcherType) {
        // Enabling a mapping posts a confirmation message to the channel, so wait for it to settle before the
        // test triggers the event it actually asserts on (mirrors SlackWebTestBase in the web tests)
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                client.admin().notifications().enable(
                        JiraFuncTestData.CONFIGURATION_GROUP_ID,
                        JiraFuncTestData.PROJECT_ID,
                        JiraFuncTestData.PROJECT_KEY,
                        DUMMY_TEAM.getTeamId(),
                        PUBLIC.getId(),
                        matcherType.getDbKey()));
    }
}
