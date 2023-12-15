require([
    'jquery',
    'jira/flag'
], function (
    $,
    jiraFlag
) {
    var FLAG_ID = 'slack.token.disconnected.flag';
    var CONFIGURE_PAGE_URL = '/plugins/servlet/slack/configure';
    var OAUTH_SESSIONS_PAGE_URL = '/plugins/servlet/slack/view-oauth-sessions';
    var flag = null;

    $(function () {
        // skip if user is already in configuration view or edit pages or in issue view and slack panel isn't shown
        if (contains(window.location.pathname, CONFIGURE_PAGE_URL)
            || contains(window.location.pathname, OAUTH_SESSIONS_PAGE_URL)
            || (isInIssueView() && !isSlackPanelVisible())) {
            return;
        }

        $(document).on('click', '.slack-dismiss-user-disconnection', dismissUserDisconnection);

        $.ajax({
            url: AJS.contextPath() + '/rest/slack/latest/connection-status/disconnected',
            type: 'GET',
            success: displayWarningIfNeeded
        });

        function displayWarningIfNeeded(data) {
            // title and body
            var title = '';
            var messages = [];
            var showWarning = data.disconnectedSlackUserId || data.isAnyLinkDisconnected;
            if (data.disconnectedSlackUserId) {
                var oauthSessionsPageUrl = AJS.contextPath() + OAUTH_SESSIONS_PAGE_URL;
                messages.push(AJS.I18n.getText('jira.plugins.slack.disconnection.user.notification',
                    oauthSessionsPageUrl, data.disconnectedSlackUserId));
            }

            if (data.isAnyLinkDisconnected) {
                var configurePageUrl = AJS.contextPath() + CONFIGURE_PAGE_URL;
                messages.push(AJS.I18n.getText('jira.plugins.slack.disconnection.admin.notification', configurePageUrl));
            }

            // show notification
            if (showWarning) {
                var body = messages.join('<br>');
                flag = jiraFlag.showWarningMsg(title, body, {
                    dismissalKey: FLAG_ID
                });
            }
        }

        function dismissUserDisconnection() {
            $.ajax({
                url: AJS.contextPath() + '/rest/slack/latest/connection-status/dismiss-user-disconnection',
                type: 'POST',
                data: JSON.stringify({
                    slackUserId: $(this).data('slack-user-id'),
                }),
                contentType: 'application/json',
                success: function() {
                    flag.close();
                }
            });
        }

        /**
         * @returns {boolean} true when using the Slack app in Jira and the content of an issue is shown
         */
        function isInIssueView() {
            return $('#issue-content').length > 0;
        }

        /**
         * @returns {boolean} true when using the Slack app in Jira and the slack issue panel is shown
         */
        function isSlackPanelVisible() {
            return $("#slack-issue-panel").length > 0;
        }

        function contains(str, substr) {
            return str.indexOf(substr) !== -1;
        }
    });
});
