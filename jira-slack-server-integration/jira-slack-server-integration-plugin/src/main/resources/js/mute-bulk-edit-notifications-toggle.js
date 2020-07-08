require([
    'jquery',
    "wrm/context-path"
], function (
    $,
    wrmContextPath
) {
    $(function() {
        // skip if it's not a page Bulk Operation last page
        var url = window.location.href;
        var isBulkTransitionDetails = url.indexOf('BulkWorkflowTransitionDetailsValidation.jspa') !== -1;
        var isBulkEditDetails = url.indexOf('BulkEditDetails.jspa') !== -1;
        var muteBulkNotificationsInputId = 'slack-mute-bulk-notifications';
        var muteBulkNotificationsLabel = AJS.I18n.getText('jira.plugins.slack.mute.bulk.edit.label');
        var muteBulkNotificationsDescription = AJS.I18n.getText('jira.plugins.slack.mute.bulk.edit.description');
        if (!isBulkTransitionDetails && !isBulkEditDetails) {
            return;
        }

        $.ajax({
            url: wrmContextPath() + '/slack/configuration/bulk-edit-notifications',
            type: 'GET',
            cache: false,
            datatype: 'json'
        }).fail(function(jqXHR, textStatus, error) {
            console.error('Failed to get Slack bulk edit mute notifications toggle value', error);
        }).success(function(data) {
            var checkedByDefault = data.muted ? 'checked' : '';

            $('#bulkedit p:last').before(
                '<div class="checkbox">' +
                '  <input class="checkbox" type="checkbox" id="' + muteBulkNotificationsInputId + '" ' + checkedByDefault + '>' +
                '  <label for="' + muteBulkNotificationsInputId + '">' + muteBulkNotificationsLabel + '</label>' +
                '  <div class="description">' + muteBulkNotificationsDescription + '</div>' +
                '</div>'
            );

            $('#' + muteBulkNotificationsInputId).change(function (event) {
                var isChecked = $(this).is(':checked');
                var muteLabel = isChecked ? 'mute' : 'unmute';
                $.ajax({
                    url: wrmContextPath() + '/slack/configuration/bulk-edit-notifications/' + muteLabel,
                    type: 'POST',
                    cache: false
                }).fail(function(jqXHR, textStatus, error) {
                    console.error('Failed to save Slack bulk edit mute notifications toggle value', error);
                });
            });
        });
    });
});
