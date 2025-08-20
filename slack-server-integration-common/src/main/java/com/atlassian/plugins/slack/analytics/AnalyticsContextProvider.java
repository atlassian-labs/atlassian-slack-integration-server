package com.atlassian.plugins.slack.analytics;

import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AnalyticsContextProvider {
    private final UserManager userManager;
    private final SlackUserManager slackUserManager;

    public AnalyticsContext byTeamIdAndUserKey(final String teamId, final String userKey) {
        Optional<SlackUser> slackUser = slackUserManager.getByTeamIdAndUserKey(teamId, userKey);
        String slackUserId = slackUser.map(SlackUser::getSlackUserId).orElse(null);

        return new AnalyticsContext(userKey, teamId, slackUserId);
    }

    public AnalyticsContext byTeamIdAndSlackUserId(final String teamId, final String slackUserId) {
        List<SlackUser> slackUsers = slackUserManager.getByTeamId(teamId);
        Optional<SlackUser> slackUser = slackUsers.stream()
                .filter(user -> Objects.equals(slackUserId, user.getSlackUserId()))
                .findAny();
        String userKey = slackUser.map(SlackUser::getUserKey).orElse(null);

        return new AnalyticsContext(userKey, teamId, slackUserId);
    }

    public AnalyticsContext bySlackLink(@Nullable final SlackLink slackLink) {
        return slackLink != null ? byTeamId(slackLink.getTeamId()) : current();
    }

    public AnalyticsContext byTeamId(@Nullable final String teamId) {
        AnalyticsContext context;
        if (teamId == null) {
            context = current();
        } else {
            String slackUserId = null;
            String userKeyStr = null;

            UserKey userKey = userManager.getRemoteUserKey();
            if (userKey != null) {
                userKeyStr = userKey.getStringValue();
                Optional<SlackUser> slackUser = slackUserManager.getByTeamIdAndUserKey(teamId, userKeyStr);
                slackUserId = slackUser.map(SlackUser::getSlackUserId).orElse(null);
            }

            context = new AnalyticsContext(userKeyStr, teamId, slackUserId);
        }

        return context;
    }

    public AnalyticsContext current() {
        String teamId = null;
        String slackUserId = null;
        String userKeyStr = null;

        UserKey userKey = userManager.getRemoteUserKey();
        if (userKey != null) {
            userKeyStr = userKey.getStringValue();
            List<SlackUser> slackUsers = slackUserManager.getByUserKey(userKey);
            if (!slackUsers.isEmpty()) {
                SlackUser slackUser = slackUsers.get(0);
                teamId = slackUser.getSlackTeamId();
                slackUserId = slackUser.getSlackUserId();
            }
        }

        return new AnalyticsContext(userKeyStr, teamId, slackUserId);
    }
}
