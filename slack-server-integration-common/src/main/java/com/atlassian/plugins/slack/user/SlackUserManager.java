package com.atlassian.plugins.slack.user;

import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.sal.api.user.UserKey;

import java.util.List;
import java.util.Optional;

public interface SlackUserManager {
    Optional<SlackUser> getBySlackUserId(String slackUserId);

    List<SlackUser> getByUserKey(UserKey userKey);

    Optional<SlackUser> getByTeamIdAndUserKey(final String teamId, String userKey);

    List<SlackUser> getByTeamId(String teamId);

    List<SlackUser> getAll();

    List<SlackUser> findDisconnected();

    SlackUser create(String slackUserId,
                     UserKey userKey,
                     SlackLink slackLink);

    Optional<SlackUser> update(String slackUserId,
                               UserKey userKey,
                               SlackLink slackLink);

    void updatePersonalToken(String slackUserId,
                             String token);

    void revokeToken(String slackUserId);

    /**
     * Remove the given slack user link.
     *
     * @param user the AO Slack user
     */
    void delete(SlackUser user);
}
