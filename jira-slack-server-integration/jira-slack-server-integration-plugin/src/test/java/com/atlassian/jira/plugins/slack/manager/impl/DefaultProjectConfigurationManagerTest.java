package com.atlassian.jira.plugins.slack.manager.impl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.slack.dao.ConfigurationDAO;
import com.atlassian.jira.plugins.slack.manager.PluginConfigurationManager;
import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.atlassian.jira.plugins.slack.model.ProjectConfiguration;
import com.atlassian.jira.plugins.slack.model.analytics.IssuePanelHiddenEvent;
import com.atlassian.jira.plugins.slack.model.analytics.ProjectChannelUnlinkedEvent;
import com.atlassian.jira.plugins.slack.model.dto.ProjectConfigurationDTO;
import com.atlassian.jira.plugins.slack.model.dto.ProjectToChannelConfigurationDTO;
import com.atlassian.jira.plugins.slack.model.event.AutoConvertEvent;
import com.atlassian.jira.plugins.slack.model.event.ConfigurationEvent;
import com.atlassian.jira.plugins.slack.model.event.ProjectMappingConfigurationEvent;
import com.atlassian.jira.plugins.slack.service.notification.NotificationInfo;
import com.atlassian.jira.plugins.slack.service.task.TaskBuilder;
import com.atlassian.jira.plugins.slack.service.task.TaskExecutorService;
import com.atlassian.jira.plugins.slack.service.task.impl.SendNotificationTask;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.ConversationLoaderHelper;
import com.atlassian.plugins.slack.api.client.ConversationsAndLinks;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.util.ErrorResponse;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.github.seratch.jslack.api.model.Conversation;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Either;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.atlassian.jira.plugins.slack.manager.impl.DefaultProjectConfigurationManager.CONFIGURATION_OWNER;
import static com.atlassian.jira.plugins.slack.manager.impl.DefaultProjectConfigurationManager.ISSUE_PANEL_HIDDEN;
import static com.atlassian.jira.plugins.slack.manager.impl.DefaultProjectConfigurationManager.IS_MUTED;
import static com.atlassian.jira.plugins.slack.manager.impl.DefaultProjectConfigurationManager.PROJECT_AUTOCONVERT_ENABLED;
import static com.atlassian.jira.plugins.slack.manager.impl.DefaultProjectConfigurationManager.VERBOSITY;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultProjectConfigurationManagerTest {
    @Mock
    private ConfigurationDAO configurationDAO;
    @Mock
    private TaskExecutorService taskExecutorService;
    @Mock
    private TaskBuilder taskBuilder;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private ProjectManager projectManager;
    @Mock
    private PluginConfigurationManager pluginConfigurationManager;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private ConversationLoaderHelper conversationLoaderHelper;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;

    @Mock
    private ProjectConfiguration projectConfiguration;
    @Mock
    private ProjectConfiguration projectConfiguration2;
    @Mock
    private ProjectConfiguration projectConfigurationDifferentProject;
    @Mock
    private ProjectConfiguration projectConfigurationMissingProject;
    @Mock
    private ProjectConfiguration projectConfigurationMissingChannel;
    @Mock
    private ProjectConfiguration projectConfigurationMissingLink;
    @Mock
    private ProjectConfiguration projectConfigurationOwner;
    @Mock
    private ProjectConfiguration projectConfigurationVerbosity;
    @Mock
    private SlackLink slackLink;
    @Mock
    private Conversation conversation;
    @Mock
    private Conversation conversation2;
    @Mock
    private SlackClient client;
    @Mock
    private SlackClient clientOwner;
    @Mock
    private SlackClient clientCurrent;
    @Mock
    private Project project;
    @Mock
    private Project project2;
    @Mock
    private ProjectConfigurationDTO projectConfigurationDTO;
    @Mock
    private SendNotificationTask sendNotificationTask;
    @Mock
    private PluginSettings pluginSettings;
    @Mock
    private ApplicationUser user;

    @Captor
    private ArgumentCaptor<Function<ProjectConfiguration, ConversationKey>> entityCaptor;
    @Captor
    private ArgumentCaptor<BiFunction<SlackClient, ConversationKey, Optional<Conversation>>> loaderCaptor;
    @Captor
    private ArgumentCaptor<ProjectMappingConfigurationEvent> eventCaptor;
    @Captor
    private ArgumentCaptor<Object> genericCaptor;
    @Captor
    private ArgumentCaptor<NotificationInfo> notificationInfoCaptor;
    @Captor
    private ArgumentCaptor<ProjectConfiguration> projectConfigurationArgumentCaptor;
    @Captor
    private ArgumentCaptor<AutoConvertEvent> autoConvertEventArgumentCaptor;
    @Captor
    private ArgumentCaptor<IssuePanelHiddenEvent> issuePanelHiddenEventCaptor;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultProjectConfigurationManager target;

    @Test
    public void isAnyProjectConfigured() {
        when(configurationDAO.isAnyProjectConfigured()).thenReturn(true);

        boolean result = target.isAnyProjectConfigured();

        assertThat(result, is(true));
    }

    @Test
    public void getConfiguration() {
        List<ProjectConfiguration> configs = Arrays.asList(
                projectConfiguration,
                projectConfiguration2,
                projectConfigurationMissingChannel,
                projectConfigurationMissingLink);
        List<ProjectConfiguration> configsWithoutName = Arrays.asList(
                projectConfiguration2,
                projectConfigurationMissingChannel,
                projectConfigurationMissingLink);

        ConversationsAndLinks conversationsAndLinks = new ConversationsAndLinks(
                ImmutableMap.of(new ConversationKey("T", "C"), conversation,
                        new ConversationKey("T", "C2"), conversation2),
                ImmutableMap.of("T", slackLink),
                ImmutableMap.of(new ConversationKey("T", "C"), slackLink,
                        new ConversationKey("T", "C2"), slackLink));

        when(projectConfiguration.getTeamId()).thenReturn("T");
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getName()).thenReturn("PCN");
        when(projectConfiguration.getValue()).thenReturn("PCV");
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");

        when(projectConfiguration2.getTeamId()).thenReturn("T");
        when(projectConfiguration2.getChannelId()).thenReturn("C");
        when(projectConfiguration2.getProjectId()).thenReturn(7L);
        when(projectConfiguration2.getConfigurationGroupId()).thenReturn("G");

        when(projectConfigurationMissingChannel.getTeamId()).thenReturn("T");
        when(projectConfigurationMissingChannel.getProjectId()).thenReturn(7L);
        when(projectConfigurationMissingChannel.getChannelId()).thenReturn("NOTC");
        when(projectConfigurationMissingChannel.getConfigurationGroupId()).thenReturn("G3");

        when(projectConfigurationMissingLink.getTeamId()).thenReturn("NOTT");
        when(projectConfigurationMissingLink.getProjectId()).thenReturn(7L);

        when(configurationDAO.findByProjectId(7L)).thenReturn(configs);
        when(conversationLoaderHelper.conversationsAndLinksById(eq(configsWithoutName), entityCaptor.capture(), loaderCaptor.capture()))
                .thenReturn(conversationsAndLinks);
        when(projectManager.getProjectObj(7L)).thenReturn(project);
        when(conversation.getName()).thenReturn("CNB");
        when(slackLink.getTeamName()).thenReturn("TN");
        when(project.getId()).thenReturn(7L);
        when(project.getKey()).thenReturn("PK");
        when(project.getName()).thenReturn("PN");

        ProjectToChannelConfigurationDTO result = target.getConfiguration(7L);

        assertThat(result.getProjectId(), is(7L));
        assertThat(result.getProjectName(), is("PN"));
        assertThat(result.getProjectKey(), is("PK"));
        assertThat(result.getOrderedChannelIds(), contains(new ConversationKey("T", "C"), new ConversationKey("T", "NOTC")));
        assertThat(result.getChannels().keySet(), containsInAnyOrder("T:C", "T:NOTC"));
        assertThat(result.getChannels().get("T:C").getChannelId(), is("C"));
        assertThat(result.getChannels().get("T:C").getTeamId(), is("T"));
        assertThat(result.getChannels().get("T:C").getChannelName(), is("CNB"));
        assertThat(result.getChannels().get("T:C").getTeamName(), is("TN"));
        assertThat(result.getChannels().get("T:NOTC").getChannelId(), is("NOTC"));
        assertThat(result.getChannels().get("T:NOTC").getTeamId(), is("T"));
        assertThat(result.getChannels().get("T:NOTC").getChannelName(), is("id:NOTC"));
        assertThat(result.getChannels().get("T:NOTC").getTeamName(), is("TN"));
        assertThat(result.getConfiguration().keySet(), containsInAnyOrder("T:C", "T:NOTC"));
        assertThat(result.getConfiguration().get("T:C"), hasSize(1));
        assertThat(result.getConfiguration().get("T:C").get(0).getConfigurationGroupId(), is("G"));
        assertThat(result.getConfiguration().get("T:C").get(0).getSettings().keySet(), containsInAnyOrder("PCN"));
        assertThat(result.getConfiguration().get("T:C").get(0).getSettings().get("PCN").getValue(), is("PCV"));

        final ConversationKey convKey = entityCaptor.getValue().apply(projectConfiguration);
        assertThat(convKey.getTeamId(), is("T"));
        assertThat(convKey.getChannelId(), is("C"));

        // assert loader works as expected
        when(client.getConversationsInfo("C")).thenReturn(Either.right(conversation));
        when(client.withRemoteUserTokenIfAvailable()).thenReturn(Optional.of(client));

        final ConversationKey conversationKey = new ConversationKey("T", "C");
        Optional<Conversation> captor2Apply = loaderCaptor.getValue().apply(client, conversationKey);
        MatcherAssert.assertThat(captor2Apply.isPresent(), is(true));
        MatcherAssert.assertThat(captor2Apply.get(), sameInstance(conversation));
    }

    @Test
    public void getConfigurations() {
        List<ProjectConfiguration> configs = Arrays.asList(
                projectConfiguration,
                projectConfiguration2,
                projectConfigurationDifferentProject,
                projectConfigurationMissingProject,
                projectConfigurationMissingChannel,
                projectConfigurationMissingLink);
        List<ProjectConfiguration> configsWithoutName = Arrays.asList(
                projectConfiguration2,
                projectConfigurationDifferentProject,
                projectConfigurationMissingProject,
                projectConfigurationMissingChannel,
                projectConfigurationMissingLink);

        ConversationsAndLinks conversationsAndLinks = new ConversationsAndLinks(
                ImmutableMap.of(new ConversationKey("T", "C"), conversation, new ConversationKey("T",
                        "C2"), conversation2),
                ImmutableMap.of("T", slackLink),
                ImmutableMap.of(new ConversationKey("T", "C"), slackLink, new ConversationKey("T",
                        "C2"), slackLink));

        when(projectConfiguration.getTeamId()).thenReturn("T");
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getName()).thenReturn("PCN");
        when(projectConfiguration.getValue()).thenReturn("PCV");
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");

        when(projectConfiguration2.getTeamId()).thenReturn("T");
        when(projectConfiguration2.getChannelId()).thenReturn("C");
        when(projectConfiguration2.getProjectId()).thenReturn(7L);
        when(projectConfiguration2.getConfigurationGroupId()).thenReturn("G");

        when(projectConfigurationDifferentProject.getTeamId()).thenReturn("T");
        when(projectConfigurationDifferentProject.getChannelId()).thenReturn("C2");
        when(projectConfigurationDifferentProject.getProjectId()).thenReturn(6L);
        when(projectConfigurationDifferentProject.getConfigurationGroupId()).thenReturn("G2");

        when(projectConfigurationMissingProject.getProjectId()).thenReturn(8L);

        when(projectConfigurationMissingChannel.getTeamId()).thenReturn("T");
        when(projectConfigurationMissingChannel.getProjectId()).thenReturn(7L);
        when(projectConfigurationMissingChannel.getChannelId()).thenReturn("NOTC");
        when(projectConfigurationMissingChannel.getConfigurationGroupId()).thenReturn("G3");

        when(projectConfigurationMissingLink.getTeamId()).thenReturn("NOTT");
        when(projectConfigurationMissingLink.getProjectId()).thenReturn(7L);

        when(configurationDAO.getProjectConfigurations(1, 2)).thenReturn(configs);
        when(conversationLoaderHelper.conversationsAndLinksById(eq(configsWithoutName), entityCaptor.capture(), loaderCaptor.capture()))
                .thenReturn(conversationsAndLinks);
        when(projectManager.getProjectObj(7L)).thenReturn(project);
        when(projectManager.getProjectObj(6L)).thenReturn(project2);
        when(projectManager.getProjectObj(8L)).thenReturn(null);
        when(conversation.getName()).thenReturn("CNB");
        when(conversation2.getName()).thenReturn("CNA");
        when(slackLink.getTeamName()).thenReturn("TN");
        when(project.getId()).thenReturn(7L);
        when(project.getKey()).thenReturn("PK");
        when(project.getName()).thenReturn("PN");
        when(project2.getId()).thenReturn(6L);
        when(project2.getKey()).thenReturn("PK2");
        when(project2.getName()).thenReturn("PN2");

        List<ProjectToChannelConfigurationDTO> result = target.getConfigurations(1, 2);

        assertThat(result, hasSize(2));

        final ProjectToChannelConfigurationDTO projConfigDTO = result.get(0);
        assertThat(projConfigDTO.getProjectId(), is(7L));
        assertThat(projConfigDTO.getProjectName(), is("PN"));
        assertThat(projConfigDTO.getProjectKey(), is("PK"));
        assertThat(projConfigDTO.getOrderedChannelIds(), contains(new ConversationKey("T", "C"), new ConversationKey("T", "NOTC")));
        assertThat(projConfigDTO.getChannels().keySet(), containsInAnyOrder("T:C", "T:NOTC"));
        assertThat(projConfigDTO.getChannels().get("T:C").getChannelId(), is("C"));
        assertThat(projConfigDTO.getChannels().get("T:C").getTeamId(), is("T"));
        assertThat(projConfigDTO.getChannels().get("T:C").getChannelName(), is("CNB"));
        assertThat(projConfigDTO.getChannels().get("T:C").getTeamName(), is("TN"));
        assertThat(projConfigDTO.getChannels().get("T:NOTC").getChannelId(), is("NOTC"));
        assertThat(projConfigDTO.getChannels().get("T:NOTC").getTeamId(), is("T"));
        assertThat(projConfigDTO.getChannels().get("T:NOTC").getChannelName(), is("id:NOTC"));
        assertThat(projConfigDTO.getChannels().get("T:NOTC").getTeamName(), is("TN"));
        assertThat(projConfigDTO.getConfiguration().keySet(), containsInAnyOrder("T:C", "T:NOTC"));
        assertThat(projConfigDTO.getConfiguration().get("T:C"), hasSize(1));
        assertThat(projConfigDTO.getConfiguration().get("T:C").get(0).getConfigurationGroupId(), is("G"));
        assertThat(projConfigDTO.getConfiguration().get("T:C").get(0).getSettings().keySet(), containsInAnyOrder("PCN"));
        assertThat(projConfigDTO.getConfiguration().get("T:C").get(0).getSettings().get("PCN").getValue(), is("PCV"));

        final ProjectToChannelConfigurationDTO projConfigDTO2 = result.get(1);
        assertThat(projConfigDTO2.getProjectId(), is(6L));
        assertThat(projConfigDTO2.getProjectName(), is("PN2"));
        assertThat(projConfigDTO2.getProjectKey(), is("PK2"));
        assertThat(projConfigDTO2.getOrderedChannelIds(), contains(new ConversationKey("T", "C2")));
        assertThat(projConfigDTO2.getChannels().size(), is(1));
        assertThat(projConfigDTO2.getChannels().keySet(), contains("T:C2"));
        assertThat(projConfigDTO2.getChannels().get("T:C2").getChannelId(), is("C2"));
        assertThat(projConfigDTO2.getChannels().get("T:C2").getTeamId(), is("T"));
        assertThat(projConfigDTO2.getChannels().get("T:C2").getChannelName(), is("CNA"));
        assertThat(projConfigDTO2.getChannels().get("T:C2").getTeamName(), is("TN"));

        final ConversationKey convKey = entityCaptor.getValue().apply(projectConfiguration);
        assertThat(convKey.getTeamId(), is("T"));
        assertThat(convKey.getChannelId(), is("C"));

        // assert loader works as expected
        when(client.getConversationsInfo("C")).thenReturn(Either.right(conversation));
        when(client.withRemoteUserTokenIfAvailable()).thenReturn(Optional.of(client));

        final ConversationKey conversationKey = new ConversationKey("T", "C");
        Optional<Conversation> captor2Apply = loaderCaptor.getValue().apply(client, conversationKey);
        MatcherAssert.assertThat(captor2Apply.isPresent(), is(true));
        MatcherAssert.assertThat(captor2Apply.get(), sameInstance(conversation));
    }

    @Test
    public void getMutedProjectConfigurations() {
        when(configurationDAO.findMuted()).thenReturn(Collections.singletonList(projectConfiguration));

        List<ProjectConfiguration> result = target.getMutedProjectConfigurations();

        assertThat(result, contains(projectConfiguration));
    }

    @Test
    public void getAllProjectsByChannel() {
        when(configurationDAO.findByChannel("C")).thenReturn(Collections.singletonList(projectConfiguration));
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectManager.getProjectObj(7L)).thenReturn(project);

        Set<Project> result = target.getAllProjectsByChannel("C");

        assertThat(result, contains(project));
    }

    @Test
    public void getProjectsByTeamId() {
        when(configurationDAO.findByTeam("T")).thenReturn(Collections.singletonList(projectConfiguration));
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectManager.getProjectObj(7L)).thenReturn(project);

        Set<Project> result = target.getProjectsByTeamId("T");

        assertThat(result, contains(project));
    }

    @Test
    public void deleteProjectConfiguration_shouldNotPublishEventIfConfigurationRemains() {
        when(projectConfigurationDTO.getProjectId()).thenReturn(7L);
        when(projectConfigurationDTO.getChannelId()).thenReturn("C");
        when(configurationDAO.findByProjectId(7L)).thenReturn(
                Arrays.asList(projectConfiguration, projectConfiguration2),
                Collections.singletonList(projectConfiguration2));
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getName()).thenReturn(EventMatcherType.ISSUE_CREATED.getDbKey());
        when(projectConfiguration2.getChannelId()).thenReturn("C");
        when(projectConfiguration2.getName()).thenReturn(EventMatcherType.ISSUE_CREATED.getDbKey());

        target.deleteProjectConfiguration(projectConfigurationDTO, user);

        verify(configurationDAO).deleteProjectConfiguration(projectConfigurationDTO);
        verify(slackLinkManager, never()).getLinkByTeamId(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    public void deleteProjectConfiguration_shouldPublishEvent() {
        when(projectConfigurationDTO.getProjectId()).thenReturn(7L);
        when(projectConfigurationDTO.getProjectKey()).thenReturn("P");
        when(projectConfigurationDTO.getProjectName()).thenReturn("PN");
        when(projectConfigurationDTO.getChannelId()).thenReturn("C");
        when(projectConfigurationDTO.getTeamId()).thenReturn("T");
        when(projectConfigurationDTO.getConfigurationGroupId()).thenReturn("G");
        initializeDeleteMocks();

        target.deleteProjectConfiguration(projectConfigurationDTO, user);

        verify(configurationDAO).deleteProjectConfiguration(projectConfigurationDTO);
        assertDeletetion(1);
    }

    @Test
    public void deleteProjectConfigurationGroup() {
        when(projectConfigurationDTO.getProjectId()).thenReturn(7L);
        when(projectConfigurationDTO.getProjectKey()).thenReturn("P");
        when(projectConfigurationDTO.getProjectName()).thenReturn("PN");
        when(projectConfigurationDTO.getChannelId()).thenReturn("C");
        when(projectConfigurationDTO.getTeamId()).thenReturn("T");
        when(projectConfigurationDTO.getConfigurationGroupId()).thenReturn("G");
        initializeDeleteMocks();

        target.deleteProjectConfigurationGroup(projectConfigurationDTO, user);

        verify(configurationDAO).deleteProjectConfigurationGroup(projectConfigurationDTO);
        assertDeletetion(1);
    }

    @Test
    public void deleteProjectConfigurationsByChannelId() {
        initializeDeleteMocks();
        when(configurationDAO.findByChannel("C")).thenReturn(Collections.singletonList(projectConfiguration));
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(projectConfiguration.getTeamId()).thenReturn("T");
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getProjectId()).thenReturn(7L);

        target.deleteProjectConfigurationsByChannelId("C");

        assertDeletetion(1);
    }

    private void initializeDeleteMocks() {
        when(configurationDAO.findByProjectId(7L)).thenReturn(
                Arrays.asList(projectConfiguration, projectConfiguration2),
                Collections.singletonList(projectConfiguration2));
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getName()).thenReturn(EventMatcherType.ISSUE_CREATED.getDbKey());
        when(projectConfiguration2.getName()).thenReturn("");
        when(slackLinkManager.getLinkByTeamId("T")).thenReturn(Either.right(slackLink));
        when(taskBuilder.newSendNotificationTask(
                eventCaptor.capture(),
                notificationInfoCaptor.capture(),
                same(taskExecutorService))
        ).thenReturn(sendNotificationTask);
        when(configurationDAO.findByProjectConfigurationGroupId(7L, "G"))
                .thenReturn(Collections.singletonList(projectConfigurationOwner));
        when(projectConfigurationOwner.getName()).thenReturn(CONFIGURATION_OWNER);
        when(projectConfigurationOwner.getValue()).thenReturn("O");
    }

    private void assertDeletetion(int eventCount) {
        verify(taskExecutorService).submitTask(sendNotificationTask);
        verify(eventPublisher, times(eventCount)).publish(genericCaptor.capture());
        assertThat(genericCaptor.getAllValues().get(0), instanceOf(ProjectChannelUnlinkedEvent.class));

        ProjectMappingConfigurationEvent event = eventCaptor.getValue();
        assertThat(event.getEventType(), is(ConfigurationEvent.ConfigurationEventType.CHANNEL_UNLINKED));
        assertThat(event.getChannelId(), is("C"));
        assertThat(event.getTeamId(), is("T"));

        NotificationInfo info = notificationInfoCaptor.getValue();
        assertThat(info.getLink(), sameInstance(slackLink));
        assertThat(info.getChannelId(), is("C"));
        assertThat(info.getConfigurationOwner(), is("O"));
    }

    @Test
    public void updatedOwnerIfNeeded_doNothingWithoutTeam() {
        when(projectConfiguration.getTeamId()).thenReturn("T");
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.left(new Exception()));

        target.updatedOwnerIfNeeded(projectConfiguration, "UK");

        verify(configurationDAO, never()).findByProjectConfigurationGroupId(anyLong(), any());
        verify(configurationDAO, never()).updateProjectConfiguration(any());
        verify(configurationDAO, never()).insertProjectConfiguration(any());
    }

    @Test
    public void updatedOwnerIfNeeded_doNothingIfOwnerIsValid() {
        when(projectConfiguration.getTeamId()).thenReturn("T");
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(client));
        when(configurationDAO.findByProjectConfigurationGroupId(7L, "G"))
                .thenReturn(Collections.singletonList(projectConfigurationOwner));
        when(projectConfigurationOwner.getName()).thenReturn(CONFIGURATION_OWNER);
        when(projectConfigurationOwner.getValue()).thenReturn("O");

        when(client.withUserTokenIfAvailable("O")).thenReturn(Optional.of(client));
        when(client.getConversationsInfo("C")).thenReturn(Either.right(conversation));

        target.updatedOwnerIfNeeded(projectConfiguration, "UK");

        verify(configurationDAO, never()).updateProjectConfiguration(any());
        verify(configurationDAO, never()).insertProjectConfiguration(any());
    }

    @Test
    public void updatedOwnerIfNeeded_shouldUpdateIfOwnerCannotAccessChannel() {
        when(projectConfiguration.getTeamId()).thenReturn("T");
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(client));
        when(configurationDAO.findByProjectConfigurationGroupId(7L, "G"))
                .thenReturn(Collections.singletonList(projectConfigurationOwner));
        when(projectConfigurationOwner.getName()).thenReturn(CONFIGURATION_OWNER);
        when(projectConfigurationOwner.getValue()).thenReturn("O");

        when(client.withUserTokenIfAvailable("O")).thenReturn(Optional.of(clientOwner));
        when(client.withUserTokenIfAvailable("UK")).thenReturn(Optional.of(clientCurrent));
        when(clientOwner.getConversationsInfo("C")).thenReturn(Either.left(new ErrorResponse(new Exception())));
        when(clientCurrent.getConversationsInfo("C")).thenReturn(Either.right(conversation));

        target.updatedOwnerIfNeeded(projectConfiguration, "UK");

        verify(configurationDAO).updateProjectConfiguration(projectConfigurationArgumentCaptor.capture());

        ProjectConfiguration config = projectConfigurationArgumentCaptor.getValue();
        assertThat(config.getName(), is(CONFIGURATION_OWNER));
        assertThat(config.getValue(), is("UK"));
    }

    @Test
    public void updatedOwnerIfNeeded_shouldInsertIfOwnerDoesNotExist() {
        when(projectConfiguration.getTeamId()).thenReturn("T");
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(client));
        when(configurationDAO.findByProjectConfigurationGroupId(7L, "G")).thenReturn(Collections.emptyList());

        when(client.withUserTokenIfAvailable("UK")).thenReturn(Optional.of(clientCurrent));
        when(clientCurrent.getConversationsInfo("C")).thenReturn(Either.right(conversation));

        target.updatedOwnerIfNeeded(projectConfiguration, "UK");

        verify(configurationDAO).insertProjectConfiguration(projectConfigurationArgumentCaptor.capture());

        ProjectConfiguration config = projectConfigurationArgumentCaptor.getValue();
        assertThat(config.getName(), is(CONFIGURATION_OWNER));
        assertThat(config.getValue(), is("UK"));
    }

    @Test
    public void updatedOwnerIfNeeded_shouldDoNothingIfUserHasNotAccess() {
        when(projectConfiguration.getTeamId()).thenReturn("T");
        when(projectConfiguration.getChannelId()).thenReturn("C");
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(client));
        when(configurationDAO.findByProjectConfigurationGroupId(7L, "G")).thenReturn(Collections.emptyList());

        when(client.withUserTokenIfAvailable("UK")).thenReturn(Optional.of(clientCurrent));
        when(clientCurrent.getConversationsInfo("C")).thenReturn(Either.left(new ErrorResponse(new Exception())));

        target.updatedOwnerIfNeeded(projectConfiguration, "UK");

        verify(configurationDAO, never()).insertProjectConfiguration(any());
        verify(configurationDAO, never()).updateProjectConfiguration(any());
    }

    @Test
    public void getOwner() {
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(configurationDAO.findByProjectConfigurationGroupId(7L, "G"))
                .thenReturn(Collections.singletonList(projectConfigurationOwner));
        when(projectConfigurationOwner.getName()).thenReturn(CONFIGURATION_OWNER);
        when(projectConfigurationOwner.getValue()).thenReturn("O");

        Optional<String> result = target.getOwner(projectConfiguration);

        assertThat(result, is(Optional.of("O")));
    }

    @Test
    public void getOwner_emptyIfNotOwner() {
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(configurationDAO.findByProjectConfigurationGroupId(7L, "G"))
                .thenReturn(Collections.singletonList(projectConfigurationOwner));
        when(projectConfigurationOwner.getName()).thenReturn("N");

        Optional<String> result = target.getOwner(projectConfiguration);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void getVerbosity() {
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(configurationDAO.findByProjectConfigurationGroupId(7L, "G"))
                .thenReturn(Collections.singletonList(projectConfigurationVerbosity));
        when(projectConfigurationVerbosity.getName()).thenReturn(VERBOSITY);
        when(projectConfigurationVerbosity.getValue()).thenReturn("EXTENDED");

        Optional<Verbosity> result = target.getVerbosity(projectConfiguration);

        assertThat(result, is(Optional.of(Verbosity.EXTENDED)));
    }

    @Test
    public void getVerbosity_emptyIfNotOwner() {
        when(projectConfiguration.getProjectId()).thenReturn(7L);
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(configurationDAO.findByProjectConfigurationGroupId(7L, "G"))
                .thenReturn(Collections.singletonList(projectConfigurationVerbosity));
        when(projectConfigurationVerbosity.getName()).thenReturn("N");

        Optional<Verbosity> result = target.getVerbosity(projectConfiguration);

        assertThat(result.isPresent(), is(false));
    }

    @Test
    public void insertProjectConfiguration() {
        when(configurationDAO.findByProjectId(7L)).thenReturn(
                Collections.emptyList(),
                Collections.singletonList(projectConfiguration));
        when(projectConfigurationDTO.getTeamId()).thenReturn("T");
        when(projectConfigurationDTO.getChannelId()).thenReturn("C");
        when(projectConfigurationDTO.getProjectId()).thenReturn(7L);
        when(projectConfigurationDTO.getConfigurationGroupId()).thenReturn("G");
        when(projectConfigurationDTO.getName()).thenReturn(null);
        when(slackClientProvider.withTeamId("T")).thenReturn(Either.right(client));
        when(user.getKey()).thenReturn("U");
        when(client.withUserToken("U")).thenReturn(Either.right(client));
        when(client.selfInviteToConversation("C")).thenReturn(Either.right(conversation));

        ProjectConfigurationDTO result = target.insertProjectConfiguration(projectConfigurationDTO, user);

        assertThat(result.getTeamId(), is("T"));
        assertThat(result.getChannelId(), is("C"));
        assertThat(result.getConfigurationGroupId(), is("G"));

        verify(configurationDAO).insertProjectConfiguration(projectConfigurationDTO);
        verify(client).withUserToken("U");
        verify(client).selfInviteToConversation("C");
    }

    @Test
    public void updateProjectConfiguration() {
        target.updateProjectConfiguration(projectConfigurationDTO);
        verify(configurationDAO).updateProjectConfiguration(projectConfigurationDTO);
    }

    @Test
    public void isProjectAutoConvertEnabled() {
        when(project.getId()).thenReturn(7L);
        when(pluginConfigurationManager.getSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(PROJECT_AUTOCONVERT_ENABLED + "7")).thenReturn("true");

        assertThat(target.isProjectAutoConvertEnabled(project), is(true));
    }

    @Test
    public void isProjectAutoConvertEnabled_globalTrue() {
        when(project.getId()).thenReturn(7L);
        when(pluginConfigurationManager.getSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(PROJECT_AUTOCONVERT_ENABLED + "7")).thenReturn(null);
        when(pluginConfigurationManager.isGlobalAutoConvertEnabled()).thenReturn(true);

        assertThat(target.isProjectAutoConvertEnabled(project), is(true));
    }

    @Test
    public void isProjectAutoConvertEnabled_disabled() {
        when(project.getId()).thenReturn(7L);
        when(pluginConfigurationManager.getSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(PROJECT_AUTOCONVERT_ENABLED + "7")).thenReturn(null);
        when(pluginConfigurationManager.isGlobalAutoConvertEnabled()).thenReturn(false);

        assertThat(target.isProjectAutoConvertEnabled(project), is(false));
    }

    @Test
    public void setProjectAutoConvertEnabled() {
        when(pluginConfigurationManager.getSettings()).thenReturn(pluginSettings);
        when(project.getId()).thenReturn(7L);

        target.setProjectAutoConvertEnabled(project, true);

        verify(pluginSettings).put(PROJECT_AUTOCONVERT_ENABLED + "7", "true");
        verify(eventPublisher).publish(autoConvertEventArgumentCaptor.capture());
        assertThat(autoConvertEventArgumentCaptor.getValue().getAnalyticEventName(), is("notifications.slack.autoconvert.project.enabled"));
    }

    @Test
    public void setProjectAutoConvertEnabled_Disabled() {
        when(pluginConfigurationManager.getSettings()).thenReturn(pluginSettings);
        when(project.getId()).thenReturn(7L);

        target.setProjectAutoConvertEnabled(project, false);

        verify(pluginSettings).put(PROJECT_AUTOCONVERT_ENABLED + "7", "false");
        verify(eventPublisher).publish(autoConvertEventArgumentCaptor.capture());
        assertThat(autoConvertEventArgumentCaptor.getValue().getAnalyticEventName(), is("notifications.slack.autoconvert.project.disabled"));
    }

    @Test
    public void muteProjectConfigurationsByChannelId() {
        when(configurationDAO.findByChannel("C"))
                .thenReturn(Arrays.asList(projectConfiguration, projectConfiguration2));
        when(projectConfiguration.getName()).thenReturn(IS_MUTED);
        when(projectConfiguration.getConfigurationGroupId()).thenReturn("G");
        when(projectConfiguration2.getName()).thenReturn(null);
        when(projectConfiguration2.getTeamId()).thenReturn("T");
        when(projectConfiguration2.getChannelId()).thenReturn("C");
        when(projectConfiguration2.getProjectId()).thenReturn(7L);
        when(projectConfiguration2.getConfigurationGroupId()).thenReturn("G2");

        target.muteProjectConfigurationsByChannelId("C");

        verify(configurationDAO).insertProjectConfiguration(projectConfigurationArgumentCaptor.capture());
        ProjectConfiguration newConfig = projectConfigurationArgumentCaptor.getValue();
        assertThat(newConfig.getName(), is(IS_MUTED));
        assertThat(newConfig.getValue(), is("true"));
        assertThat(newConfig.getTeamId(), is("T"));
        assertThat(newConfig.getProjectId(), is(7L));
        assertThat(newConfig.getConfigurationGroupId(), is("G2"));
        assertThat(newConfig.getChannelId(), is("C"));
    }

    @Test
    public void unmuteProjectConfigurationsByChannelId() {
        when(configurationDAO.findByChannel("C"))
                .thenReturn(Arrays.asList(projectConfiguration, projectConfiguration2));
        when(projectConfiguration.getName()).thenReturn(IS_MUTED);
        when(projectConfiguration2.getName()).thenReturn("NOT_IS_MUTED");

        target.unmuteProjectConfigurationsByChannelId("C");

        verify(configurationDAO).deleteProjectConfiguration(projectConfiguration);
    }

    @Test
    public void isIssuePanelHidden_enabled() {
        when(project.getId()).thenReturn(7L);
        when(pluginConfigurationManager.getSettings()).thenReturn(pluginSettings);
        when(pluginSettings.get(ISSUE_PANEL_HIDDEN + "7")).thenReturn("true");

        assertThat(target.isIssuePanelHidden(project), is(true));
    }

    @Test
    public void setIssuePanelHidden() {
        when(pluginConfigurationManager.getSettings()).thenReturn(pluginSettings);
        when(project.getId()).thenReturn(7L);

        target.setIssuePanelHidden(project, true);

        verify(pluginSettings).put(ISSUE_PANEL_HIDDEN + "7", "true");
        verify(eventPublisher).publish(issuePanelHiddenEventCaptor.capture());
        assertThat(issuePanelHiddenEventCaptor.getValue().getProjectId(), is(7L));
    }
}
