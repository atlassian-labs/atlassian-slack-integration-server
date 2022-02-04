package com.atlassian.plugins.slack.api.descriptor;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.ozymandias.SafeAccessViaPluginAccessor;
import com.atlassian.ozymandias.SafePluginPointAccess;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.notification.BaseSlackEvent;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.plugins.slack.api.notification.SlackNotification;
import com.atlassian.plugins.slack.api.notification.SlackNotificationContext;
import com.atlassian.plugins.slack.api.notification.SlackUserActionNotification;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DefaultNotificationTypeService implements NotificationTypeService {
    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationTypeService.class);

    private final SafeAccessViaPluginAccessor safePluginAccessor;
    private final I18nResolver i18nResolver;
    private final EventPublisher eventPublisher;
    private final SlackSettingService slackSettingService;

    @Autowired
    public DefaultNotificationTypeService(
            final PluginAccessor pluginAccessor,
            final I18nResolver i18nResolver,
            final EventPublisher eventPublisher,
            final SlackSettingService slackSettingService) {
        this.i18nResolver = i18nResolver;
        this.safePluginAccessor = SafePluginPointAccess.to(pluginAccessor);
        this.eventPublisher = Preconditions.checkNotNull(eventPublisher);
        this.slackSettingService = slackSettingService;
    }

    @Override
    public List<NotificationType> getNotificationTypes() {
        return safePluginAccessor.forType(SlackNotificationDescriptor.class, (moduleDescriptor, module) -> {
            return NotificationType.fromModuleDescriptor(moduleDescriptor, i18nResolver, module);
        });
    }

    @Override
    public List<NotificationType> getVisibleNotificationTypes() {
        return getNotificationTypes().stream()
                .filter(type -> type.getNotification()
                        .map(SlackNotification::shouldDisplayInConfiguration).orElse(false))
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationType> getNotificationTypes(final String context) {
        final List<NotificationType> notificationTypes = getVisibleNotificationTypes();
        return notificationTypes.stream()
                .filter(notificationType -> notificationType.getContext().equals(context))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<NotificationType> getNotificationTypeForKey(final String key) {
        final List<NotificationType> notifications = safePluginAccessor.forType(SlackNotificationDescriptor.class, (moduleDescriptor, module) -> {
            if (key.equals(moduleDescriptor.getValue())) {
                return NotificationType.fromModuleDescriptor(moduleDescriptor, i18nResolver, module);
            }
            return null;
        });

        return notifications.stream()
                .filter(Objects::nonNull)
                .findFirst();
    }

    @Override
    public List<ChannelNotification> getNotificationsForEvent(final Object event) {
        final ImmutableList.Builder<ChannelNotification> messages = ImmutableList.builder();
        final Set<Class> modulesSent = new HashSet<>();
        safePluginAccessor.forType(SlackNotificationDescriptor.class, (moduleDescriptor, module) -> {
            try {
                if (!modulesSent.contains(module.getClass()) && module.supports(event)) {
                    if (!module.shouldSend(event)) {
                        if (module instanceof SlackUserActionNotification && event instanceof BaseSlackEvent) {
                            @SuppressWarnings("unchecked") final SlackUserActionNotification<BaseSlackEvent> userActionNotification =
                                    (SlackUserActionNotification) module;
                            userActionNotification.buildNotificationBlockedEvent((BaseSlackEvent) event).ifPresent(eventPublisher::publish);
                        }
                        return;
                    }

                    final NotificationType notificationType = NotificationType.fromModuleDescriptor(moduleDescriptor, i18nResolver, module);
                    final Optional<SlackNotificationContext<Object>> context = getContext(notificationType.getContext());

                    if (!context.isPresent()) {
                        return;
                    }
                    modulesSent.add(module.getClass());

                    module.getSlackMessage(event).ifPresent(message ->
                            context.get().getChannels(event, notificationType).stream()
                                    // if the channel was archived, skip sending notifications to it
                                    .filter(channel -> !slackSettingService.isChannelMuted(new ConversationKey(channel.getTeamId(), channel.getChannelId())))
                                    .forEach(channel ->
                                            messages.add(new ChannelNotification(
                                                    channel.getTeamId(),
                                                    channel.getChannelId(),
                                                    notificationType.getKey(),
                                                    message.threadTs(channel.getThreadTs())))));
                }
            } catch (Throwable e) {
                log.error("Could not load Slack notifications: " + e.getMessage(), e);
            }
        });

        return messages.build();
    }

    private Optional<SlackNotificationContext<Object>> getContext(final String context) {
        final List<SlackNotificationContext<Object>> notifications = safePluginAccessor.forType(
                SlackNotificationContextDescriptor.class, (moduleDescriptor, module) -> {
                    if (context.equals(moduleDescriptor.getValue())) {
                        //noinspection unchecked
                        return (SlackNotificationContext<Object>) module;
                    }
                    return null;
                });

        return notifications.stream()
                .filter(Objects::nonNull)
                .findFirst();
    }
}
