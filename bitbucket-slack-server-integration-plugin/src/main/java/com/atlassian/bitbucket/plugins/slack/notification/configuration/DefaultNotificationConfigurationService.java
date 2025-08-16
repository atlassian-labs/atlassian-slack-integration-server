package com.atlassian.bitbucket.plugins.slack.notification.configuration;

import com.atlassian.bitbucket.event.repository.RepositoryDeletedEvent;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.permission.PermissionValidationService;
import com.atlassian.bitbucket.plugins.slack.event.analytic.RepositoryNotificationDisabledAnalyticEvent;
import com.atlassian.bitbucket.plugins.slack.event.analytic.RepositoryNotificationEnabledAnalyticEvent;
import com.atlassian.bitbucket.plugins.slack.notification.NotificationUtil;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.dao.NotificationConfigurationDao;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DefaultNotificationConfigurationService implements NotificationConfigurationService {
    private final NotificationConfigurationDao dao;
    private final EventPublisher eventPublisher;
    private final PermissionValidationService permissionService;
    private final SlackSettingService settingService;
    private final AnalyticsContextProvider analyticsContextProvider;

    @Override
    public void disable(@Nonnull NotificationDisableRequest request) {
        checkPermission(request);

        dao.delete(request);

        Set<String> notificationTypeKeys = request.getNotificationTypeKeys();
        AnalyticsContext context = analyticsContextProvider.byTeamId(request.getTeamId().orElse(null));
        int repoId = request.getRepository().map(Repository::getId).orElse(0);
        String channelId = request.getChannelId().orElse(null);
        if (notificationTypeKeys.isEmpty()) {
            notificationTypeKeys = NotificationUtil.ALL_NOTIFICATION_TYPE_KEYS;
        }
        notificationTypeKeys.forEach(key -> eventPublisher.publish(
                new RepositoryNotificationDisabledAnalyticEvent(context, repoId, channelId, key)));
    }

    @Override
    public void enable(@Nonnull NotificationEnableRequest request) {
        checkPermission(request);

        Repository repository = request.getRepository().get();
        String channelId = request.getChannelId().get();
        String teamId = request.getTeamId().get();

        dao.create(request);

        AnalyticsContext context = analyticsContextProvider.byTeamId(teamId);
        request.getNotificationTypeKeys().forEach(key -> eventPublisher.publish(
                new RepositoryNotificationEnabledAnalyticEvent(context, repository.getId(), channelId, key)));
    }

    @Override
    public void removeNotificationsForTeam(@Nonnull final String teamId) {
        dao.removeNotificationsForTeam(teamId);
    }

    @Override
    public void removeNotificationsForChannel(@Nonnull final ConversationKey conversationKey) {
        dao.removeNotificationsForChannel(conversationKey);
    }

    @Nonnull
    @Override
    //This service is not secured, its being used by the Slack NotificationEventListener
    public Set<ChannelToNotify> getChannelsToNotify(@Nonnull NotificationSearchRequest request) {
        Set<ChannelToNotify> channels = dao.getChannelsToNotify(request);
        Set<ChannelToNotify> unmutedChannels = channels.stream()
                .filter(channel -> !settingService.isChannelMuted(new ConversationKey(channel.getTeamId(), channel.getChannelId())))
                .collect(Collectors.toSet());
        return unmutedChannels;
    }

    @EventListener
    public void onRepositoryDeleted(@Nonnull RepositoryDeletedEvent event) {
        NotificationDisableRequest request = new NotificationDisableRequest.Builder()
                .repository(event.getRepository())
                .build();
        dao.delete(request);
    }

    @Nonnull
    @Override
    public Page<RepositoryConfiguration> search(@Nonnull NotificationSearchRequest request, @Nonnull PageRequest pageRequest) {
        checkPermission(request);

        return dao.search(request, pageRequest);
    }

    private void checkPermission(AbstractNotificationRequest request) {
        Optional<Repository> repository = request.getRepository();
        if (repository.isPresent()) {
            permissionService.validateForRepository(repository.get(), Permission.REPO_ADMIN);
        } else {
            permissionService.validateForGlobal(Permission.ADMIN);
        }
    }
}
