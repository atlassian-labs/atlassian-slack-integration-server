require([
    'jquery',
    'underscore',
    'aui/flag',
    'wrm/data'
], function (
    $,
    _,
    flag,
    wrmData
) {
    var pluginKey = "com.atlassian.confluence.plugins.confluence-slack-server-integration-plugin";
    var providedData = wrmData.claim(pluginKey + ":slack-link-error-resources.slack-link-error");

    $(function() {
        if (providedData && providedData.errorString) {
            flag({
                type: "error",
                title: AJS.I18n.getText("slack.oauth2.error.title"),
                persistent: true,
                body: _.escape(providedData.errorString)
            });
        }
    });
});
