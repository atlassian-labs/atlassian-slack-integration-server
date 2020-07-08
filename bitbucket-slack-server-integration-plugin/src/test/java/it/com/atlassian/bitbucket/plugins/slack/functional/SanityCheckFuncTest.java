package it.com.atlassian.bitbucket.plugins.slack.functional;

import it.com.atlassian.bitbucket.plugins.slack.util.SlackFunctionalTestBase;
import org.junit.jupiter.api.Test;

import static com.atlassian.plugins.slack.test.RequestMatchers.success;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class SanityCheckFuncTest extends SlackFunctionalTestBase {
    @Test
    void openDashboard() {
        assertThat(
                client.admin().visitPage("dashboard"),
                success(containsString("<title>Your work")));
    }

    @Test
    void openGlobalConfigurationPage() {
        assertThat(
                client.admin().visitPage("plugins/servlet/slack/configure"),
                success(containsString("<h1>Slack integration</h1>")));
    }

    @Test
    public void openPersonalNotificationPage() {
        assertThat(
                client.admin().visitPage("plugins/servlet/slack/personal-notifications"),
                success(containsString("A pull request that")));
    }
}
