package com.atlassian.plugins.slack.api.client;

import com.atlassian.plugins.slack.api.SlackUser;

import java.util.Optional;

public interface RetryUser {
    Optional<SlackClient> withClient(SlackClient client);

    static RetryUser botUser() {
        return Optional::of;
    }

    static RetryUser installerUser() {
        return client -> Optional.of(client.withInstallerUserToken());
    }

    static RetryUser remoteUser() {
        return SlackClient::withRemoteUserTokenIfAvailable;
    }

    static RetryUser userKey(String userKey) {
        return client -> client.withUserTokenIfAvailable(userKey);
    }

    static RetryUser userKey(Optional<String> optionalUserKey) {
        return client -> optionalUserKey.flatMap(client::withUserTokenIfAvailable);
    }

    static RetryUser user(SlackUser user) {
        return client -> client.withUserTokenIfAvailable(user);
    }

    static RetryUser user(Optional<SlackUser> optionalUser) {
        return client -> optionalUser.flatMap(client::withUserTokenIfAvailable);
    }
}
