package it.com.atlassian.bitbucket.plugins.slack.functional;

import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import it.com.atlassian.bitbucket.plugins.slack.util.SlackFunctionalTestBase;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.atlassian.plugins.slack.test.RequestMatchers.body;
import static com.atlassian.plugins.slack.test.RequestMatchers.hasHit;
import static com.atlassian.plugins.slack.test.RequestMatchers.request;
import static com.atlassian.plugins.slack.test.RequestMatchers.requestEntityProperty;
import static com.atlassian.plugins.slack.test.TestChannels.DIRECT;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class WorkspaceLinkFuncTest extends SlackFunctionalTestBase {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceLinkFuncTest.class);

    @Test
    void connectToDummyTeamAndCheckWelcomeMessage() {
        // do some action and make sure it has generated a request to slack of the expected type
        connectToDummyTeamWithCustomInstall();

        // the following lines exemplify three ways to assert requests made to the Slack Mock Server

        // individual properties' assertion of the posted object using jslack type
        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(allOf(
                requestEntityProperty(ChatPostMessageRequest::getText,
                        containsString("This workspace is now connected to")),
                requestEntityProperty(ChatPostMessageRequest::getChannel, is(DIRECT.getId()))
        ))));

        // raw posted body assertion
        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(body(allOf(
                containsString("now%20connected"),
                containsString(DIRECT.getId())
        )))));

        String applicationTitle = fetchApplicationTitle();

        // exact assertion of the posted object using jslack type
        // be careful if local username is different
        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(request(is(
                ChatPostMessageRequest.builder()
                        .channel(DIRECT.getId())
                        .text("This workspace is now connected to <http://example.com/context/dashboard?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258c2l0ZQ%3D%3D|" + applicationTitle + "> :tada:\n"
                                + "The next step is to confirm your account in Bitbucket Data Center. Then you can choose events to see notifications for, "
                                + "and the channels where these notifications will appear. You'll also be able to take action on some notifications "
                                + "and unfurl relevant Bitbucket Data Center links when they're pasted in Slack. "
                                + "If you're an admin of this Bitbucket Data Center instance, you can configure this integration in the <http://example.com/context/plugins/servlet/slack/configure?teamId=DUMMY-T123456|administration page>. "
                                + "For more help, take a look at our <https://confluence.atlassian.com/slack/use-slack-and-bitbucket-server-together-974387205.html|documentation>.")
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
