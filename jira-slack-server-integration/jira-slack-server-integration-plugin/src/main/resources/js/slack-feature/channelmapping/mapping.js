define("slack/feature/channelmapping/mapping",
[
    "jquery",
    "backbone",
    "slack/feature/channelmapping/mapping-setting"
], function (
    $,
    Backbone,
    MappingSetting
) {

    function getMappingSettingAttributes(name, model, options) {

        var attributes = _.extend({name: name}, model.toJSON());
        if (options.config !== undefined && options.config[name] !== undefined) {
            attributes.value = options.config[name];
        }

        return attributes;
    }

    var Mapping = Backbone.Model.extend({

        idAttribute: "configurationGroupId",

        initialize: function(options) {
            this.settings = {
                "IssueCreate": new MappingSetting(getMappingSettingAttributes("MATCHER:ISSUE_CREATED", this, options)),
                "IssueTransition": new MappingSetting(getMappingSettingAttributes("MATCHER:ISSUE_TRANSITIONED", this, options)),
                "IssueCommented": new MappingSetting(getMappingSettingAttributes("MATCHER:ISSUE_COMMENTED", this, options)),
                "IssueAssignmentChanged": new MappingSetting(getMappingSettingAttributes("MATCHER:ISSUE_ASSIGNMENT_CHANGED", this, options)),
                "JQL": new MappingSetting(getMappingSettingAttributes("FILTER:JQL_QUERY", this, options)),
                "VERBOSITY": new MappingSetting(getMappingSettingAttributes("VERBOSITY", this, options)),
                "SkipRestrictedComments": new MappingSetting(getMappingSettingAttributes("SKIP_RESTRICTED_COMMENTS", this, options)),
            };
        },

        url: function() {
            var projectKey = this.get("projectKey");

            return AJS.contextPath() + "/slack/mapping/" + encodeURI(projectKey);
        },

        parse: function(data) {
            return _.extend(data, {
                id: data.configurationGroupId,
                projectKey: data.projectKey
            });
        },

        destroy: function() {
            // By default, Backbone doesn't send anything on destroy
            var data = this.toJSON();
            delete data.id; // configurationGroupId is used in the UI to identify the config but is not used as id on the server
            var options = {
                data: JSON.stringify(data),
                contentType: 'application/json'
            };

            Backbone.Model.prototype.destroy.call(this, options);
        },

        getSetting: function(settingName) {
            return this.settings[settingName];
        },

        getSettings: function() {
            return this.settings;
        },

        saveSettings: function(model, response, options) {
            for (var key in this.settings) {
                if (this.settings.hasOwnProperty(key)) {
                    var setting = this.settings[key];
                    setting.set("configurationGroupId", model.get("configurationGroupId"));

                    if (setting.get("value") !== undefined) {
                        setting.save();
                    }
                }
            }
        },

        save: function(attributes, options) {
            options = options ? _.clone(options) : {};
            var success = options.success;
            var self = this;
            options.success = function(model, resp, xhr) {
                if (success) {
                    success(model, resp, options);
                }

                // Save nested settings when saving the model
                self.saveSettings(model, resp, xhr);
            };

            Backbone.Model.prototype.save.call(this, attributes, options);
        }

    });

    return Mapping;

});
