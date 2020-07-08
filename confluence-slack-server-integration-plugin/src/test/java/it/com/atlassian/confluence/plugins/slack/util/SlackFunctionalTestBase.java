package it.com.atlassian.confluence.plugins.slack.util;

import com.atlassian.confluence.api.model.content.Space;
import com.atlassian.confluence.rest.client.RemoteSpaceServiceImpl;
import com.atlassian.confluence.rest.client.authentication.AuthenticatedWebResourceProvider;
import com.atlassian.confluence.webdriver.pageobjects.ConfluenceTestedProduct;
import com.atlassian.plugins.slack.admin.InstallationCompletionData;
import com.atlassian.plugins.slack.test.ServerDiscovery;
import com.atlassian.plugins.slack.test.UserCredentials;
import com.atlassian.plugins.slack.test.client.TestClientExtension;
import com.atlassian.plugins.slack.test.mockserver.SlackMockServerExtension;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import static com.atlassian.confluence.test.api.model.person.UserWithDetails.ADMIN;
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
    protected static TestClientExtension<ConfluenceTestClient> client = new TestClientExtension<>(
            ConfluenceTestClient::new,
            new UserCredentials(ADMIN.getUsername(), ADMIN.getPassword()),
            new UserCredentials(ADMIN.getUsername(), ADMIN.getPassword()),
            () -> ServerDiscovery.instance("confluence", ConfluenceTestedProduct.class,
                    new UserCredentials(ADMIN.getUsername(), ADMIN.getPassword()), Optional.of(1990)),
            testNameProvider);

    @RegisterExtension
    protected static SlackMockServerExtension server = new SlackMockServerExtension(
            () -> Collections.singleton(client.instance().getBaseUrl()), testNameProvider);

    public static final String SPACE_KEY = "IT";

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

    protected void createTestSpace() {
        final AuthenticatedWebResourceProvider prov = AuthenticatedWebResourceProvider.createWithNewClient(client.instance().getBaseUrl());
        prov.setAuthContext(ADMIN.getUsername(), ADMIN.getPassword().toCharArray());
        final RemoteSpaceServiceImpl spaceService = new RemoteSpaceServiceImpl(prov, MoreExecutors.newDirectExecutorService());
        final Space testSpace = Space.builder().name("IT Space").key(SPACE_KEY).build();
        if (spaceService.find().withKeys(SPACE_KEY).fetchOne().claim().isEmpty()) {
            spaceService.create(testSpace, false).claim();
        }
    }
}
