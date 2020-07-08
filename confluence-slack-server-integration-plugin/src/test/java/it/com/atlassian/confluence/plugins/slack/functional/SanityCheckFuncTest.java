package it.com.atlassian.confluence.plugins.slack.functional;

import it.com.atlassian.confluence.plugins.slack.util.SlackFunctionalTestBase;
import org.junit.jupiter.api.Test;

import static com.atlassian.plugins.slack.test.RequestMatchers.success;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class SanityCheckFuncTest extends SlackFunctionalTestBase {
    @Test
    void openDashboard() {
        assertThat(
                client.admin().visitPage(""),
                success(containsString("<title>Dashboard")));
    }

    @Test
    void openGlobalConfigurationPage() {
        assertThat(
                client.admin().visitPage("plugins/servlet/slack/configure"),
                success(containsString("Slack integration")));
    }

    @Test
    public void openPersonalNotificationPage() {
        assertThat(
                client.admin().visitPage("plugins/servlet/slack/personal-notifications"),
                success(containsString("When pages and posts")));
    }
}
