package it.com.atlassian.bitbucket.plugins.slack.util;

import com.atlassian.bitbucket.test.DefaultFuncTestData;
import com.atlassian.plugins.slack.admin.InstallationCompletionData;
import com.atlassian.plugins.slack.test.ServerDiscovery;
import com.atlassian.plugins.slack.test.UserCredentials;
import com.atlassian.plugins.slack.test.client.TestClientExtension;
import com.atlassian.plugins.slack.test.mockserver.SlackMockServerExtension;
import com.atlassian.webdriver.bitbucket.BitbucketTestedProduct;
import it.com.atlassian.bitbucket.plugins.slack.util.model.RepositoryResponse;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Function;

import static com.atlassian.bitbucket.test.DefaultFuncTestData.getAdminAuthentication;
import static com.atlassian.bitbucket.test.DefaultFuncTestData.getRegularAuthentication;
import static com.atlassian.plugins.slack.test.TestChannels.PUBLIC;
import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM;
import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM_ID;
import static com.atlassian.plugins.slack.test.TestUsers.ADMIN_USER;
import static com.atlassian.plugins.slack.test.TestUsers.REGULAR_USER;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class SlackFunctionalTestBase {
    final static Function<ExtensionContext, String> testNameProvider = context -> context.getRequiredTestMethod().getName();

    @RegisterExtension
    protected static TestClientExtension<BitbucketTestClient> client = new TestClientExtension<>(
            BitbucketTestClient::new,
            new UserCredentials(getAdminAuthentication().getUsername(), getAdminAuthentication().getPassword()),
            new UserCredentials(getRegularAuthentication().getUsername(), getRegularAuthentication().getPassword()),
            () -> ServerDiscovery.instance("bitbucket", BitbucketTestedProduct.class,
                    new UserCredentials(getAdminAuthentication().getUsername(), getAdminAuthentication().getPassword())),
            testNameProvider);

    @RegisterExtension
    protected static SlackMockServerExtension server = new SlackMockServerExtension(
            () -> new HashSet<>(Arrays.asList(
                    client.admin().getDefaultBaseUrl().toString(),
                    client.instance().getBaseUrl())
            ),
            testNameProvider);

    protected static RepositoryResponse repository1;

    protected void connectToDummyTeamWithCustomInstall() {
        connectToDummyTeam(false);
    }

    protected void connectToDummyTeamWithCustomApp() {
        connectToDummyTeam(true);
    }

    protected void connectToDummyTeam(boolean customApp) {
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () -> {
            final InstallationCompletionData data = client.admin().slackLink().connectTeam(DUMMY_TEAM, customApp);
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

    protected void connectRepo1ToPublicChannel() {
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () ->
                client.admin().notifications().createNew(
                        getCachedRepository1().getId(),
                        DUMMY_TEAM.getTeamId(),
                        PUBLIC.getId()));

    }

    protected void connectRepo1ToPublicChannelWithSingleNotificationEnabled(String notificationKey) {
        client.admin().notifications().enable(
                getCachedRepository1().getId(),
                DUMMY_TEAM.getTeamId(),
                PUBLIC.getId(),
                notificationKey);
    }

    protected void connectRepo1ToPublicChannelWithSingleNotificationDisabled(String notificationKey) {
        connectRepo1ToPublicChannel();
        client.admin().notifications().disable(
                getCachedRepository1().getId(),
                DUMMY_TEAM.getTeamId(),
                PUBLIC.getId(),
                notificationKey);
    }

    protected RepositoryResponse getCachedRepository1() {
        if (repository1 == null) {
            repository1 = client.admin().bitbucket().getRepository(
                    DefaultFuncTestData.getProject1(),
                    DefaultFuncTestData.getProject1Repository1());
        }
        return repository1;
    }
}
