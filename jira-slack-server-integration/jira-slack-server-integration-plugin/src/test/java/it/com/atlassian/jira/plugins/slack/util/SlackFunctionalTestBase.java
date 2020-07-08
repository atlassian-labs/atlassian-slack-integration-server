package it.com.atlassian.jira.plugins.slack.util;

import com.atlassian.jira.functest.framework.BaseJiraRestTest;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.plugins.slack.admin.InstallationCompletionData;
import com.atlassian.plugins.slack.test.ServerDiscovery;
import com.atlassian.plugins.slack.test.UserCredentials;
import com.atlassian.plugins.slack.test.client.TestClientExtension;
import com.atlassian.plugins.slack.test.mockserver.SlackMockServerExtension;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.Collections;

import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM;
import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM_ID;
import static com.atlassian.plugins.slack.test.TestUsers.ADMIN_USER;
import static com.atlassian.plugins.slack.test.TestUsers.REGULAR_USER;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.ADMIN_PASSWORD;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.ADMIN_USERNAME;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.DEFAULT_BASE_URL;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.USER_PASSWORD;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.USER_USERNAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class SlackFunctionalTestBase extends BaseJiraRestTest {
    protected static TestClientExtension<JiraTestClient> client;
    protected static SlackMockServerExtension server;
    private static String testTag;

    @Rule
    public TestRule testIdWatcher = new TestWatcher() {
        protected void starting(Description description) {
            testTag = description.getMethodName() + "-" + System.currentTimeMillis();
        }
    };

    @BeforeClass
    public static void setExtensionsUpForClass() {
        client = new TestClientExtension<>(
                JiraTestClient::new,
                new UserCredentials(ADMIN_USERNAME, ADMIN_PASSWORD),
                new UserCredentials(USER_USERNAME, USER_PASSWORD),
                () -> ServerDiscovery.instance("jira", JiraTestedProduct.class,
                        new UserCredentials(ADMIN_USERNAME, ADMIN_PASSWORD)),
                context -> testTag);
        server = new SlackMockServerExtension(
                () -> Collections.singleton(DEFAULT_BASE_URL.toString()),
                context -> testTag
        );

        server.beforeAll(null);
        client.beforeAll(null);
    }

    @AfterClass
    public static void closeExtensionsForClass() {
        server.close();
    }

    @Before
    public void setExtensionsUpTest() {
        if ("true".equals(System.getenv("DANGER_MODE"))) {
            backdoor.dataImport().turnOnDangerMode();
        }
        server.beforeEach(null);
        client.beforeEach(null);
    }

    protected void connectToDummyTeamWithCustomInstall() {
        connectToDummyTeam(false);
    }

    protected void connectToDummyTeamWithCustomApp() {
        connectToDummyTeam(true);
    }

    protected void connectToDummyTeam(boolean custom) {
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () -> {
            final InstallationCompletionData data = client.admin().slackLink().connectTeam(DUMMY_TEAM, custom);
            assertThat(data, is(new InstallationCompletionData(DUMMY_TEAM.getTeamName(), DUMMY_TEAM_ID)));
        });
    }

    protected void confirmAdminAccount() {
        client.admin().oAuth().confirmAccount(ADMIN_USER.getSlackTeamId(), ADMIN_USER.getSlackUserId());
    }

    protected void confirmUserAccount() {
        client.user().oAuth().confirmAccount(REGULAR_USER.getSlackTeamId(), REGULAR_USER.getSlackUserId());
    }

    protected void connectToDummyTeamAndConfirmAdminAccount() {
        connectToDummyTeamWithCustomInstall();
        confirmAdminAccount();
    }

    protected void connectToDummyTeamAndConfirmUserAccount() {
        connectToDummyTeamWithCustomInstall();
        confirmUserAccount();
    }
}
