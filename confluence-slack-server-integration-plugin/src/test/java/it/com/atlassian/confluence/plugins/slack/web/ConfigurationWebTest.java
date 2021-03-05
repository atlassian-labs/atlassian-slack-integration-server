package it.com.atlassian.confluence.plugins.slack.web;

import com.atlassian.confluence.test.api.model.person.UserWithDetails;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsInviteRequest;
import it.com.atlassian.confluence.plugins.slack.pageobjects.ConfigurationSection;
import it.com.atlassian.confluence.plugins.slack.pageobjects.ConnectionStatus;
import it.com.atlassian.confluence.plugins.slack.pageobjects.MappingRow;
import it.com.atlassian.confluence.plugins.slack.pageobjects.MappingTable;
import it.com.atlassian.confluence.plugins.slack.pageobjects.SpaceConfigurationPage;
import it.com.atlassian.confluence.plugins.slack.util.SlackWebTestBase;
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
        createTestSpace();
    }

    @Test
    void addNewConfiguration() {
        final SpaceConfigurationPage configPage = confluence.loginAs(
                UserWithDetails.ADMIN, SpaceConfigurationPage.class, SPACE_KEY, DUMMY_TEAM_ID);
        closeAuiFlags();

        assertThat(configPage.isLinked(), is(true));
        ConnectionStatus workspaceConnection = configPage.workspaceConnection();
        assertThat(workspaceConnection.isConnectedLimitedly() || workspaceConnection.isConnected(), is(true));
        assertThat(configPage.userConnection().isConnected(), is(true));
        assertThat(configPage.workspaceSelector().selectedTeamName(), is(DUMMY_TEAM.getTeamName()));

        final ConfigurationSection configurationSection = configPage.geConfigurationSection();
        configurationSection.selectChannel(PUBLIC.getName());

        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () -> {
            MappingRow mapping = configurationSection.clickAddButton();

            final List<String> configuredNotifications = mapping.getConfiguredNotifications();
            assertThat(configuredNotifications, containsInAnyOrder("BlogCreate", "PageCreate"));
        });

        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(
                requestEntityProperty(ChatPostMessageRequest::getText, containsString(
                        "set Confluence notifications from space *<http://example.com/context/display/IT?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258c3BhY2U%3D|IT Space>* to appear in this channel."))
        )));

        assertThat(server.requestHistoryForTest(), hasHit(CONVERSATIONS_INVITE, contains(allOf(
                requestEntityProperty(ConversationsInviteRequest::getUsers, contains(DUMMY_TEAM.getBotUserId())),
                requestEntityProperty(ConversationsInviteRequest::getChannel, is(PUBLIC.getId()))
        ))));
    }

    @Test
    void deleteConfiguration() {
        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () -> {
            client.admin().notifications().enable(
                    SPACE_KEY,
                    DUMMY_TEAM.getTeamId(),
                    PUBLIC.getId(),
                    "BlogCreate",
                    true);
        });

        final SpaceConfigurationPage configPage = confluence.loginAs(
                UserWithDetails.ADMIN, SpaceConfigurationPage.class, SPACE_KEY, DUMMY_TEAM_ID);
        closeAuiFlags();

        final ConfigurationSection configurationSection = configPage.geConfigurationSection();
        final Optional<MappingRow> mapping = findMappingRow(configurationSection.getMappingTable(), PUBLIC.getId());
        assertThat(mapping.isPresent(), is(true));

        mapping.get().clickTrashButton();

        int twoSecondsTimeout = 2 * 1000;
        Poller.waitUntilFalse(Conditions.forSupplier(twoSecondsTimeout, () ->
                findMappingRow(configurationSection.getMappingTable(), PUBLIC.getId()).isPresent()));
    }

    private Optional<MappingRow> findMappingRow(final MappingTable mappingTable, final String channelId) {
        return mappingTable.getTableRows().stream()
                .filter(r -> channelId.equals(r.getChannelId().byDefaultTimeout()))
                .findFirst();
    }
}
