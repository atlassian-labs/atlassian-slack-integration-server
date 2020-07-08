define('bitbucket-plugin-slack/feature/repo-channel-table/is-notification-enabled', [], function () {

    // This is an implementation of a soy function and should be kept aligned with server side implementation in
    // com.atlassian.confluence.plugins.slack.spacetochannel.soy.IsSlackChannelNotificationEnabledFunction
    return function isSlackNotificationEnabled (notificationTypeKey, channelConfig) {
        return (channelConfig.notificationConfigurationKeys || []).filter(function(key) {
            return notificationTypeKey === key;
        }).length > 0;
    }
});
