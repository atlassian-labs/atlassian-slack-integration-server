package it.com.atlassian.jira.plugins.slack.functional;

import com.atlassian.jira.functest.rule.SkipCacheCheck;
import it.com.atlassian.jira.plugins.slack.util.SlackFunctionalTestBase;
import org.junit.Before;
import org.junit.Test;

import static com.atlassian.jira.testkit.client.util.TimeBombLicence.LICENCE_FOR_TESTING;
import static com.atlassian.plugins.slack.test.RequestMatchers.success;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.SAMPLE_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@SkipCacheCheck
public class SanityCheckFuncTest extends SlackFunctionalTestBase {
    @Before
    public void beforeEach() {
        backdoor.restoreDataFromResource(SAMPLE_DATA, LICENCE_FOR_TESTING);
    }

    @Test
    public void openIssuePage() {
        assertThat(
                client.admin().visitPage("browse/PRO-1"),
                success(containsString("<title>[PRO-1] This is your first task - Super Jira</title>")));
    }

    @Test
    public void openGlobalConfigurationPage() {
        assertThat(
                client.admin().visitPage("plugins/servlet/slack/configure"),
                success(containsString("<h1>Slack integration</h1>")));
    }

    @Test
    public void openPersonalNotificationPage() {
        assertThat(
                client.admin().visitPage("plugins/servlet/slack/personal-notifications"),
                success(containsString("When issues that are assigned to me are updated")));
    }
}
