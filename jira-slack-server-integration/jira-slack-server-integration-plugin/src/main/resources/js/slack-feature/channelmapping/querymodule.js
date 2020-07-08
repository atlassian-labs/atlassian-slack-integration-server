define("slack/feature/channelmapping/querymodule",
[
    "jquery",
    "jira/components/query"
], function (
    $,
    QueryComponent
) {

    var queryModule;

    function init(options) {
        if (!options) {
            options = {
                jql: "issuetype in standardIssueTypes()",
                primaryClauses: [{
                    id: "issuetype",
                    name: "Issue"
                }, {
                    id: "priority",
                    name: "Priority"
                }],
                without: ["project", "status"],
                style: "slack",
                layoutSwitcher: false
            }
        }

        queryModule = QueryComponent.create(options);

        queryModule.onJqlChanged(disableSwitcherIfJQLTooComplex, this);

        var jqlChangedHandler;
        var jqlChangedHandlerObj;
        queryModule.onJqlChanged(function(jql) {
            validateJql(jql).done(function() {
                triggerJqlChanged(jql);
            }).fail(function(data) {
                var errors;
                try {
                    errors = JSON.parse(data.responseText).errorMessages;
                } catch (err) {
                    errors = [AJS.I18n.getText("issues.components.query.invalid.jql.error")];
                }

                showErrors(errors);
            });
        }, this);

        $(".criteria-selector").removeClass("aui-button-subtle");


        $(document).on("focusout", "#advanced-search", function() {
            submitJql($(this));
        });

        $(document).on("focusout", "#searcher-query", function() {
            submitJql($(this));
        });

        $(document).on("keypress keydown keyup", "#advanced-search", function() {
            clearNotifications();
        });
    }


     function showErrors(errors) {
        queryModule._errors[queryModule._queryStateModel.ADVANCED_SEARCH] = errors;
        queryModule.showSearchErrors();
    }

    function validateJql(jql) {
        return $.ajax({
            url: AJS.contextPath() + "/rest/api/2/search",
            data: JSON.stringify({
                jql: jql,
                startAt: 0,
                maxResults: 1,
                fields: ["id"]
            }),
            cache: false,
            type: "POST",
            contentType: "application/json",
            dataType: 'json'
        });
    }

    function submitJql($input) {
        // Simulate enter key
        var e = jQuery.Event("keypress");
        e.which = 13;
        e.keyCode = 13;
        $input.trigger(e);
    }

    function clearNotifications() {
        queryModule._queryView.clearNotifications();
    }

    function fixAdvancedSearchHeight($el) {
        $el.find("#advanced-search").height("33").expandOnInput();
    }

    function triggerJqlChanged(jql) {
        if (jqlChangedHandler !== undefined) {
            jqlChangedHandler.call(jqlChangedHandlerObj, jql);
        }
    }

    var validFieldNames = {
        "issuetype": true,
        "type": true,
        "priority": true
    };


    var isJQLTooComplexFn = function isJQLTooComplex(jql) {
        if (_.isEmpty(jql)) {
            return false;
        }

        var parser = new JIRA.JQLAutoComplete.MyParser();

        var tooComplex = false;
        var splits = jql.split(/\bAND\b|\bOR\b/i);
        splits.forEach(function(split) {
            var result = parser.parse(split).getResult();

            if (!result.getParseError() && !validFieldNames.hasOwnProperty(result.getLastFieldName())) {
                tooComplex = true;
            }
        });


        return tooComplex;
    }

    function setIsJQLTooComplexFn(f) {
        isJQLTooComplexFn = f;
    }


    function disableSwitcherIfJQLTooComplex(jql) {
        if (isJQLTooComplexFn(jql)) {
            var switcherView = queryModule._queryView.switcherViewModel;
            if (switcherView) {
                switcherView.disableSwitching();
            }
        }
    }

    function switchModeBasedOnJQL(jql) {
        if (isJQLTooComplexFn(jql)) {
            queryModule._queryStateModel.switchPreferredSearchMode("advanced");
            var switcherView = queryModule._queryView.switcherViewModel;
            if (switcherView) {
                switcherView.disableSwitching();
            }
        } else {
            queryModule._queryStateModel.switchPreferredSearchMode("basic");
        }
    }

    return {
        init: init,

        setIsJQLTooComplexFn: setIsJQLTooComplexFn,

        setJql: function(jql) {
            switchModeBasedOnJQL(jql);
            return queryModule.resetToQuery(jql);
        },

        switchView: function($el) {
            var $queryViewEl = $("<div></div>").appendTo($el);

            $queryViewEl.addClass("query-component slack-styled");
            queryModule._queryView.remove();
            queryModule.createAndRenderView($queryViewEl);

            disableSwitcherIfJQLTooComplex(queryModule.getJql());
            fixAdvancedSearchHeight($queryViewEl);
            $queryViewEl.find(".criteria-selector").removeClass("aui-button-subtle");
        },

        onJqlChanged: function(handler, obj) {
            jqlChangedHandler = handler;
            jqlChangedHandlerObj = obj;
        }
    };

});
