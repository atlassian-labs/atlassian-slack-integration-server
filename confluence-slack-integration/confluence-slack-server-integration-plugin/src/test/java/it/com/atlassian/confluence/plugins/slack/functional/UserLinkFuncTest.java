package it.com.atlassian.confluence.plugins.slack.functional;

import com.atlassian.plugins.slack.test.client.TestClient;
import it.com.atlassian.confluence.plugins.slack.util.SlackFunctionalTestBase;
import org.junit.jupiter.api.Test;

import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class UserLinkFuncTest extends SlackFunctionalTestBase {
    @Test
    void confirmThenRemoveConfirmationFromAdminAccount() {
        confirmThenRemoveConfirmationFor(client.admin(), this::connectToDummyTeamAndConfirmAdminAccount);
    }

    @Test
    void confirmThenRemoveConfirmationFromRegularAccount() {
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
