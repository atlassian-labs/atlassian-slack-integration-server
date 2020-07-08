require([
    'jquery',
    'jira/autocomplete/jql-autocomplete',
    'jira/jql/jql-parser',
    'wrm/context-path'
], function($, JQLAutoComplete, JQLParser, wrmContextPath) {

    function showLoading() {
        $(".slack-jql-loading").show();
    }

    function hideLoading() {
        $(".slack-jql-loading").hide();
    }

    function getJqlAutoCompleteData() {
        return $.ajax({
            url: wrmContextPath() + "/rest/api/2/jql/autocompletedata",
            type: "GET"
        });
    }

    function init(options) {

        showLoading();
        getJqlAutoCompleteData()
            .done(function(autoCompleteData) {
                var fieldID = options.fieldID;
                var errorID = options.errorID;
                var jqlFieldNames = autoCompleteData.visibleFieldNames || [];
                var jqlFunctionNames = autoCompleteData.visibleFunctionNames || [];
                var jqlReservedWords = autoCompleteData.jqlReservedWords || [];

                JQLAutoComplete({
                    fieldID: fieldID,
                    parser: JQLParser(jqlReservedWords),
                    queryDelay: 0.65,
                    jqlFieldNames: jqlFieldNames,
                    jqlFunctionNames: jqlFunctionNames,
                    minQueryLength: 0,
                    allowArrowCarousel: true,
                    autoSelectFirst: false,
                    errorID: errorID
                });
            })
            .always(function() {
                hideLoading();
            });
    }

    AJS.toInit(function() {
        var $input = $("#slack-jql-text");
        if ($input.length) {
            init({
                fieldID: "slack-jql-text",
                errorID: "slack-jql-error"
            });
        }
    });
});
