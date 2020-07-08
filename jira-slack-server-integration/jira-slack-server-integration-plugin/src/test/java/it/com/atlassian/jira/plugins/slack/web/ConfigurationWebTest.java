package it.com.atlassian.jira.plugins.slack.web;

import com.atlassian.jira.functest.rule.SkipCacheCheck;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsInviteRequest;
import it.com.atlassian.jira.plugins.slack.pageobjects.ConfigurationSection;
import it.com.atlassian.jira.plugins.slack.pageobjects.MappingRow;
import it.com.atlassian.jira.plugins.slack.pageobjects.MappingTable;
import it.com.atlassian.jira.plugins.slack.pageobjects.ProjectConfigurationPage;
import it.com.atlassian.jira.plugins.slack.util.FlakyWebTestKiller;
import it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData;
import it.com.atlassian.jira.plugins.slack.util.SlackWebTestBase;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static com.atlassian.jira.testkit.client.util.TimeBombLicence.LICENCE_FOR_TESTING;
import static com.atlassian.plugins.slack.test.RequestMatchers.hasHit;
import static com.atlassian.plugins.slack.test.RequestMatchers.requestEntityProperty;
import static com.atlassian.plugins.slack.test.TestChannels.PUBLIC;
import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM;
import static com.atlassian.plugins.slack.test.TestTeams.DUMMY_TEAM_ID;
import static com.github.seratch.jslack.api.methods.Methods.CHAT_POST_MESSAGE;
import static com.github.seratch.jslack.api.methods.Methods.CONVERSATIONS_INVITE;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.ADMIN_PASSWORD;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.ADMIN_USERNAME;
import static it.com.atlassian.jira.plugins.slack.util.JiraFuncTestData.SAMPLE_DATA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@SkipCacheCheck
public class ConfigurationWebTest extends SlackWebTestBase {
    @Inject
    FlakyWebTestKiller flakyWebTestKiller;

    @Before
    public void beforeEach() {
        backdoor.restoreDataFromResource(SAMPLE_DATA, LICENCE_FOR_TESTING);
        flakyWebTestKiller.disableJiraFlags();
        connectToDummyTeamAndConfirmAdminAccount();
    }

    @Test
    public void addNewConfiguration() {
        final ProjectConfigurationPage configPage = jira.quickLogin(ADMIN_USERNAME, ADMIN_PASSWORD,
                ProjectConfigurationPage.class, JiraFuncTestData.PROJECT_KEY, DUMMY_TEAM_ID);
        flakyWebTestKiller.closeAllOpenedFlagsAndTips();

        assertThat(configPage.isLinked(), is(true));
        assertThat(configPage.workspaceConnection().isConnectedLimitedly(), is(true));
        assertThat(configPage.userConnection().isConnected(), is(true));
        assertThat(configPage.workspaceSelector().selectedTeamName(), is(DUMMY_TEAM.getTeamName()));

        final ConfigurationSection configurationSection = configPage.geConfigurationSection();
        configurationSection.selectChannel(PUBLIC.getName());

        server.clearHistoryExecuteAndWaitForNewRequest(CHAT_POST_MESSAGE, () -> {
            MappingRow mapping = configurationSection.clickAddButton();

            final List<String> configuredNotifications = mapping.getCheckedNotifications();
            assertThat(configuredNotifications, containsInAnyOrder("IssueCreate"));
        });

        assertThat(server.requestHistoryForTest(), hasHit(CHAT_POST_MESSAGE, contains(
                requestEntityProperty(ChatPostMessageRequest::getText, containsString(
                        "set Jira notifications from project *<http://example.com/context/projects/PRO?atlLinkOrigin=c2xhY2staW50ZWdyYXRpb258cHJvamVjdA%3D%3D|PROJ>* to appear in this channel."))
        )));

        assertThat(server.requestHistoryForTest(), hasHit(CONVERSATIONS_INVITE, contains(allOf(
                requestEntityProperty(ConversationsInviteRequest::getUsers, contains(DUMMY_TEAM.getBotUserId())),
                requestEntityProperty(ConversationsInviteRequest::getChannel, is(PUBLIC.getId()))
        ))));
    }

    @Test
    public void deleteConfiguration() {
        connectProject1ToPublicChannel();

        final ProjectConfigurationPage configPage = jira.quickLogin(ADMIN_USERNAME, ADMIN_PASSWORD,
                ProjectConfigurationPage.class, JiraFuncTestData.PROJECT_KEY, DUMMY_TEAM_ID);
        flakyWebTestKiller.closeAllOpenedFlagsAndTips();

        final ConfigurationSection configurationSection = configPage.geConfigurationSection();
        final Optional<MappingRow> mapping = findMappingRow(configurationSection.getMappingTable(), PUBLIC.getId());
        assertThat(mapping.isPresent(), is(true));

        mapping.get().clickTrashButton();

        Poller.waitUntilFalse(Conditions.forSupplier(() ->
                findMappingRow(configurationSection.getMappingTable(), PUBLIC.getId()).isPresent()));
    }

    private Optional<MappingRow> findMappingRow(final MappingTable mappingTable, final String channelId) {
        return mappingTable.getTableRows().stream()
                .filter(r -> channelId.equals(r.getChannelId().byDefaultTimeout()))
                .findFirst();
    }
}
