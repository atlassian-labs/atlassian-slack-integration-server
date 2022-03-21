package com.atlassian.bitbucket.plugins.slack.notification.configuration.dao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.bitbucket.ao.AbstractAoDao;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.DefaultChannelDetails;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.DefaultRepositoryConfiguration;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationDisableRequest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationEnableRequest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.NotificationSearchRequest;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.RepositoryConfiguration;
import com.atlassian.bitbucket.plugins.slack.notification.configuration.ao.AoNotificationConfiguration;
import com.atlassian.bitbucket.plugins.slack.settings.BitbucketSlackSettingsService;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageUtils;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.ConversationLoaderHelper;
import com.atlassian.plugins.slack.api.client.ConversationsAndLinks;
import com.atlassian.plugins.slack.api.notification.ChannelToNotify;
import com.atlassian.plugins.slack.api.notification.Verbosity;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.github.seratch.jslack.api.model.Conversation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.atlassian.bitbucket.util.MoreStreams.streamIterable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Component
public class AoNotificationConfigurationDao extends AbstractAoDao implements NotificationConfigurationDao {
    private static final String AND_QUERY = " = ? AND ";
    private static final Logger logger = LoggerFactory.getLogger(AoNotificationConfigurationDao.class);

    private final ConversationLoaderHelper conversationLoaderHelper;
    private final RepositoryService repositoryService;
    private final TransactionTemplate transactionTemplate;
    private final BitbucketSlackSettingsService bitbucketSlackSettingsService;
    private final Comparator<DefaultRepositoryConfiguration> configurationComparator;

    @Autowired
    public AoNotificationConfigurationDao(final ActiveObjects ao,
                                          final ConversationLoaderHelper conversationLoaderHelper,
                                          final RepositoryService repositoryService,
                                          final TransactionTemplate transactionTemplate,
                                          final BitbucketSlackSettingsService bitbucketSlackSettingsService) {
        super(ao);
        this.conversationLoaderHelper = conversationLoaderHelper;
        this.repositoryService = repositoryService;
        this.transactionTemplate = transactionTemplate;
        this.bitbucketSlackSettingsService = bitbucketSlackSettingsService;

        this.configurationComparator = Comparator.<DefaultRepositoryConfiguration, String>
                comparing(repoConfig -> repoConfig.getRepository().getProject().getName())
                .thenComparing(repoConfig -> repoConfig.getRepository().getName());
    }

    @Override
    public void create(@Nonnull final NotificationEnableRequest request) {
        transactionTemplate.execute(() -> {
            Iterable<String> notificationTypeKeys = request.getNotificationTypeKeys();
            for (String notificationTypeKey : notificationTypeKeys) {

                Optional<Repository> repository = request.getRepository();
                Optional<String> channelId = request.getChannelId();
                Optional<String> teamId = request.getTeamId();
                if (!notificationExists(Optional.of(notificationTypeKey), repository, teamId, channelId)) {
                    ao.create(AoNotificationConfiguration.class,
                            ImmutableMap.of(
                                    AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, notificationTypeKey,
                                    AoNotificationConfiguration.REPO_ID_COLUMN, repository.get().getId(),
                                    AoNotificationConfiguration.TEAM_ID_COLUMN, teamId.get(),
                                    AoNotificationConfiguration.CHANNEL_ID_COLUMN, channelId.get()));
                }
            }
            return null;
        });
    }

    @Override
    public void delete(@Nonnull NotificationDisableRequest request) {
        StringBuilder query = new StringBuilder();
        ImmutableList.Builder<Object> parameters = ImmutableList.builder();

        Iterable<String> notificationTypeKeys = request.getNotificationTypeKeys();
        if (!Iterables.isEmpty(notificationTypeKeys)) {
            List<Character> inputs = Collections.nCopies(Iterables.size(notificationTypeKeys), '?');
            parameters.addAll(notificationTypeKeys);
            query.append(AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN + " IN (" + StringUtils.join(inputs, ',') + ")  AND ");
        }

        where(AoNotificationConfiguration.REPO_ID_COLUMN,
                request.getRepository(),
                parameters,
                query,
                Repository::getId);

        where(AoNotificationConfiguration.CHANNEL_ID_COLUMN,
                request.getChannelId(),
                parameters,
                query,
                value -> value);

        ao.deleteWithSQL(AoNotificationConfiguration.class,
                removeTrailingAND(query),
                parameters.build().toArray());
    }

    @Override
    public void removeNotificationsForTeam(@Nonnull final String teamId) {
        StringBuilder query = new StringBuilder();
        ImmutableList.Builder<Object> parameters = ImmutableList.builder();

        where(AoNotificationConfiguration.TEAM_ID_COLUMN,
                Optional.of(teamId),
                parameters,
                query,
                value -> value);

        ao.deleteWithSQL(AoNotificationConfiguration.class,
                removeTrailingAND(query),
                parameters.build().toArray());
    }

    @Override
    public void removeNotificationsForChannel(@Nonnull final ConversationKey conversationKey) {
        StringBuilder query = new StringBuilder();
        ImmutableList.Builder<Object> parameters = ImmutableList.builder();
        where(AoNotificationConfiguration.TEAM_ID_COLUMN,
                Optional.of(conversationKey.getTeamId()),
                parameters,
                query,
                value -> value);

        where(AoNotificationConfiguration.CHANNEL_ID_COLUMN,
                Optional.of(conversationKey.getChannelId()),
                parameters,
                query,
                value -> value);

        ao.deleteWithSQL(AoNotificationConfiguration.class,
                removeTrailingAND(query),
                parameters.build().toArray());
    }

    @Override
    public Set<ChannelToNotify> getChannelsToNotify(NotificationSearchRequest request) {
        Query query = createQuery(Optional.empty(),
                request.getNotificationTypeKey(),
                request.getRepository());
        AoNotificationConfiguration[] configurations = ao.find(AoNotificationConfiguration.class, query);

        return Arrays.stream(configurations)
                .map(c -> new ChannelToNotify(c.getTeamId(), c.getChannelId(), null, false))
                .collect(toSet());
    }

    @Nonnull
    @Override
    public Page<RepositoryConfiguration> search(@Nonnull NotificationSearchRequest request, @Nonnull PageRequest pageRequest) {
        Optional<String> notificationType = request.getNotificationTypeKey();
        Optional<Repository> repository = request.getRepository();

        Page<AoNotificationConfiguration> pageRepoId = queryRepoIds(pageRequest, notificationType, repository);

        List<AoNotificationConfiguration> notificationConfigs = queryNotificationConfigs(notificationType, pageRepoId);

        final ConversationsAndLinks conversationsAndLinks = conversationLoaderHelper.conversationsAndLinksById(
                notificationConfigs,
                config -> new ConversationKey(config.getTeamId(), config.getChannelId()),
                (client, channel) -> client.withRemoteUserTokenIfAvailable()
                        .flatMap(userClient -> userClient.getConversationsInfo(channel.getChannelId())
                                .fold(e -> Optional.empty(), Optional::of)));

        final Map<Integer, List<AoNotificationConfiguration>> reposById = notificationConfigs.stream()
                .collect(groupingBy(AoNotificationConfiguration::getRepoId, mapping(a -> a, toList())));

        List<RepositoryConfiguration> repoConfigs = reposById.entrySet().stream()
                .flatMap(entry -> {
                    Integer repoId = entry.getKey();
                    Repository repo = repositoryService.getById(repoId);
                    if (repo == null) {
                        logger.error("Repository with ID " + repoId + " does not exist!");
                        deleteRepositoryNotifications(repoId);

                        return Stream.empty();
                    }
                    DefaultRepositoryConfiguration.Builder builder = new DefaultRepositoryConfiguration.Builder(repo);
                    entry.getValue()
                            .forEach(aoConfig -> {
                                final Optional<Conversation> conversation = conversationsAndLinks.conversation(new ConversationKey(aoConfig.getTeamId(), aoConfig.getChannelId()));
                                final Optional<SlackLink> slackLink = conversationsAndLinks.link(aoConfig.getTeamId());
                                Verbosity verbosity = bitbucketSlackSettingsService.getVerbosity(repo.getId(),
                                        aoConfig.getTeamId(), aoConfig.getChannelId());
                                if (conversation.isPresent()) {
                                    builder.channelConfiguration(aoConfig.getNotificationType(), DefaultChannelDetails.builder()
                                            .teamId(aoConfig.getTeamId())
                                            .teamName(slackLink.map(SlackLink::getTeamName).orElse(teamNamePlaceholder(aoConfig.getTeamId())))
                                            .channelId(aoConfig.getChannelId())
                                            .channelName(conversation.get().getName())
                                            .muted(conversation.get().isArchived())
                                            .verbosity(verbosity.name())
                                            .build());
                                } else {
                                    builder.channelConfiguration(aoConfig.getNotificationType(), DefaultChannelDetails.builder()
                                            .teamId(aoConfig.getTeamId())
                                            .teamName(slackLink.map(SlackLink::getTeamName).orElse(teamNamePlaceholder(aoConfig.getTeamId())))
                                            .channelId(aoConfig.getChannelId())
                                            .channelName("private:" + aoConfig.getChannelId())
                                            .muted(false)
                                            .verbosity(verbosity.name())
                                            .build());
                                }
                            });
                    return Stream.of(builder.build());
                })
                // sort repository mappings alphabetically
                .sorted(configurationComparator)
                .collect(toList());

        return PageUtils.createPage(repoConfigs, pageRepoId.getIsLastPage(), pageRequest);
    }

    private Query createQuery(Optional<String> notificationTypeKey, Optional<Repository> repository) {
        return createQuery(Optional.empty(), notificationTypeKey, repository);
    }

    private Query createQuery(Optional<String> column, Optional<String> notificationTypeKey, Optional<Repository> repository) {
        Query query = column.map(Query::select).orElseGet(() -> Query.select(AoNotificationConfiguration.ALL_FIELDS));

        where(AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, notificationTypeKey, query, value -> value);
        where(AoNotificationConfiguration.REPO_ID_COLUMN, repository, query, Repository::getId);

        query.distinct();
        query.order(AoNotificationConfiguration.REPO_ID_COLUMN + " ASC");

        return query;
    }

    private void deleteRepositoryNotifications(Integer repoId) {
        StringBuilder query = new StringBuilder();
        ImmutableList.Builder<Object> parameters = ImmutableList.builder();

        where(AoNotificationConfiguration.REPO_ID_COLUMN,
                Optional.of(repoId),
                parameters,
                query,
                value -> value);

        logger.warn("Deleting all slack notification settings for repository with ID " + repoId);
        ao.deleteWithSQL(AoNotificationConfiguration.class,
                removeTrailingAND(query),
                parameters.build().toArray());
    }

    private boolean notificationExists(Optional<String> notificationTypeKey, Optional<Repository> repository,
                                       Optional<String> teamId, Optional<String> channelId) {
        Query query = createQuery(notificationTypeKey, repository);

        where(AoNotificationConfiguration.TEAM_ID_COLUMN, teamId, query, value -> value);
        where(AoNotificationConfiguration.CHANNEL_ID_COLUMN, channelId, query, value -> value);
        return ao.find(AoNotificationConfiguration.class, query).length > 0;
    }

    private ImmutableList<AoNotificationConfiguration> queryNotificationConfigs(Optional<String> notificationTypeKey, Page<AoNotificationConfiguration> pageRepoId) {
        ImmutableList.Builder<AoNotificationConfiguration> builder = ImmutableList.builder();
        Set<Integer> repoIds = streamIterable(pageRepoId.getValues())
                .map(AoNotificationConfiguration::getRepoId)
                .collect(toSet());

        //MySQL has performance issues with long in clauses, and Oracle has a hard limit of 1000 entries in
        //an in clause. To reduce the likelihood of encountering an issue, querying 100 rows at a time
        for (List<Integer> ids : Iterables.partition(repoIds, 100)) {
            Query query = Query.select(AoNotificationConfiguration.ALL_FIELDS);
            query.where(AoNotificationConfiguration.REPO_ID_COLUMN + " IN (" + StringUtils.join(ids, ',') + ") ");
            where(AoNotificationConfiguration.NOTIFICATION_TYPE_COLUMN, notificationTypeKey, query, value -> value);

            builder.add(ao.find(AoNotificationConfiguration.class, query));
        }
        return builder.build();
    }

    private Page<AoNotificationConfiguration> queryRepoIds(PageRequest pageRequest,
                                                           Optional<String> notificationTypeKey,
                                                           Optional<Repository> repository) {
        Query repoIdQuery = createQuery(Optional.of(AoNotificationConfiguration.REPO_ID_COLUMN), notificationTypeKey, repository);

        return pageQuery(AoNotificationConfiguration.class, repoIdQuery, pageRequest);
    }

    private String removeTrailingAND(StringBuilder query) {
        int length = query.length();
        if (length > 0) {
            return query.substring(0, length - 5);
        }
        return query.toString();
    }

    private <A> void where(String column,
                           Optional<A> option,
                           Query query,
                           Function<A, Object> function) {
        option.ifPresent(value -> {
            String whereClause = query.getWhereClause();
            Object[] whereParams = query.getWhereParams();

            if (StringUtils.isEmpty(whereClause)) {
                query.where(column + " = ? ", function.apply(value));
            } else {
                StringBuilder whereBuilder = new StringBuilder(whereClause);
                query.where(whereBuilder
                                .append(" AND ")
                                .append(column + " = ? ").toString(),
                        ImmutableList.builder()
                                .add(whereParams)
                                .add(function.apply(value)).build().toArray());
            }
        });
    }

    private <A> void where(String column,
                           Optional<A> option,
                           ImmutableList.Builder<Object> parameters,
                           StringBuilder query,
                           Function<A, Object> function) {
        option.ifPresent(value -> {
            query.append(column);
            query.append(AND_QUERY);
            parameters.add(function.apply(value));
        });
    }

    private String teamNamePlaceholder(String teamId) {
        return "team:" + teamId;
    }
}
