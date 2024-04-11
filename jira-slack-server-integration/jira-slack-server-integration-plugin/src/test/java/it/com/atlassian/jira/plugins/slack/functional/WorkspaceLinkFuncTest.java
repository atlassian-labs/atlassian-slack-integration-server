package it.com.atlassian.jira.plugins.slack.functional;

import com.atlassian.jira.functest.rule.SkipCacheCheck;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import it.com.atlassian.jira.plugins.slack.util.SlackFunctionalTestBase;
import org.junit.Before;
import org.junit.Test;

import static com.atlassian.plugins.slack.test.RequestMatchers.body;
import static com.atlassian.plugins.slack.test.RequestMatchers.hasHit;
import static com.atlassian.plugins.slack.test.RequestMatchers.request;
import static com.atlassian.plugins.slack.test.RequestMatchers.requestEntityProperty;
import static com.atlassian.plugins.slack.test.TestChannels.DIRECT;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.SAMPLE_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@SkipCacheCheck
public class WorkspaceLinkFuncTest extends SlackFunctionalTestBase {
    @Before
    public void beforeEach() {
        backdoor.restoreDataFromResource(SAMPLE_DATA);
    }

    @Test
    public void connectToDummyTeamAndCheckWelcomeMessage() {
        // do some action and make sure it has generated a request to slack of the expected type
        connectToDummyTeamWithCustomInstall();

        // the following lines exemplify three ways to assert requests made to the Slack Mock Server

        // individual properties' assertion of the posted object using jslack type
        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(allOf(
                requestEntityProperty(ChatPostMessageRequest::getText,
                        containsString(":tada: Hey there! Thank you for connecting your Slack workspace")),
                requestEntityProperty(ChatPostMessageRequest::getChannel, is(DIRECT.getId()))
        ))));

        // raw posted body assertion
        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(body(allOf(
                containsString("Hey%20there"),
                containsString(DIRECT.getId())
        )))));

        // exact assertion of the posted object using jslack type
        // be careful if local username is different
        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(request(is(
                ChatPostMessageRequest.builder()
                        .channel(DIRECT.getId())
                        .text(":tada: Hey there! Thank you for connecting your Slack workspace to <http://example.com/context?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258c2l0ZQ%3D%3D|Super Jira>.\n"
                                + "Now you can start managing notifications in the <http://example.com/context/plugins/servlet/slack/configure?teamId=DUMMY-T123456|admin configuration page>.")
                        .build()
        )))));
    }
}
