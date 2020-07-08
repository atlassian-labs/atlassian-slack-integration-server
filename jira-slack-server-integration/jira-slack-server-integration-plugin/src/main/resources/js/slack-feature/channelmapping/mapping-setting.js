define("slack/feature/channelmapping/mapping-setting",
[
    "backbone",
    "slack/base"
], function (
    Backbone,
    Slack
) {
    var MappingSetting = Slack.Model.extend({

        idAttribute: "internalId",
        DEBOUNCE_TIME: 800,

        initialize: function(options) {
            this.id = undefined;
            this.addDebounceSave(this.DEBOUNCE_TIME);
        },

        url: function() {
            var projectKey = this.get("projectKey");

            return AJS.contextPath() + "/slack/mapping/" + encodeURI(projectKey);
        },

        parse: function(data) {
            return _.extend(data, {internalId: data.configurationGroupId + "-" + data.name});
        },

        destroy: function() {
            // By default, Backbone doesn't send anything on destroy
            this.id = this.get("configurationGroupId") + "-" + this.get("name");
            var options = {
                data: JSON.stringify(this.toJSON()),
                contentType: 'application/json'
            };

            var self = this;
            return Backbone.Model.prototype.destroy.call(this, options).done(function() {
                self.id = null;
            });
        }

    });

    return MappingSetting;

});
