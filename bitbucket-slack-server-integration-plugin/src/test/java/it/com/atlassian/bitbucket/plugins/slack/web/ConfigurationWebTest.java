package it.com.atlassian.bitbucket.plugins.slack.web;

import com.atlassian.bitbucket.plugins.slack.notification.NotificationUtil;
import com.atlassian.bitbucket.test.DefaultFuncTestData;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsInviteRequest;
import it.com.atlassian.bitbucket.plugins.slack.pageobjects.ChannelSelector;
import it.com.atlassian.bitbucket.plugins.slack.pageobjects.ConfigurationSection;
import it.com.atlassian.bitbucket.plugins.slack.pageobjects.ConnectionStatus;
import it.com.atlassian.bitbucket.plugins.slack.pageobjects.MappingRow;
import it.com.atlassian.bitbucket.plugins.slack.pageobjects.MappingTable;
import it.com.atlassian.bitbucket.plugins.slack.pageobjects.RepoConfigurationPage;
import it.com.atlassian.bitbucket.plugins.slack.util.SlackWebTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.atlassian.plugins.slack.test.RequestMatchers.hasHit;
import static com.atlassian.plugins.slack.test.RequestMatchers.requestEntityProperty;
import static com.atlassian.plugins.slack.test.TestChannels.PUBLIC;
import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM;
import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM_ID;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static com.github.seratch.jslack.api.methods.Methods.CONVERSATIONS_INVITE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class ConfigurationWebTest extends SlackWebTestBase {
    @BeforeEach
    void beforeEach() {
        connectToDummyTeamAndConfirmAdminAccount();
    }

    @Test
    void addNewConfiguration() {
        final RepoConfigurationPage configPage = bitbucket.loginAsAdmin(RepoConfigurationPage.class,
                DefaultFuncTestData.getProject1(),
                DefaultFuncTestData.getProject1Repository1(),
                DUMMY_TEAM_ID);

        assertThat(configPage.isLinked(), is(true));
        ConnectionStatus workspaceConnection = configPage.workspaceConnection();
        assertThat(workspaceConnection.isConnectedLimitedly() || workspaceConnection.isConnected(), is(true));
        assertThat(configPage.userConnection().isConnected(), is(true));
        assertThat(configPage.workspaceSelector().selectedTeamName(), is(DUMMY_TEAM.getTeamName()));

        final ConfigurationSection configurationSection = configPage.geConfigurationSection();
        final ChannelSelector channelSelector = configurationSection.getChannelSelector();
        channelSelector.selectOption(PUBLIC.getName());

        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () -> {
            configurationSection.clickAddButton();

            final Optional<MappingRow> mapping = findMappingRow(configurationSection.getMappingTable(), PUBLIC.getId());
            assertThat(mapping.isPresent(), is(true));

            final List<String> configuredNotifications = mapping.get().getConfiguredNotifications();
            assertThat(configuredNotifications, containsInAnyOrder(NotificationUtil.ALL_NOTIFICATION_TYPE_KEYS.toArray()));
        });

        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(
                requestEntityProperty(ChatPostMessageRequest::getText, containsString(
                        "has set up Bitbucket Server notifications for this channel. Notifications for events from the repository, *<http://example.com/context/projects/PROJECT_1/repos/rep_1/browse?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258cmVwb3NpdG9yeQ%3D%3D|Project 1/rep_1>*, will now appear here."))
        )));

        assertThat(server.requestHistoryForTest(), hasHit(CONVERSATIONS_INVITE, contains(allOf(
                requestEntityProperty(ConversationsInviteRequest::getUsers, contains(DUMMY_TEAM.getBotUserId())),
                requestEntityProperty(ConversationsInviteRequest::getChannel, is(PUBLIC.getId()))
        ))));
    }

    @Test
    void deleteConfiguration() {
        connectRepo1ToPublicChannel();

        final RepoConfigurationPage configPage = bitbucket.loginAsAdmin(RepoConfigurationPage.class,
                DefaultFuncTestData.getProject1(),
                DefaultFuncTestData.getProject1Repository1(),
                DUMMY_TEAM_ID);

        final ConfigurationSection configurationSection = configPage.geConfigurationSection();
        final Optional<MappingRow> mapping = findMappingRow(configurationSection.getMappingTable(), PUBLIC.getId());
        assertThat(mapping.isPresent(), is(true));

        mapping.get().clickTrashButton().clickMainAction();

        Poller.waitUntilFalse(Conditions.forSupplier(() ->
                findMappingRow(configurationSection.getMappingTable(), PUBLIC.getId()).isPresent()));
    }

    private Optional<MappingRow> findMappingRow(final MappingTable mappingTable, final String channelId) {
        return mappingTable.getTableRows().stream()
                .filter(r -> channelId.equals(r.getChannelId().byDefaultTimeout()))
                .findFirst();
    }
}
