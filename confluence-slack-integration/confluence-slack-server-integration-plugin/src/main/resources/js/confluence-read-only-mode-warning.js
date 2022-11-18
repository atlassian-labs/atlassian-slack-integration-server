require([
    'jquery'
], function(
    $
) {
    $(function() {
        var accessMode = AJS.Meta.get('access-mode');
        var isReadOnlyEnabled = accessMode === 'READ_ONLY';
        if (isReadOnlyEnabled) {
            AJS.messages.warning("#read-only-mode-warning", {
                // title: 'This is a title in a default message.',
                body: '<p>Changes to Slack channel configuration while in read-only mode could be lost ' +
                    'as a result of database maintenance that is in progress.</p>'
            });
        }
    })
});
