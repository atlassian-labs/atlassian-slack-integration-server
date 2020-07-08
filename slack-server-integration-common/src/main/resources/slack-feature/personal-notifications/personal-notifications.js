require([
    'jquery',
    'aui/flag',
    'wrm/context-path'
], function (
    jQuery,
    flag,
    wrmContextPath
) {
    jQuery(function () {
        var contextPath = wrmContextPath();
        var restBaseUrl = contextPath + "/rest/slack/latest";

        jQuery('#slack-content .slack-personal-notification').on('click', function () {
            var input = jQuery(this);
            var checked = input.prop('checked');

            jQuery.ajax(
                restBaseUrl + '/users/notification/' + input.data('key'),
                { type: checked ? 'PUT' : 'DELETE' }
            )
                .done(successFlag)
                .fail(function () {
                    errorFlag();
                    input.prop('checked', !checked)
                });
        });

        jQuery('#pn-workspace-select').on('change', function () {
            var input = jQuery(this);
            var teamId = input.val();

            jQuery.ajax(
                restBaseUrl + '/users/notification?teamId=' + teamId,
                { type: teamId ? 'PUT' : 'DELETE' }
            )
                .done(successFlag)
                .fail(errorFlag);
        });
    });

    function successFlag() {
        flag({
            type: "success",
            title: AJS.I18n.getText('plugins.slack.pn.update.success'),
            close: "auto"
        });
    }

    function errorFlag() {
        flag({
            type: "error",
            title: AJS.I18n.getText('plugins.slack.pn.update.error'),
            close: "auto"
        });
    }
});

