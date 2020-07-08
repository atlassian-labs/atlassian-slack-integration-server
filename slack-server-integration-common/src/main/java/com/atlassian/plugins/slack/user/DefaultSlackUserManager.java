package com.atlassian.plugins.slack.user;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.ao.AOSlackUser;
import com.atlassian.plugins.slack.api.ImmutableSlackUser;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.events.SlackUserMappedEvent;
import com.atlassian.plugins.slack.api.events.SlackUserUnmappedEvent;
import com.atlassian.plugins.slack.api.events.SlackUserUpdatedEvent;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserKey;
import com.google.common.base.Preconditions;
import net.java.ao.DBParam;
import net.java.ao.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

@Component
public class DefaultSlackUserManager implements SlackUserManager {
    private static final String USER_KEY_CLAUSE = "USER_KEY = ?";
    private static final String SLACK_USER_ID_CLAUSE = "SLACK_USER_ID = ?";
    private static final String TEAM_ID_CLAUSE = "SLACK_TEAM_ID = ?";
    private static final String DISCONNECTED_CLAUSE = "CONNECTION_ERROR IS NOT NULL";

    private final ActiveObjects ao;
    private final EventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public DefaultSlackUserManager(
            final ActiveObjects ao,
            final EventPublisher eventPublisher,
            final TransactionTemplate transactionTemplate) {
        this.ao = ao;
        this.eventPublisher = eventPublisher;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Optional<SlackUser> getBySlackUserId(final String slackUserId) {
        return getAoBySlackUserId(slackUserId)
                .map(ImmutableSlackUser::new);
    }

    @Override
    public List<SlackUser> getByUserKey(final UserKey userKey) {
        final AOSlackUser[] results = ao.find(
                AOSlackUser.class,
                Query.select().where(USER_KEY_CLAUSE, userKey.getStringValue()));

        if (isEmpty(results)) {
            return Collections.emptyList();
        }

        return toList(results);
    }

    @Override
    public List<SlackUser> getByTeamId(final String teamId) {
        final AOSlackUser[] results = ao.find(
                AOSlackUser.class,
                Query.select().where(TEAM_ID_CLAUSE, teamId));

        if (isEmpty(results)) {
            return Collections.emptyList();
        }

        return toList(results);
    }

    @Override
    public Optional<SlackUser> getByTeamIdAndUserKey(final String teamId, final String userKey) {
        final AOSlackUser[] results = ao.find(
                AOSlackUser.class,
                Query.select().where(TEAM_ID_CLAUSE + " AND " + USER_KEY_CLAUSE, teamId, userKey));

        if (isEmpty(results)) {
            return Optional.empty();
        }

        return Optional.of(results[0]);
    }

    @Override
    public List<SlackUser> getAll() {
        final AOSlackUser[] results = ao.find(AOSlackUser.class);

        if (isEmpty(results)) {
            return Collections.emptyList();
        }

        return toList(results);
    }

    @Override
    public List<SlackUser> findDisconnected() {
        final AOSlackUser[] users = ao.find(AOSlackUser.class, Query.select().where(DISCONNECTED_CLAUSE));

        if (isEmpty(users)) {
            return Collections.emptyList();
        }

        return toList(users);
    }

    @Override
    public SlackUser create(final String slackUserId,
                            final UserKey userKey,
                            final SlackLink slackLink) {
        return transactionTemplate.execute(() -> {
            final Optional<AOSlackUser> existing = getAoBySlackUserId(slackUserId);
            if (!existing.isPresent()) {
                final AOSlackUser user = ao.create(AOSlackUser.class,
                        new DBParam("SLACK_USER_ID", slackUserId),
                        new DBParam("USER_KEY", userKey.getStringValue()),
                        new DBParam("SLACK_TEAM_ID", slackLink.getTeamId()));
                eventPublisher.publish(new SlackUserMappedEvent(AnalyticsContext.fromSlackUser(user)));
                return toImmutable(user);
            } else {
                return update(existing.get(), userKey, slackLink);
            }
        });
    }

    @Override
    public Optional<SlackUser> update(final String slackUserId,
                                      final UserKey userKey,
                                      final SlackLink slackLink) {
        Preconditions.checkNotNull(slackUserId, "slackUserId should not be null");
        return transactionTemplate.execute(() -> getAoBySlackUserId(slackUserId)
                .map(link -> update(link, userKey, slackLink)));
    }

    @Override
    public void updatePersonalToken(final String slackUserId, final String token) {
        Preconditions.checkNotNull(slackUserId, "slackUserId should not be null");
        transactionTemplate.execute(() -> getAoBySlackUserId(slackUserId)
                .map(existing -> {
                    existing.setUserToken(token);
                    existing.setConnectionError(null);
                    existing.save();
                    return existing;
                })).ifPresent(user ->
                eventPublisher.publish(AnalyticsContext.fromSlackUser(user)));
    }

    @Override
    public void revokeToken(final String slackUserId) {
        Preconditions.checkNotNull(slackUserId, "slackUserId should not be null");
        transactionTemplate.execute(() -> {
            getAoBySlackUserId(slackUserId).ifPresent(user -> {
                user.setUserToken(null);
                user.setConnectionError("Token was revoked");
                user.save();
            });
            return null;
        });
    }

    @Override
    public void delete(final SlackUser user) {
        transactionTemplate.execute(() -> {
            getAoBySlackUserId(user.getSlackUserId()).ifPresent(ao::delete);
            return null;
        });
        eventPublisher.publish(new SlackUserUnmappedEvent(AnalyticsContext.fromSlackUser(user)));
    }

    private Optional<AOSlackUser> getAoBySlackUserId(final String slackUserId) {
        final AOSlackUser[] results = ao.find(
                AOSlackUser.class,
                Query.select().where(SLACK_USER_ID_CLAUSE, slackUserId));

        if (isEmpty(results)) {
            return Optional.empty();
        }

        return Optional.of(results[0]);
    }

    private SlackUser update(final AOSlackUser user,
                             final UserKey userKey,
                             final SlackLink slackLink) {
        Preconditions.checkNotNull(user, "user should not be null");
        user.setUserKey(userKey.getStringValue());
        user.setSlackTeamId(slackLink.getTeamId());
        user.save();
        eventPublisher.publish(new SlackUserUpdatedEvent(AnalyticsContext.fromSlackUser(user)));
        return toImmutable(user);
    }

    private List<SlackUser> toList(final AOSlackUser[] results) {
        return Arrays.stream(results)
                .map(ImmutableSlackUser::new)
                .collect(Collectors.toList());
    }

    private SlackUser toImmutable(AOSlackUser result) {
        return new ImmutableSlackUser(result);
    }
}
