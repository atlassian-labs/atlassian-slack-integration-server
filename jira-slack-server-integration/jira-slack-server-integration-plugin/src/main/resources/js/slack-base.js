/**
 * Top level configuration/information object.
 */
define("slack/base", [ "underscore", "slack/backbone", "exports" ], function (_, Backbone, exports) {
    if (typeof Slack === "undefined" || !Slack) {
        window.Slack = {};
    }

    /**
     * Provide our own sub classes of the backbone objects for common methods.
     */
    function setupBackbone() {
        exports.View = Backbone.View;

        exports.Model = Backbone.Model.extend({

            saveWrapper: function(saveFunction) {
                this.trigger("waitingToSave");
                saveFunction.apply(this);
                this.trigger("saved");
            },

            addDebounceSave: function(milliseconds) {
                this.save = _.wrap(_.debounce(this.save, milliseconds), this.saveWrapper);
            },

            /**
             * Observes this model for request/sync/error events and logs the beginning and the end.
             * This is mainly used for testing to tell page objects when ajax operations have finished.
             *
             * @param {string} [traceSuffix] suffix to append to the trace key
             */
            traceAjaxRequests: function(traceSuffix) {
                var attribute = null;
                var request = attribute ? "request:" + attribute : "request";
                var sync = attribute ? "sync:" + attribute : "sync";
                var error = attribute ? "error:" + attribute : "error";
                var observed = this;

                observed.on(request, function () {
                    JIRA.trace("ajax.request.started." + traceSuffix);
                }, this);

                observed.on(sync + " " + error, function () {
                    JIRA.trace("ajax.request.completed." + traceSuffix);
                }, this);
            }

        });

        exports.Collection = Backbone.Collection;
        exports.Events = Backbone.Events;
    }

    setupBackbone();
});
