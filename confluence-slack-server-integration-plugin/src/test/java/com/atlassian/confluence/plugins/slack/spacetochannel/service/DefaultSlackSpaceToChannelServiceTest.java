package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.plugins.slack.spacetochannel.ao.AOEntityToChannelMapping;
import com.atlassian.confluence.plugins.slack.spacetochannel.ao.EntityToChannelMappingManager;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SlackChannelDefinition;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelSettings;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.ConversationLoaderHelper;
import com.atlassian.plugins.slack.api.client.ConversationsAndLinks;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.github.seratch.jslack.api.model.Conversation;
import com.google.common.collect.ImmutableMap;
import io.atlassian.fugue.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultSlackSpaceToChannelServiceTest {
    private static final String TEAM_ID = "T";
    private static final String TEAM_NAME = "TN";
    private static final String CHANNEL_ID = "ID";
    private static final String CHANNEL_ID2 = "ID2";
    private static final String CHANNEL_NAME = "CNAME";
    private static final String USER = "USR";
    private static final String SPACE_KEY = "SPACE";
    private static final String NOTIF_TYPE_KEY = "NTK";
    private static final String NOTIF_TYPE_KEY2 = "NTK2";
    private static final UserKey userKey = new UserKey(USER);

    @Mock
    private SpaceManager spaceManager;
    @Mock
    private EntityToChannelMappingManager entityToChannelMappingManager;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private NotificationTypeService notificationTypeService;
    @Mock
    private ConversationLoaderHelper conversationLoaderHelper;
    @Mock(lenient = true)
    private SlackSettingService slackSettingService;
    @Mock
    private UserManager userManager;
    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private SlackLink slackLink;
    @Mock
    private SlackClient client;
    @Mock
    private Conversation conversation;
    @Mock
    private Space space;
    @Mock
    private NotificationType notificationType;
    @Mock
    private NotificationType notificationType2;
    @Mock
    private AOEntityToChannelMapping entity1;
    @Mock
    private AOEntityToChannelMapping entity2;

    @Captor
    private ArgumentCaptor<Function<AOEntityToChannelMapping, ConversationKey>> entityCaptor;
    @Captor
    private ArgumentCaptor<BiFunction<SlackClient, ConversationKey, Optional<Conversation>>> loaderCaptor;

    @InjectMocks
    private DefaultSlackSpaceToChannelService target;

    @Test
    public void getAllSpaceToChannelLinks_shouldReturnExpectedValue() {
        List<AOEntityToChannelMapping> mappings = Arrays.asList(entity1, entity2);
        ConversationsAndLinks conversationsAndLinks = new ConversationsAndLinks(
                ImmutableMap.of(new ConversationKey(TEAM_ID, CHANNEL_ID), conversation),
                ImmutableMap.of(TEAM_ID, slackLink),
                ImmutableMap.of(CHANNEL_ID, slackLink, CHANNEL_ID2, slackLink));

        when(slackLinkManager.isAnyLinkDefined()).thenReturn(true);
        when(entityToChannelMappingManager.getAll()).thenReturn(mappings);
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(conversationLoaderHelper.conversationsAndLinksById(same(mappings), entityCaptor.capture(), loaderCaptor.capture()))
                .thenReturn(conversationsAndLinks);
        when(slackSettingService.isChannelMuted(CHANNEL_ID)).thenReturn(true);
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_TYPE_KEY)).thenReturn(Optional.of(notificationType));
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_TYPE_KEY2)).thenReturn(Optional.of(notificationType2));
        when(transactionTemplate.execute(any())).thenAnswer(args -> ((TransactionCallback) args.getArgument(0)).doInTransaction());

        when(entity1.getMessageTypeKey()).thenReturn(NOTIF_TYPE_KEY);
        when(entity1.getEntityKey()).thenReturn(SPACE_KEY);
        when(entity1.getChannelId()).thenReturn(CHANNEL_ID);
        when(entity1.getTeamId()).thenReturn(TEAM_ID);
        when(entity2.getMessageTypeKey()).thenReturn(NOTIF_TYPE_KEY2);
        when(entity2.getEntityKey()).thenReturn(SPACE_KEY);
        when(entity2.getChannelId()).thenReturn(CHANNEL_ID2);
        when(slackLink.getTeamName()).thenReturn(TEAM_NAME);
        when(slackLink.getTeamId()).thenReturn(TEAM_ID);
        when(conversation.getName()).thenReturn(CHANNEL_NAME);
        when(conversation.getId()).thenReturn(CHANNEL_ID);
        when(conversation.isPrivate()).thenReturn(true);

        List<SpaceToChannelConfiguration> result = target.getAllSpaceToChannelConfigurations();

        assertThat(result, hasSize(1));

        assertSpaceToChannelConfiguration(result.get(0));
    }

    @Test
    public void getSpaceToChannelConfiguration_shouldReturnExpectedValue() {
        List<AOEntityToChannelMapping> mappings = Arrays.asList(entity1, entity2);
        ConversationsAndLinks conversationsAndLinks = new ConversationsAndLinks(
                ImmutableMap.of(new ConversationKey(TEAM_ID, CHANNEL_ID), conversation),
                ImmutableMap.of(TEAM_ID, slackLink),
                ImmutableMap.of(CHANNEL_ID, slackLink, CHANNEL_ID2, slackLink));

        when(entityToChannelMappingManager.getForEntity(SPACE_KEY)).thenReturn(mappings);
        when(userManager.getRemoteUserKey()).thenReturn(userKey);
        when(conversationLoaderHelper.conversationsAndLinksById(same(mappings), entityCaptor.capture(), loaderCaptor.capture()))
                .thenReturn(conversationsAndLinks);
        when(slackSettingService.isChannelMuted(CHANNEL_ID)).thenReturn(true);
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_TYPE_KEY)).thenReturn(Optional.of(notificationType));
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_TYPE_KEY2)).thenReturn(Optional.of(notificationType2));
        when(transactionTemplate.execute(any())).thenAnswer(args -> ((TransactionCallback) args.getArgument(0)).doInTransaction());

        when(entity1.getMessageTypeKey()).thenReturn(NOTIF_TYPE_KEY);
        when(entity1.getChannelId()).thenReturn(CHANNEL_ID);
        when(entity1.getTeamId()).thenReturn(TEAM_ID);
        when(entity2.getMessageTypeKey()).thenReturn(NOTIF_TYPE_KEY2);
        when(entity2.getChannelId()).thenReturn(CHANNEL_ID2);
        when(slackLink.getTeamName()).thenReturn(TEAM_NAME);
        when(slackLink.getTeamId()).thenReturn(TEAM_ID);
        when(conversation.getName()).thenReturn(CHANNEL_NAME);
        when(conversation.getId()).thenReturn(CHANNEL_ID);
        when(conversation.isPrivate()).thenReturn(true);

        SpaceToChannelConfiguration result = target.getSpaceToChannelConfiguration(SPACE_KEY);

        assertSpaceToChannelConfiguration(result);
    }

    private void assertSpaceToChannelConfiguration(SpaceToChannelConfiguration spaceToChannelConfiguration) {
        assertThat(spaceToChannelConfiguration.getSpace(), sameInstance(space));

        final List<SlackChannelDefinition> configuredChannels = new ArrayList<>(spaceToChannelConfiguration.getConfiguredChannels());
        assertThat(configuredChannels, hasSize(2));
        configuredChannels.sort(SlackChannelDefinition.ORDER_BY_NAME);

        assertThat(configuredChannels.get(0).getTeamId(), is(TEAM_ID));
        assertThat(configuredChannels.get(0).getChannelId(), is(CHANNEL_ID));
        assertThat(configuredChannels.get(0).getTeamName(), is(TEAM_NAME));
        assertThat(configuredChannels.get(0).getChannelName(), is(CHANNEL_NAME));
        assertThat(configuredChannels.get(0).isPrivate(), is(true));
        assertThat(configuredChannels.get(0).isMuted(), is(true));

        assertThat(configuredChannels.get(1).getTeamId(), is(TEAM_ID));
        assertThat(configuredChannels.get(1).getChannelId(), is(CHANNEL_ID2));
        assertThat(configuredChannels.get(1).getTeamName(), is(TEAM_NAME));
        assertThat(configuredChannels.get(1).getChannelName(), is("id:" + CHANNEL_ID2));
        assertThat(configuredChannels.get(1).isPrivate(), is(true));
        assertThat(configuredChannels.get(1).isMuted(), is(false));

        assertThat(spaceToChannelConfiguration.getChannelConfiguration(configuredChannels.get(0)).isPresent(), is(true));
        assertThat(spaceToChannelConfiguration.getChannelConfiguration(configuredChannels.get(0)).get(),
                contains(sameInstance(notificationType)));
        assertThat(spaceToChannelConfiguration.getChannelConfiguration(configuredChannels.get(1)).isPresent(), is(true));
        assertThat(spaceToChannelConfiguration.getChannelConfiguration(configuredChannels.get(1)).get(),
                contains(sameInstance(notificationType2)));

        assertThat(spaceToChannelConfiguration.getChannelsForNotification(notificationType), hasSize(1));
        assertThat(spaceToChannelConfiguration.getChannelsForNotification(notificationType).iterator().next(),
                sameInstance(configuredChannels.get(0)));

        assertThat(spaceToChannelConfiguration.getAllSpaceSettings().keySet(),
                containsInAnyOrder(configuredChannels.get(0), configuredChannels.get(1)));

        assertThat(spaceToChannelConfiguration.isChannelNotificationEnabled(configuredChannels.get(0), notificationType),
                is(true));
        assertThat(spaceToChannelConfiguration.isChannelNotificationEnabled(configuredChannels.get(0), notificationType2),
                is(false));
        assertThat(spaceToChannelConfiguration.isChannelNotificationEnabled(configuredChannels.get(1), notificationType2),
                is(true));

        assertThat(entityCaptor.getValue().apply(entity1), allOf(
                hasProperty("teamId", is(TEAM_ID)),
                hasProperty("channelId", is(CHANNEL_ID))
        ));

        // assert the loader lambda works as expected
        when(client.getConversationsInfo(CHANNEL_ID)).thenReturn(Either.right(conversation));
        when(client.withUserTokenIfAvailable(USER)).thenReturn(Optional.of(client));

        final ConversationKey conversationKey = new ConversationKey(TEAM_ID, CHANNEL_ID);
        Optional<Conversation> captor2Apply = loaderCaptor.getValue().apply(client, conversationKey);
        verify(client).withUserTokenIfAvailable(USER);
        assertThat(captor2Apply.isPresent(), is(true));
        assertThat(captor2Apply.get(), sameInstance(conversation));
    }

    @Test
    public void getSpaceToChannelSettings_shouldReturnExpectedValue() {
        List<AOEntityToChannelMapping> mappings = Collections.singletonList(entity1);

        when(entityToChannelMappingManager.getForEntityAndChannel(SPACE_KEY, CHANNEL_ID)).thenReturn(mappings);
        when(notificationTypeService.getNotificationTypeForKey(NOTIF_TYPE_KEY)).thenReturn(Optional.of(notificationType));
        when(entity1.getMessageTypeKey()).thenReturn(NOTIF_TYPE_KEY);

        Optional<SpaceToChannelSettings> result = target.getSpaceToChannelSettings(SPACE_KEY, CHANNEL_ID);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getNotificationTypes(), contains(notificationType));
    }

    @Test
    public void hasSpaceToChannelConfiguration_shouldReturnExpectedValue() {
        when(entityToChannelMappingManager.hasConfigurationForEntity(SPACE_KEY)).thenReturn(true);

        boolean result = target.hasSpaceToChannelConfiguration(SPACE_KEY);

        assertThat(result, is(true));
    }

    @Test
    public void hasMappingForEntityChannelAndType_shouldReturnExpectedValue() {
        when(entityToChannelMappingManager.hasConfigurationForEntityChannelAndType(SPACE_KEY, CHANNEL_ID, notificationType))
                .thenReturn(true);

        boolean result = target.hasMappingForEntityChannelAndType(SPACE_KEY, CHANNEL_ID, notificationType);

        assertThat(result, is(true));
    }

    @Test
    public void removeNotificationsForTeam_shouldCallExpectedMethod() {
        target.removeNotificationsForTeam(TEAM_ID);

        verify(entityToChannelMappingManager).removeNotificationsForTeam(TEAM_ID);
    }

    @Test
    public void addNotificationForSpaceAndChannel_shouldCallExpectedMethod() {
        target.addNotificationForSpaceAndChannel(SPACE_KEY, USER, TEAM_ID, CHANNEL_ID, notificationType);

        verify(entityToChannelMappingManager).addNotificationForEntityAndChannel(
                SPACE_KEY, USER, TEAM_ID, CHANNEL_ID, notificationType);
    }

    @Test
    public void removeNotificationsForSpaceAndChannel_shouldCallExpectedMethod() {
        target.removeNotificationsForSpaceAndChannel(SPACE_KEY, CHANNEL_ID);

        verify(entityToChannelMappingManager).removeNotificationsForEntityAndChannel(
                SPACE_KEY, CHANNEL_ID);
    }

    @Test
    public void removeNotificationForSpaceAndChannel_shouldCallExpectedMethod() {
        target.removeNotificationForSpaceAndChannel(SPACE_KEY, CHANNEL_ID, notificationType);

        verify(entityToChannelMappingManager).removeNotificationForEntityAndChannel(
                SPACE_KEY, CHANNEL_ID, notificationType);
    }

    @Test
    public void removeNotificationsForChannel_shouldCallExpectedMethod() {
        target.removeNotificationsForChannel(CHANNEL_ID);

        verify(entityToChannelMappingManager).removeNotificationsForChannel(CHANNEL_ID);
    }

    @Test
    public void removeNotificationsForSpace_shouldCallExpectedMethod() {
        target.removeNotificationsForSpace(SPACE_KEY);

        verify(entityToChannelMappingManager).removeNotificationsForEntity(SPACE_KEY);
    }
}
