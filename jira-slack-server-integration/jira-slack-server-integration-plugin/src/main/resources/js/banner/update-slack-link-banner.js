require([
    'jquery',
    'jira/flag',
    'jira/util/users/logged-in-user',
    'wrm/context-path',
    'slack/js-cookies'
], function (
    $,
    flag,
    loggedInUser,
    wrmContextPath,
    jsCookies
) {
    var SLACK_ADMIN_BANNER_COOKIE_KEY = "SLACK_ADMIN_BANNER_DISMISS_2";
    AJS.toInit(function () {
        if (!isSlackConfigurationPage() && loggedInUser.isAdmin() && !isAlreadyDismissed()) {
            $.ajax({
                url: wrmContextPath() + '/slack/configuration/status',
                dataType: 'json',
                cache: false
            }).done(function (data, resp) {
                if (resp.status === 200 && data.connectionError) {
                    showWarningMsg(data);
                }
            });
        }

        function showWarningMsg(data) {
            var html = JIRA.Templates.Slack.Banner.updateSlackLink(data);
            flag.showWarningMsg(null, html);
            $(document).on("aui-flag-close", ".aui-flag", dismissPopUp);
        }

        function isSlackConfigurationPage() {
            return window.location.toString().indexOf("/plugins/servlet/slack/configure") > 0;
        }

        function dismissPopUp(e) {
            // The aui flag sends all the close events, that is why we need to filter
            // to the flag that has my container
            if ($(e.target).find(".slack-update-banner").length > 0) {
                jsCookies.set(SLACK_ADMIN_BANNER_COOKIE_KEY, true);
            }
        }

        function isAlreadyDismissed() {
            return jsCookies.get(SLACK_ADMIN_BANNER_COOKIE_KEY) === "true";
        }
    });
});
