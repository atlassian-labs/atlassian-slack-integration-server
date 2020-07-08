AJS.toInit(function ($) {

    $(document).on("click", "#dialog-save-button", function (e) {

        e.preventDefault();
        clearErrorPanel();
        var selected = $("#allowAutoConvertCheckbox").attr('checked') === "checked";
        var url = AJS.contextPath() + '/slack/configuration';

        var projectVal = $("#project-key").val();
        var data = {"allowAutoConvert": selected};
        if (projectVal != undefined) {
            data["projectKey"] = projectVal;
        }

        var guestChannelAccess = $("#allowAutoConvertInGuestChannels");
        if(guestChannelAccess.val() != undefined){
            var issuePreviewInGuestChannels = guestChannelAccess.attr('checked') === "checked";
            data['guestChannelEnabled'] = issuePreviewInGuestChannels;
        }

        var hideIssuePanel = $("#hideIssuePanel");
        if(hideIssuePanel.val() != undefined){
            var hideIssuePanelValue = hideIssuePanel.attr('checked') === "checked";
            data['hideIssuePanel'] = hideIssuePanelValue;
        }

        var sendRestrictedCommentsToDedicated = $("#sendRestrictedCommentsToDedicated");
        if(sendRestrictedCommentsToDedicated.length){
            var restrictedCommentsInDedicatedValue = sendRestrictedCommentsToDedicated.attr('checked') === "checked";
            data['sendRestrictedCommentsToDedicated'] = restrictedCommentsInDedicatedValue;
        }

        $.ajax({
            url: url,
            data: JSON.stringify(data),
            contentType: 'application/json',
            cache: false,
            type: "POST"
        }).error(function(error){
            showError(error);
        }).done(function (data) {
            $("#dialog-close-button").click();
        });
    });

    /**
     * We clear the error panel before every transaction
     */
    function clearErrorPanel() {
        var errorPanel = $("#error-panel");
        errorPanel.hide(); // Just in case is visible...
        errorPanel.empty();
    }

    /**
     * We populate the error in the db
     */
    function showError(error) {
        var errorPanel = $("#error-panel");
        errorPanel.append(" Could not complete the action : Status [" + error.status + "] Reason [" + error.statusText + "]");
        errorPanel.show();
    }
});

(function(){

    JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, $ctx) {
        initLargeDialogs($ctx);
    });

    function initLargeDialogs(context) {
        context.find(".trigger-dialog-large").each(function () {
            new JIRA.FormDialog({
                trigger: this,
                id: this.id + "-dialog",
                ajaxOptions: {
                    url: this.href,
                    data: {
                        decorator: "dialog",
                        inline: "true"
                    }
                },
                width: 530
            });
        });
    }

})(jQuery);

