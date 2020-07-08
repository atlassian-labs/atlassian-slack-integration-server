define("slack/widget/criteria/criteria",
[
    "jquery",
    "backbone",
    "jira/components/query/basic/searchercollection",
    "jira/components/query/querystatemodel",
    "jira/components/query/basic/criteriamodel",
    "jira/components/query/basic/criteriaview"
], function (
    $,
    Backbone,
    JIRAIssuesSearcherCollection,
    JIRAIssuesStateModel,
    JIRAIssuesCriteriaModel,
    JIRAIssuesCriteriaView
) {

    function Criteria(options) {

        var jql = options.jql || "";
        var fieldId = options.fieldId;
        var $el = options.$el;
        var active = options.active || false;
        var values = fromCommaSeparatedList(options.serializedParams);
        var regex = new RegExp(fieldId + "=", "g");
        var onChanged = options.onChanged;
        var collectionChangedCallback;

        var searcherCollection = new JIRAIssuesSearcherCollection([], {
            fixedLozenges: [fieldId],
            queryStateModel: new JIRAIssuesStateModel({
                jql: jql,
                without: [],
                basicAutoUpdate: true
            })
        });

        function tweakView(view) {

            var $selector = view.$(".criteria-selector");
            if ($selector !== undefined) {
                $selector.removeClass("aui-button-subtle");
            }
        }

        function changeId(fieldId) {
            // These shenanigans are necessary to deal with Jira.Issues.SearcherDialog
            // Jira.Issues.SearcherDialog assumes that data-id is unique on a page.
            $el.find(".criteria-selector").attr("data-id", fieldId ? fieldId : "");
        }

        function toCommaSeparatedList(value) {
            if (value !== undefined && value !== null) {
                return String(value).replace(regex, "").replace(/&/g, ",");
            }
        }

        function fromCommaSeparatedList(list) {
            if (list !== undefined && list !== null) {
                return String(list).replace(/^/, fieldId + "=").replace(/,/g, "&" + fieldId + "=");
            }
        }

        this.createDeferred = function() {
            var deferred = new $.Deferred();
            var self = this;
            searcherCollection.restoreFromQuery(jql, true).always(function () {

                var criteria = new JIRAIssuesCriteriaModel({
                    id: fieldId
                });
                var criteriaView = new JIRAIssuesCriteriaView({
                    el: $el,
                    model: criteria,
                    searcherCollection: searcherCollection
                });

                var searcher = searcherCollection.get(fieldId);
                searcher.setSerializedParams(values);
                searcher.createOrUpdateClauseWithQueryString().done(function () {
                    collectionChangedCallback = function () {
                        if (self.onChanged) {
                            var value = searcherCollection.get(fieldId).get("serializedParams");
                            self.onChanged(toCommaSeparatedList(value));
                            values = value;
                        }
                    };
                    searcherCollection.on("collectionChanged", collectionChangedCallback, this);


                    criteriaView.render();

                    tweakView(criteriaView);

                    if (!active) {
                        changeId();
                    }

                    deferred.resolve();
                });

            }, this);
            this.searcherCollection = searcherCollection;

            return deferred;
        };


        this.getValue = function() {
            var value = this.searcherCollection.get(fieldId).get("serializedParams");
            return toCommaSeparatedList(value);
        };

        return {
            switchView: function() {
                active = true;
                changeId(fieldId);
            },

            disableView: function() {
                active = false;
                searcherCollection.off("collectionChanged", collectionChangedCallback);
                changeId();
            },

            getValue: this.getValue,

            createDeferred: this.createDeferred
        };
    }


    return Criteria;
});
