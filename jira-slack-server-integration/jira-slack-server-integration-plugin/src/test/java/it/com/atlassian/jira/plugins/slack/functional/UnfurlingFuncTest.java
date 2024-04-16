package it.com.atlassian.jira.plugins.slack.functional;

import com.atlassian.jira.functest.rule.SkipCacheCheck;
import com.atlassian.plugins.slack.test.mockserver.RequestHistoryItem;
import com.github.seratch.jslack.api.methods.request.chat.ChatUnfurlRequest;
import it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData;
import it.com.atlassian.jira.plugins.slack.util.SlackFunctionalTestBase;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.atlassian.plugins.slack.test.RequestMatchers.hasHit;
import static com.atlassian.plugins.slack.test.RequestMatchers.requestEntityProperty;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_UNFURL;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.SAMPLE_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;

@SkipCacheCheck
public class UnfurlingFuncTest extends SlackFunctionalTestBase {
    @Before
    public void beforeEach() {
        backdoor.restoreDataFromResource(SAMPLE_DATA);
    }

    @Test
    public void issueUnfurling() {
        connectToDummyTeamWithCustomApp();
        confirmAdminAccount();

        String issueUrl = JiraFuncTestData.DEFAULT_BASE_URL + "/browse/" + JiraFuncTestData.ISSUE_KEY;

        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_UNFURL, () ->
                client.admin().events().linkShared(issueUrl));

        List<RequestHistoryItem> history = server.requestHistoryForTest();
        assertThat(history, hasHit(CHAT_UNFURL, contains(
                requestEntityProperty(ChatUnfurlRequest::getUnfurls, containsString("PRO-1: This is your first task"))
        )));
    }
}
