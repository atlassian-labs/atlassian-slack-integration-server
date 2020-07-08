require([
    'jquery',
    'slack/js-cookies',
    'wrm/context-path'
], function (
    $,
    jsCookies,
    wrmContextPath
) {

    var SLACK_NATIVE_LINK_ENABLED = "SLACK_NATIVE_LINK_ENABLED";

    function getTeamUrl(teamId) {
        return $.ajax({
            url: wrmContextPath() + "/rest/slack/latest/connection/" + teamId + "/info",
            type: "GET"
        });
    }

    function getMessageLink(teamId, channelId, messageTimestamp) {
        return $.ajax({
            url: wrmContextPath() + "/rest/slack/latest/channels/" + teamId + "/" + channelId + "/" + messageTimestamp,
            type: "GET"
        });
    }

    function isSlackNativeLinkEnabled() {
        return jsCookies.get(SLACK_NATIVE_LINK_ENABLED) === "true";
    }

    function sendAnalyticEvent() {
        var linkType = isSlackNativeLinkEnabled() ? "inapp" : "web";
        var nativeEnabled = navigator.platform.toUpperCase().indexOf('MAC') >= 0 ? "native.enabled" : "native.disabled";
        var metric  = 'notifications.slack.link.clicked.' + linkType  + '.' + nativeEnabled;
        AJS.trigger('analyticsEvent', {name: metric});
    }

    function findTheRightElement(element, tagName) {
        var levels = 2;
        while (levels > 0 && element.prop("tagName").toLowerCase() !== tagName.toLowerCase()) {
            element = element.parent();
            levels--;
        }
        return element;
    }

    $(document).on("click", ".slack-team-link-url", function (event) {
        event.preventDefault();

        var target = findTheRightElement($(event.target), "A");
        var teamId = target.data("team-id");

        if (isSlackNativeLinkEnabled()) {
            window.location = "slack://open?team=" + teamId;
        } else {
            getTeamUrl(teamId)
                .done(function(data) {
                    window.open(data.teamUrl, "slack");
                })
                .fail(function() {
                    window.location = "slack://open?team=" + teamId;
                });
        }

        sendAnalyticEvent();
    });

    $(document).on("click", ".slack-channel-link-url", function (event) {
        event.preventDefault();

        var target = findTheRightElement($(event.target), "A");
        var teamId = target.data("team-id");
        var channelId = target.data("channel-id");

        if (isSlackNativeLinkEnabled()) {
            window.location = "slack://channel?team=" + teamId + "&id=" + channelId;
        } else {
            window.open("https://slack.com/app_redirect?team=" + teamId + "&channel=" + channelId, "slack");
        }

        sendAnalyticEvent();
    });

    var openSlackUserUrl = function (event) {
        event.preventDefault();

        var target = findTheRightElement($(event.target), "A");
        var teamId = target.data("team-id");
        var userId = target.data("user-id");

        if (isSlackNativeLinkEnabled()) {
            window.location = "slack://user?team=" + teamId + "&id=" + userId;
        } else {
            window.open("https://slack.com/app_redirect?team=" + teamId + "&channel=" + userId, "slack");
        }

        sendAnalyticEvent();
    };
    $(document).on("click", ".slack-user-link-url", openSlackUserUrl);

    $(document).on("click", ".slack-message-link-url", function (event) {
        event.preventDefault();

        var target = findTheRightElement($(event.target), "A");
        var teamId = target.data("team-id");
        var channelId = target.data("channel-id");
        var messageTs = target.attr("data-message-ts");

        getMessageLink(teamId, channelId, messageTs)
            .done(function(data) {
                window.open(data.link, "slack");
            })
            .fail(function() {
                var jiraFlag = require('jira/flag');
                jiraFlag.showWarningMsg(
                    AJS.I18n.getText("plugins.slack.message.link.not.found"),
                    AJS.I18n.getText("plugins.slack.message.link.not.found.details"));
            });

        sendAnalyticEvent();
    });

    // exports
    window.Slack = window.Slack || {};
    window.Slack.SlackLinks = window.Slack.SlackLinks || {};
    window.Slack.SlackLinks.openSlackUserUrl = openSlackUserUrl;

});
