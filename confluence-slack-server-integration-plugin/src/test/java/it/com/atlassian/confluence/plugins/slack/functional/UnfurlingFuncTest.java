package it.com.atlassian.confluence.plugins.slack.functional;

import com.atlassian.plugins.slack.test.mockserver.RequestHistoryItem;
import com.github.seratch.jslack.api.methods.request.chat.ChatUnfurlRequest;
import it.com.atlassian.confluence.plugins.slack.util.SlackFunctionalTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.atlassian.plugins.slack.test.RequestMatchers.hasHit;
import static com.atlassian.plugins.slack.test.RequestMatchers.requestEntityProperty;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_UNFURL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;

public class UnfurlingFuncTest extends SlackFunctionalTestBase {
    @Test
    void pageUnfurling() {
        createTestSpace();
        connectToDummyTeamWithCustomApp();
        confirmAdminAccount();

        String pageUrl = client.instance().getBaseUrl() + "/display/IT/IT+Space+Home";

        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_UNFURL, () ->
                client.admin().events().linkShared(pageUrl));

        List<RequestHistoryItem> history = server.requestHistoryForTest();
        assertThat(history, hasHit(CHAT_UNFURL, contains(
                requestEntityProperty(ChatUnfurlRequest::getUnfurls, containsString("IT Space Home"))
        )));
    }
}
