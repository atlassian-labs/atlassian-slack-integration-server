package it.com.atlassian.jira.plugins.slack.functional;

import com.atlassian.jira.functest.rule.SkipCacheCheck;
import com.atlassian.plugins.slack.test.client.TestClient;
import it.com.atlassian.jira.plugins.slack.util.SlackFunctionalTestBase;
import org.junit.Before;
import org.junit.Test;

import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM_ID;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.SAMPLE_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SkipCacheCheck
public class UserLinkFuncTest extends SlackFunctionalTestBase {
    @Before
    public void beforeEach() {
        backdoor.restoreDataFromResource(SAMPLE_DATA);
    }

    @Test
    public void confirmThenRemoveConfirmationFromAdminAccount() {
        confirmThenRemoveConfirmationFor(client.admin(), this::connectToDummyTeamAndConfirmAdminAccount);
    }

    @Test
    public void confirmThenRemoveConfirmationFromRegularAccount() {
        confirmThenRemoveConfirmationFor(client.user(), this::connectToDummyTeamAndConfirmUserAccount);
    }

    static void confirmThenRemoveConfirmationFor(TestClient client, Runnable methodConfirmingAccount) {
        assertThat(client.oAuth().hasUserConfirmed(DUMMY_TEAM_ID), is(false));
        methodConfirmingAccount.run();
        assertThat(client.oAuth().hasUserConfirmed(DUMMY_TEAM_ID), is(true));
        client.oAuth().unlinkAccount(DUMMY_TEAM_ID);
        assertThat(client.oAuth().hasUserConfirmed(DUMMY_TEAM_ID), is(false));
    }
}
