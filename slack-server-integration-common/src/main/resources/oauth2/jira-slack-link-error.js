require([
    'jquery',
    'underscore',
    'jira/flag',
    'wrm/data'
], function (
    $,
    _,
    jiraFlag,
    wrmData
) {
    var pluginKey = "com.atlassian.jira.plugins.jira-slack-server-integration-plugin";
    var providedData = wrmData.claim(pluginKey + ":slack-link-error-resources.slack-link-error");

    $(function() {
        if (providedData && providedData.errorString) {
            jiraFlag.showErrorMsg(AJS.I18n.getText("slack.oauth2.error.title"), _.escape(providedData.errorString));
        }
    });
});
