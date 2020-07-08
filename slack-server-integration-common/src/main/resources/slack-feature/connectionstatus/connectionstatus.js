require([
    "jquery"
], function (
    $
) {
    var connectionStatusViews = {
        "CONNECTED": {
            status: Slack.Templates.Configuration.ConnectionStatus.connectedStatus
        },
        "PARTIALLY_CONNECTED": {
            status: Slack.Templates.Configuration.ConnectionStatus.partialConnectivityStatus,
            toolTipTitle: AJS.I18n.getText("plugins.slack.admin.connection.status.partial.connectivity.tooltip.title"),
            toolTipDescription: AJS.I18n.getText("plugins.slack.admin.connection.status.partial.connectivity.tooltip.description")
        },
        "NO_CONNECTION": {
            status: Slack.Templates.Configuration.ConnectionStatus.notConnectedStatus,
            toolTipTitle: AJS.I18n.getText("plugins.slack.admin.connection.status.not.connected.tooltip.title"),
            toolTipDescription: AJS.I18n.getText("plugins.slack.admin.connection.status.not.connected.tooltip.description")
        },
        "UNKNOWN": {
            status: Slack.Templates.Configuration.ConnectionStatus.unknownStatus,
            toolTipTitle: AJS.I18n.getText("plugins.slack.admin.connection.status.unknown.tooltip.title"),
            toolTipDescription: AJS.I18n.getText("plugins.slack.admin.connection.status.unknown.tooltip.description")
        },
        "OAUTH_FAILURE": {
            status: Slack.Templates.Configuration.ConnectionStatus.oauthFailureStatus,
            toolTipTitle: AJS.I18n.getText("plugins.slack.admin.connection.status.oauth.failure.tooltip.title"),
            toolTipDescription: AJS.I18n.getText("plugins.slack.admin.connection.status.oauth.failure.tooltip.description")
        }
    };

    var userConnectionStatusViews = {
        "CONNECTED": {
            status: Slack.Templates.Configuration.ConnectionStatus.connectedStatus
        },
        "UNKNOWN": {
            status: Slack.Templates.Configuration.ConnectionStatus.unknownStatus,
            toolTipTitle: AJS.I18n.getText("plugins.slack.admin.connection.status.unknown.tooltip.title"),
            toolTipDescription: AJS.I18n.getText("plugins.slack.admin.connection.status.unknown.tooltip.description")
        },
        "NO_CONNECTION": {
            status: Slack.Templates.Configuration.ConnectionStatus.notConnectedStatus,
            toolTipTitle: AJS.I18n.getText("plugins.slack.admin.connection.status.not.connected.tooltip.title"),
            toolTipDescription: AJS.I18n.getText("plugins.slack.admin.connection.status.not.connected.tooltip.description")
        },
        "OAUTH_FAILURE": {
            status: Slack.Templates.Configuration.ConnectionStatus.oauthFailureStatus,
            toolTipTitle: AJS.I18n.getText("plugins.slack.admin.connection.status.oauth.failure.tooltip.title"),
            toolTipDescription: AJS.I18n.getText("plugins.slack.admin.connection.user.status.oauth.failure.tooltip.description")
        }
    };

    function getViewDataForStatus(data, viewsMap) {
        var view = viewsMap[data.key];
        if (!view) {
            view = connectionStatusViews.UNKNOWN;
        }

        return view;
    }

    function getConnectionStatusHtml(status, viewsMap, toolTipId) {
        var view = getViewDataForStatus(status, viewsMap);

        return view.status({
            extraClasses: view.toolTipDescription || status.error ? "has-tooltip" : "",
            toolTipTitle: view.toolTipTitle,
            toolTipDescription: view.toolTipDescription,
            toolTipId: view.toolTipDescription ? toolTipId : '',
        });
    }

    function getConnectionStatus(teamId, user) {
        return $.ajax({
            url: AJS.contextPath() + "/rest/slack/latest/connection-status/" + teamId + (user ? '/user' : ''),
            cache: false,
            dataType: 'json',
            type: "GET"
        });
    }

    function updateConnectionStatus(status) {
        var $connectionStatus = $(".slack-connection-status");
        var statusHtml = getConnectionStatusHtml(status, connectionStatusViews, 'bot-status-popup');
        $connectionStatus.html(statusHtml);
    }

    function updateUserConnectionStatus(status) {
        var $userConnectionStatus = $(".slack-user-connection-status");
        var statusHtml = getConnectionStatusHtml(status, userConnectionStatusViews, 'user-status-popup');
        $userConnectionStatus.html(statusHtml);
    }

    AJS.toInit(function($) {
        var configuration = $('#slack-channel-configuration');
        var teamId = configuration.data('slack-team-id');
        var $connectionStatus = $(".slack-connection-status");
        if ($connectionStatus.length > 0) {
            $connectionStatus.spin();
            getConnectionStatus(teamId, false).done(function(response) {
                updateConnectionStatus({ key: response.status });
            }).fail(function(err) {
                // in case of app credentials error, do not show user oauth links at all
                $('.slack-server-integration-account-text').remove();
                $('.slack-server-integration-account-text-error').remove();
                $('.slack-server-integration-credentials-update').removeClass('hidden');
                var resp = err && err.responseText && JSON.parse(err.responseText);
                if (resp && resp.key) {
                    updateConnectionStatus(resp);
                } else {
                    updateConnectionStatus({ key: "UNKNOWN" });
                }
            }).always(function() {
                $connectionStatus.spinStop();
            });
        }

        var userConnectionStatus = $(".slack-user-connection-status");
        if (userConnectionStatus.length > 0) {
            userConnectionStatus.spin();
            getConnectionStatus(teamId, true).done(function() {
                updateUserConnectionStatus({ key: "CONNECTED" });
                $('.slack-server-integration-account-text').removeClass('hidden');
            }).fail(function(err) {
                $('.slack-server-integration-account-text-error').removeClass('hidden');
                var resp = err && err.responseText && JSON.parse(err.responseText);
                if (resp && resp.key) {
                    updateUserConnectionStatus(resp);
                } else {
                    updateUserConnectionStatus({ key: "UNKNOWN" });
                }
            }).always(function() {
                userConnectionStatus.spinStop();
            });
        }
    });
});
