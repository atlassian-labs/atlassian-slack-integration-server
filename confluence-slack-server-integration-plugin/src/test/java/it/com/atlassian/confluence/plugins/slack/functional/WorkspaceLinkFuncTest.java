package it.com.atlassian.confluence.plugins.slack.functional;

import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import it.com.atlassian.confluence.plugins.slack.util.SlackFunctionalTestBase;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.atlassian.plugins.slack.test.RequestMatchers.hasHit;
import static com.atlassian.plugins.slack.test.RequestMatchers.request;
import static com.atlassian.plugins.slack.test.TestChannels.DIRECT;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

public class WorkspaceLinkFuncTest extends SlackFunctionalTestBase {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceLinkFuncTest.class);

    @Test
    void connectToDummyTeamAndCheckWelcomeMessage() {
        // do some action and make sure it has generated a request to slack of the expected type
        connectToDummyTeamWithCustomInstall();

        // verify message posted
        String applicationTitle = fetchApplicationTitle();
        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(request(is(
                ChatPostMessageRequest.builder()
                        .channel(DIRECT.getId())
                        .text(":tada: Hey there! Thank you for connecting your Slack workspace to <http://example.com/context?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258c2l0ZQ%3D%3D|" + applicationTitle + ">.\n"
                                + "Now you can start managing notifications in the <http://example.com/context/plugins/servlet/slack/configure?teamId=DUMMY-T123456|admin configuration page>.")
                        .build()
        )))));
    }

    String fetchApplicationTitle() {
        try (Response response = client.admin().visitPage("dashboard")) {
            String body = response.body().string();
            int titleStart = body.indexOf("<title>");
            int separator = body.indexOf('-', titleStart);
            int titleEnd = body.indexOf("</title>", separator);
            return body.substring(separator + 1, titleEnd).trim();
        } catch (IOException e) {
            log.error("Could not fetch dashboard", e);
            return "";
        }
    }
}
