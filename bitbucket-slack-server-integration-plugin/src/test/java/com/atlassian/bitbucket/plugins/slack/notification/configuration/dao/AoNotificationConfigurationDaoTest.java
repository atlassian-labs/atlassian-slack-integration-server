package com.atlassian.bitbucket.plugins.slack.notification.configuration.dao;

import com.atlassian.activeobjects.test.TestActiveObjects;
import com.atlassian.bitbucket.ao.AbstractAoDaoTest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.ChannelConfiguration;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.DefaultChannelDetails;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationDisableRequest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationEnableRequest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationSearchRequest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.RepositoryConfiguration;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.ao.AoNotificationConfiguration;
import com.atlassian.bitbucket.plugins.slack.settings.BitbucketSlackSettingsService;
import com.atlassian.bitbucket.plugins.slack.util.TestUtil;
import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.ConversationLoaderHelper;
import com.atlassian.plugins.slack.api.client.ConversationsAndLinks;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.github.seratch.jslack.api.model.Conversation;
import com.google.common.collect.ImmutableMap;
import net.java.ao.EntityManager;
import net.java.ao.Query;
import net.java.ao.test.jdbc.Data;
import net.java.ao.test.jdbc.DatabaseUpdater;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Data(AoNotificationConfigurationDaoTest.DataUpdater.class)
public class AoNotificationConfigurationDaoTest extends AbstractAoDaoTest {
    public static final String TEAM_ID = "someTeamId";
    public static final String CHANNEL_ID = "someChannelId";
    public static final int REPO_ID = 4;

    public static class DataUpdater implements DatabaseUpdater {
        @Override
        public void update(final EntityManager entityManager) throws Exception {
            entityManager.migrate(AoNotificationConfiguration.class);
        }
    }

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ConversationLoaderHelper conversationLoaderHelper;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    BitbucketSlackSettingsService bitbucketSlackSettingsService;

    @Mock
    Repository repository;
    @Mock
    Repository repository2;
    @Mock
    ConversationsAndLinks conversationsAndLinks;
    @Mock
    Conversation conversation;
    @Mock
    SlackLink slackLink;
    @Mock
    Project project;

    private AoNotificationConfigurationDao target;

    public AoNotificationConfigurationDaoTest() {
        super(AoNotificationConfiguration.class);
    }

    @Before
    public void setUp() throws Exception {
        assertNotNull(entityManager);
        target = new AoNotificationConfigurationDao(new TestActiveObjects(entityManager), conversationLoaderHelper,
                repositoryService, transactionTemplate, bitbucketSlackSettingsService);
        TestUtil.bypass(transactionTemplate);
    }

    @Test
    public void create() throws Exception {
        when(repository.getId()).thenReturn(REPO_ID);

        target.create(new NotificationEnableRequest.Builder()
                .teamId(TEAM_ID)
                .channelId(CHANNEL_ID)
                .repository(repository)
                .notificationTypes(Arrays.asList("notif1", "notif2"))
                .build());

        AoNotificationConfiguration[] created = entityManager.find(AoNotificationConfiguration.class,
                Query.select().order("NOTIFICATION_TYPE"));
        assertThat(created.length, is(2));
        assertThat(created[0], allOf(hasProperty("teamId", is(TEAM_ID)), hasProperty("channelId", is(CHANNEL_ID)),
                hasProperty("repoId", is(REPO_ID)), hasProperty("notificationType", is("notif1"))));
        assertThat(created[1], allOf(hasProperty("teamId", is(TEAM_ID)), hasProperty("channelId", is(CHANNEL_ID)),
                hasProperty("repoId", is(REPO_ID)), hasProperty("notificationType", is("notif2"))));
    }

    @Test
    public void deleteByRepoId() throws Exception {
        when(repository.getId()).thenReturn(REPO_ID);
        entityManager.create(AoNotificationConfiguration.class, ImmutableMap.of(
                AoNotificationConfiguration.TEAM_ID_COLUMN, TEAM_ID,
                AoNotificationConfiguration.CHANNEL_ID_COLUMN, CHANNEL_ID,
                AoNotificationConfiguration.REPO_ID_COLUMN, REPO_ID,
                AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, "someNotificationType"
        ));
        assertThat(entityManager.count(AoNotificationConfiguration.class), is(1));

        target.delete(new NotificationDisableRequest.Builder().repository(repository).build());

        assertThat(entityManager.count(AoNotificationConfiguration.class), is(0));
    }

    @Test
    public void removeNotificationsForTeam_shouldPerformExpectedDeletion() throws Exception {
        entityManager.create(AoNotificationConfiguration.class, ImmutableMap.of(
                AoNotificationConfiguration.TEAM_ID_COLUMN, TEAM_ID,
                AoNotificationConfiguration.CHANNEL_ID_COLUMN, CHANNEL_ID,
                AoNotificationConfiguration.REPO_ID_COLUMN, REPO_ID,
                AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, "someNotificationType"
                ));
        assertThat(entityManager.count(AoNotificationConfiguration.class), is(1));

        target.removeNotificationsForTeam(TEAM_ID);

        assertThat(entityManager.count(AoNotificationConfiguration.class), is(0));
    }

    @Test
    public void removeNotificationsForChannel_shouldPerformExpectedDeletion() throws Exception {
        entityManager.create(AoNotificationConfiguration.class, ImmutableMap.of(
                AoNotificationConfiguration.TEAM_ID_COLUMN, TEAM_ID,
                AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, "someNotificationType",
                AoNotificationConfiguration.REPO_ID_COLUMN, REPO_ID,
                AoNotificationConfiguration.CHANNEL_ID_COLUMN, CHANNEL_ID
        ));
        assertThat(entityManager.count(AoNotificationConfiguration.class), is(1));

        target.removeNotificationsForChannel(CHANNEL_ID);

        assertThat(entityManager.count(AoNotificationConfiguration.class), is(0));
    }

    @Test
    public void getChannelsToNotify() throws Exception {
        when(repository.getId()).thenReturn(REPO_ID);
        entityManager.create(AoNotificationConfiguration.class, ImmutableMap.of(
                AoNotificationConfiguration.TEAM_ID_COLUMN, TEAM_ID,
                AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, "someNotificationType1",
                AoNotificationConfiguration.REPO_ID_COLUMN, REPO_ID,
                AoNotificationConfiguration.CHANNEL_ID_COLUMN, CHANNEL_ID
        ));
        entityManager.create(AoNotificationConfiguration.class, ImmutableMap.of(
                AoNotificationConfiguration.TEAM_ID_COLUMN, TEAM_ID,
                AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, "someNotificationType2",
                AoNotificationConfiguration.REPO_ID_COLUMN, REPO_ID,
                AoNotificationConfiguration.CHANNEL_ID_COLUMN, CHANNEL_ID
        ));

        Set<ChannelToNotify> channels = target.getChannelsToNotify(new NotificationSearchRequest.Builder()
                .repository(repository)
                .notificationType("someNotificationType1")
                .build());

        assertThat(channels.size(), is(1));
        assertThat(channels.iterator().next(), is(new ChannelToNotify(TEAM_ID, CHANNEL_ID, null, false)));
    }

    @Test
    public void search() throws Exception {
        when(repository.getId()).thenReturn(REPO_ID);
        entityManager.create(AoNotificationConfiguration.class, ImmutableMap.of(
                AoNotificationConfiguration.TEAM_ID_COLUMN, TEAM_ID,
                AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, "someNotificationType1",
                AoNotificationConfiguration.REPO_ID_COLUMN, REPO_ID,
                AoNotificationConfiguration.CHANNEL_ID_COLUMN, CHANNEL_ID
        ));
        entityManager.create(AoNotificationConfiguration.class, ImmutableMap.of(
                AoNotificationConfiguration.TEAM_ID_COLUMN, TEAM_ID,
                AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, "someNotificationType2",
                AoNotificationConfiguration.REPO_ID_COLUMN, REPO_ID,
                AoNotificationConfiguration.CHANNEL_ID_COLUMN, CHANNEL_ID
        ));
        entityManager.create(AoNotificationConfiguration.class, ImmutableMap.of(
                AoNotificationConfiguration.TEAM_ID_COLUMN, TEAM_ID,
                AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, "someNotificationType1",
                AoNotificationConfiguration.REPO_ID_COLUMN, REPO_ID + 2,
                AoNotificationConfiguration.CHANNEL_ID_COLUMN, CHANNEL_ID + "2"
        ));
        entityManager.create(AoNotificationConfiguration.class, ImmutableMap.of(
                AoNotificationConfiguration.TEAM_ID_COLUMN, TEAM_ID,
                AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, "someNotificationType1",
                AoNotificationConfiguration.REPO_ID_COLUMN, REPO_ID + 3,
                AoNotificationConfiguration.CHANNEL_ID_COLUMN, CHANNEL_ID
        ));
        when(conversationLoaderHelper.conversationsAndLinksById(any(), any(), any())).thenReturn(conversationsAndLinks);
        when(conversationsAndLinks.conversation(new ConversationKey(TEAM_ID, CHANNEL_ID))).thenReturn(Optional.of(conversation));
        when(conversation.getName()).thenReturn("someConversationName");
        when(conversationsAndLinks.link(TEAM_ID)).thenReturn(Optional.of(slackLink));
        when(repositoryService.getById(REPO_ID)).thenReturn(repository);
        when(repositoryService.getById(REPO_ID + 2)).thenReturn(repository2);
        when(repository.getProject()).thenReturn(project);
        when(repository.getName()).thenReturn("repo1");
        when(repository2.getProject()).thenReturn(project);
        when(repository2.getName()).thenReturn("repo2");
        when(project.getName()).thenReturn("someProjectName");
        when(bitbucketSlackSettingsService.getVerbosity(anyInt(), eq(TEAM_ID), any())).thenReturn(Verbosity.EXTENDED);

        List<RepositoryConfiguration> results = newArrayList(target.search(new NotificationSearchRequest.Builder()
                        .build(), new PageRequestImpl(0, 5)).getValues());

        assertThat(results.size(), is(2));
        assertThat(results.get(0).getRepository(), is(repository));
        List<ChannelConfiguration> channelConfigs = newArrayList(results.get(0).getChannelConfigurations());
        assertThat(channelConfigs.get(0).getNotificationConfigurationKeys(),
                containsInAnyOrder("someNotificationType1", "someNotificationType2"));
        assertThat(channelConfigs.get(0).getChannelDetails(), is(DefaultChannelDetails.builder()
                .teamId(TEAM_ID).teamName("team:" + TEAM_ID).channelId(CHANNEL_ID).channelName("someConversationName")
                .verbosity("EXTENDED").build()));

        assertThat(results.get(1).getRepository(), is(repository2));
        channelConfigs = newArrayList(results.get(1).getChannelConfigurations());
        assertThat(channelConfigs.get(0).getNotificationConfigurationKeys(),
                contains("someNotificationType1"));
        assertThat(channelConfigs.get(0).getChannelDetails(), is(DefaultChannelDetails.builder()
                .teamId(TEAM_ID).teamName("team:" + TEAM_ID).channelId(CHANNEL_ID + "2").channelName("private:" + CHANNEL_ID + "2")
                .verbosity("EXTENDED").build()));

        assertThat(entityManager.find(AoNotificationConfiguration.class, Query.select().where("REPO_ID = ?", REPO_ID + 3)).length,
                is(0));
    }
}
