package com.atlassian.plugins.slack.api.descriptor;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugins.slack.api.events.NotificationBlockedEvent;
import com.atlassian.plugins.slack.api.notification.BaseSlackEvent;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.plugins.slack.api.notification.SlackNotification;
import com.atlassian.plugins.slack.api.notification.SlackNotificationContext;
import com.atlassian.plugins.slack.api.notification.SlackUserActionNotification;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class DefaultNotificationTypeServiceTest {
    @Mock
    private PluginAccessor pluginAccessor;
    @Mock
    private I18nResolver i18nResolver;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private SlackSettingService slackSettingService;

    @Mock
    private NotificationType notificationType;
    @Mock
    private SlackNotificationDescriptor slackNotificationDescriptor;
    @Mock
    private SlackNotificationDescriptor slackNotificationDescriptor2;
    @Mock
    private SlackNotificationContextDescriptor slackNotificationContextDescriptor;
    @Mock
    private SlackNotificationContext<Object> slackNotificationContext;
    @Mock
    private SlackNotification<Object> slackNotification;
    @Mock
    private SlackUserActionNotification slackNotificationNotToSend;
    @Mock
    private BaseSlackEvent event;
    @Mock
    private ChatPostMessageRequest.ChatPostMessageRequestBuilder messageRequestBuilder;
    @Mock
    private NotificationBlockedEvent<Object> notificationBlockedEvent;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private DefaultNotificationTypeService target;

    @Test
    public void getNotificationTypes_shouldReturnExpectedValue() {
        when(pluginAccessor.getEnabledModuleDescriptorsByClass(SlackNotificationDescriptor.class))
                .thenReturn(Arrays.asList(slackNotificationDescriptor, slackNotificationDescriptor2));
        populateDescriptor(slackNotificationDescriptor, "d1", "d1-ctx");
        populateDescriptor(slackNotificationDescriptor2, "d2", "d2-ctx");
        when(slackNotificationDescriptor.getModule()).thenReturn(slackNotification);
        when(slackNotificationDescriptor2.getModule()).thenReturn(slackNotificationNotToSend);

        List<NotificationType> result = target.getNotificationTypes();

        assertThat(result, hasSize(2));
        assertThat(result.get(0).getKey(), is("d1"));
        assertThat(result.get(0).getLabel(), is("d1-txt"));
        assertThat(result.get(0).getContext(), is("d1-ctx"));
        assertThat(result.get(0).getCategory(), is("d1-cat"));
        assertThat(result.get(0).getWeight(), is(1));
        assertThat(result.get(0).getNotification().isPresent(), is(true));
        assertThat(result.get(0).getNotification().get(), sameInstance(slackNotification));
        assertThat(result.get(1).getKey(), is("d2"));
        assertThat(result.get(1).getLabel(), is("d2-txt"));
        assertThat(result.get(1).getContext(), is("d2-ctx"));
        assertThat(result.get(1).getCategory(), is("d2-cat"));
        assertThat(result.get(1).getWeight(), is(1));
        assertThat(result.get(1).getNotification().isPresent(), is(true));
        assertThat(result.get(1).getNotification().get(), sameInstance(slackNotificationNotToSend));
    }

    @Test
    public void getNotificationTypes_shouldReturnExpectedValueForContext() {
        when(pluginAccessor.getEnabledModuleDescriptorsByClass(SlackNotificationDescriptor.class))
                .thenReturn(Arrays.asList(slackNotificationDescriptor, slackNotificationDescriptor2));
        populateDescriptor(slackNotificationDescriptor, "d1", "d1-ctx");
        populateDescriptor(slackNotificationDescriptor2, "d2", "d2-ctx");
        when(slackNotificationDescriptor.getModule()).thenReturn(slackNotification);
        when(slackNotificationDescriptor2.getModule()).thenReturn(slackNotificationNotToSend);
        when(slackNotification.shouldDisplayInConfiguration()).thenReturn(true);

        List<NotificationType> result = target.getNotificationTypes("d1-ctx");

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getKey(), is("d1"));
        assertThat(result.get(0).getLabel(), is("d1-txt"));
        assertThat(result.get(0).getContext(), is("d1-ctx"));
        assertThat(result.get(0).getCategory(), is("d1-cat"));
        assertThat(result.get(0).getWeight(), is(1));

        assertThat(result.get(0).getNotification().isPresent(), is(true));
        assertThat(result.get(0).getNotification().get(), sameInstance(slackNotification));
    }

    @Test
    public void getNotificationTypes_shouldReturnExpectedValueForShouldSendAttr() {
        when(pluginAccessor.getEnabledModuleDescriptorsByClass(SlackNotificationDescriptor.class))
                .thenReturn(Arrays.asList(slackNotificationDescriptor, slackNotificationDescriptor2));
        populateDescriptor(slackNotificationDescriptor, "d1", "ctx");
        populateDescriptor(slackNotificationDescriptor2, "d2", "ctx");
        when(slackNotificationDescriptor.getModule()).thenReturn(slackNotification);
        when(slackNotificationDescriptor2.getModule()).thenReturn(slackNotificationNotToSend);
        when(slackNotification.shouldDisplayInConfiguration()).thenReturn(true);
        when(slackNotificationNotToSend.shouldDisplayInConfiguration()).thenReturn(false);

        List<NotificationType> result = target.getNotificationTypes("ctx");

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getKey(), is("d1"));
        assertThat(result.get(0).getLabel(), is("d1-txt"));
        assertThat(result.get(0).getContext(), is("ctx"));
        assertThat(result.get(0).getCategory(), is("d1-cat"));
        assertThat(result.get(0).getWeight(), is(1));
        assertThat(result.get(0).getNotification().isPresent(), is(true));
        assertThat(result.get(0).getNotification().get(), sameInstance(slackNotification));
    }

    @Test
    public void getNotificationTypeForKey_shouldReturnExpectedValueForContext() {
        when(pluginAccessor.getEnabledModuleDescriptorsByClass(SlackNotificationDescriptor.class))
                .thenReturn(Arrays.asList(slackNotificationDescriptor, slackNotificationDescriptor2));
        populateDescriptor(slackNotificationDescriptor, "d1", "d1-ctx");
        when(slackNotificationDescriptor.getModule()).thenReturn(slackNotification);
        when(slackNotificationDescriptor2.getModule()).thenReturn(slackNotificationNotToSend);

        Optional<NotificationType> result = target.getNotificationTypeForKey("d1");

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().getKey(), is("d1"));
        assertThat(result.get().getLabel(), is("d1-txt"));
        assertThat(result.get().getContext(), is("d1-ctx"));
        assertThat(result.get().getCategory(), is("d1-cat"));
        assertThat(result.get().getWeight(), is(1));
        assertThat(result.get().getNotification().isPresent(), is(true));
        assertThat(result.get().getNotification().get(), sameInstance(slackNotification));
    }

    @Test
    public void getNotificationsForEvent_shouldReturnExpectedValue() {
        ChannelToNotify channelToNotify1 = new ChannelToNotify("", "C1", "", false);
        ChannelToNotify channelToNotify2 = new ChannelToNotify("T", "C2", "T2", false);

        when(pluginAccessor.getEnabledModuleDescriptorsByClass(SlackNotificationDescriptor.class))
                .thenReturn(Arrays.asList(slackNotificationDescriptor, slackNotificationDescriptor2));
        when(slackNotificationDescriptor.getModule()).thenReturn(slackNotification);
        when(slackNotificationDescriptor2.getModule()).thenReturn(slackNotificationNotToSend);
        when(slackNotification.supports(event)).thenReturn(true);
        when(slackNotification.shouldSend(event)).thenReturn(true);
        when(slackNotificationNotToSend.supports(event)).thenReturn(false);
        populateDescriptor(slackNotificationDescriptor, "d1", "ctx");

        when(pluginAccessor.getEnabledModuleDescriptorsByClass(SlackNotificationContextDescriptor.class))
                .thenReturn(Collections.singletonList(slackNotificationContextDescriptor));
        when(slackNotificationContextDescriptor.getValue()).thenReturn("ctx");
        when(slackNotificationContextDescriptor.getModule()).thenReturn(slackNotificationContext);
        when(slackNotification.getSlackMessage(event)).thenReturn(java.util.Optional.of(messageRequestBuilder));
        when(slackNotificationContext.getChannels(same(event), argThat(n -> Matchers.is("d1").matches(n.getKey()))))
                .thenReturn(Arrays.asList(channelToNotify1, channelToNotify2));
        when(slackSettingService.isChannelMuted("C1")).thenReturn(true);
        when(slackSettingService.isChannelMuted("C2")).thenReturn(false);
        when(messageRequestBuilder.threadTs("T2")).thenReturn(messageRequestBuilder);

        List<NotificationTypeService.ChannelNotification> result = target.getNotificationsForEvent(event);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getChannelId(), is("C2"));
        assertThat(result.get(0).getTeamId(), is("T"));
        assertThat(result.get(0).getMessage(), sameInstance(messageRequestBuilder));
        verify(messageRequestBuilder).threadTs("T2");
    }

    @Test
    public void getNotificationsForEvent_shouldSkipIfContextIsMissing() {
        when(pluginAccessor.getEnabledModuleDescriptorsByClass(SlackNotificationDescriptor.class))
                .thenReturn(Arrays.asList(slackNotificationDescriptor, slackNotificationDescriptor2));
        when(slackNotificationDescriptor.getModule()).thenReturn(slackNotification);
        when(slackNotificationDescriptor2.getModule()).thenReturn(slackNotificationNotToSend);
        when(slackNotification.supports(event)).thenReturn(true);
        when(slackNotification.shouldSend(event)).thenReturn(true);
        when(slackNotificationNotToSend.supports(event)).thenReturn(false);
        populateDescriptor(slackNotificationDescriptor, "d1", "ctx");

        when(pluginAccessor.getEnabledModuleDescriptorsByClass(SlackNotificationContextDescriptor.class))
                .thenReturn(Collections.emptyList());

        List<NotificationTypeService.ChannelNotification> result = target.getNotificationsForEvent(event);

        assertThat(result, hasSize(0));
    }

    @Test
    public void getNotificationsForEvent_shouldSkipIfNotTobeSent() {
        when(pluginAccessor.getEnabledModuleDescriptorsByClass(SlackNotificationDescriptor.class))
                .thenReturn(Collections.singletonList(slackNotificationDescriptor2));
        when(slackNotificationDescriptor2.getModule()).thenReturn(slackNotificationNotToSend);
        when(slackNotificationNotToSend.supports(event)).thenReturn(true);
        when(slackNotificationNotToSend.shouldSend(event)).thenReturn(false);
        when(slackNotificationNotToSend.buildNotificationBlockedEvent(event)).thenReturn(java.util.Optional.of(notificationBlockedEvent));

        List<NotificationTypeService.ChannelNotification> result = target.getNotificationsForEvent(event);

        assertThat(result, hasSize(0));
        verify(eventPublisher).publish(notificationBlockedEvent);
    }

    private void populateDescriptor(SlackNotificationDescriptor d, String value, String ctx) {
        when(d.getValue()).thenReturn(value);
        when(d.getI18nNameKey()).thenReturn(value + "-key");
        when(d.getContext()).thenReturn(ctx);
        when(d.getCategory()).thenReturn(value + "-cat");
        when(d.getWeight()).thenReturn(1);
        when(d.isActiveByDefault()).thenReturn(true);
        when(i18nResolver.getText(value + "-key")).thenReturn(value + "-txt");
    }

}
