package it.com.atlassian.jira.plugins.slack.functional;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.functest.rule.SkipCacheCheck;
import it.com.atlassian.jira.plugins.slack.util.SlackFunctionalTestBase;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;

import static com.atlassian.plugins.slack.test.RequestMatchers.success;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.SAMPLE_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@SkipCacheCheck
public class SanityCheckFuncTest extends SlackFunctionalTestBase {

    @Before
    public void beforeEach() {
        backdoor.restoreDataFromResource(SAMPLE_DATA);
    }

    @Test
    public void openIssuePage() {
        Response pageHtml = client.admin().visitPage("browse/PRO-1");
        assertThat(
                pageHtml,
                success(containsString("[PRO-1] This is your first task")));
    }

    @Test
    public void openGlobalConfigurationPage() {
        //websudo is tested in webdriver test ConfigurationWebTest
        boolean originalWebSudoState = backdoor.applicationProperties().getOption(APKeys.WebSudo.IS_DISABLED);
        try {
            backdoor.applicationProperties().setOption(APKeys.WebSudo.IS_DISABLED, true);
            assertThat(
                    client.admin().visitPage("plugins/servlet/slack/configure"),
                    success(containsString("<h1>Slack integration</h1>")));
        } finally {
            backdoor.applicationProperties().setOption(APKeys.WebSudo.IS_DISABLED, originalWebSudoState);
        }
    }

    @Test
    public void openPersonalNotificationPage() {
        assertThat(
                client.admin().visitPage("plugins/servlet/slack/personal-notifications"),
                success(containsString("When issues that are assigned to me are updated")));
    }
}
