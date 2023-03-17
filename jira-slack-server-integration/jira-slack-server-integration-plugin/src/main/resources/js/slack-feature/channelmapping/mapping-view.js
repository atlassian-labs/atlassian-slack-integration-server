define("slack/feature/channelmapping/mapping-view",
[
    "jquery",
    "slack/backbone",
    "slack/base",
    "slack/feature/channelmapping/querymodule",
    "slack/widget/criteria/criteria",
    "aui/flag",
], function (
    $,
    Backbone,
    Slack,
    queryModule,
    Criteria,
    flag
) {
    queryModule.init();
    var MappingView = Slack.View.extend({

        events: {
            "click .trash-channel-mapping": "clear",
            "click .edit-notification": "edit",
            "click .close-edit-notification": "closeEdit",
            "click .matcher-type": "toggleMatcher",
            "click .slack-notification-config": "toggleMatcher",
            "click .notification-option-radio": "toggleRadioOption",
            "click .notification-option-checkbox": "toggleCheckboxOption",
        },

        initialize: function(options) {

            this.model.on("sync", this.updateAttributes, this);
            this.model.on("destroy", this.remove, this);
            this.model.on("change:edit", this.onEditChanged, this);

            _.each(this.model.getSettings(), function(setting, key) {
                setting.traceAjaxRequests(key);

                setting.on("request", this._hideError, this);
                setting.on("error", this._displayError, this);
                setting.on("waitingToSave", function() {
                    this.$el.attr("data-ajax-request", true);
                }, this);
            }, this);

            _.each(this.$(".matcher-type"), function(checkbox) {
                var $checkbox = $(checkbox);
                var settingName = $checkbox.data("notification-name");
                var setting = this.model.getSetting(settingName);

                setting.on("request", this._hideError, this);
                setting.on("error", this._displayError, this);
                setting.on("waitingToSave", function() {
                    this.$el.attr("data-ajax-request", true);
                }, this);

                if ($checkbox.is(":checked")) {
                    setting.set("checked", true);
                }
            }, this);

            var jqlSetting = this.model.getSetting("JQL");
            jqlSetting.set("value", this.$(".basic-search-container").data("jql"));

            this._renderStatusElement(false);
        },

        _renderStatusElement: function(active) {
            var self = this;
            var $statusEl = this.$(".slack-criteria[data-id=status]");
            if ($statusEl) {
                this.statusCriteria = new Criteria({
                    fieldId: "status",
                    serializedParams: $statusEl.data("value"),
                    $el: $statusEl,
                    active: active
                });
                this.model.getSetting("IssueTransition").set("criteria", this.statusCriteria);
            } else {
                AJS.log("$statusEl not available yet");
            }
        },

        render: function() {
            var html = JIRA.Templates.Slack.Project.ChannelMapping.channelMapping({
                teamId: this.model.get('teamId'),
                teamName: this.model.get('teamName'),
                channelId: this.model.get('channelId'),
                channelName: this.model.get('channelName'),
                projectKey: this.model.get('projectKey'),
                projectName: this.model.get('projectName'),
                configurationGroupId: this.model.get('configurationGroupId'),
                edit: this.model.get('edit'),
                config: this.model.get('config')
            });
            this.$el.replaceWith(html);
            this.setElement(html);

            this._renderStatusElement(true);

            if (this.model.get('edit')) {
                //this.edit();
                this.onEditChanged();
            }

            return this;
        },

        clear: function() {
            this.model.destroy();
        },

        remove: function() {
            this.model.off(null, null, this);
            Backbone.View.prototype.remove.call(this);
        },

        edit: function() {
            this.model.set("edit", true);
        },

        closeEdit: function() {
            this.model.set("edit", false);
        },

        onEditChanged: function() {
            if (this.model.get("edit")) {
                // start edit
                var self = this;

                this.$(".spinner").spin({color: "black"});
                this.$el.addClass("loading");
                var jqlSetting = this.model.getSetting("JQL");
                var $searchContainer = this.$(".basic-search-container");
                var queryModuleDfd = queryModule.setJql(jqlSetting.get("value"));
                $.when(queryModuleDfd, this.statusCriteria.createDeferred()).always(function() {
                    self.statusCriteria.switchView();
                    queryModule.switchView($searchContainer);
                    queryModule.onJqlChanged(self.onJqlChanged, self);

                    self.$el.addClass("edit-active");
                    self.$(".spinner").spinStop();
                    self.$el.removeClass("loading");
                    self.statusCriteria.onChanged = function(value) {
                        self._onCriteriaChanged.call(self, value);
                    };
                    JIRA.trace("ajax.request.completed.ProjectChannelExpanded");
                });
            } else {
                // stop edit
                this.$el.removeClass("edit-active");
                this.statusCriteria.disableView();
                this.statusCriteria.onChanged = null;
                JIRA.trace("ajax.request.completed.ProjectChannelCollapsed");
            }
        },

        toggleMatcher: function(e) {
            var $checkbox = $(e.target);
            var settingName = $checkbox.data("notification-name");

            var setting = this.model.getSetting(settingName);
            if ($checkbox.is(":checked")) {
                setting.set("checked", true);
                setting.save();
            } else {
                setting.set("checked", false);
                setting.destroy();
            }

        },

        toggleRadioOption: function(e) {
            var $option = $(e.target);
            var settingName = $option.data("option-name");

            var setting = this.model.getSetting(settingName);
            setting.set('value', $option.val());
            setting.save();
        },

        toggleCheckboxOption: function(e) {
            var $option = $(e.target);
            var settingName = $option.data("option-name");

            var setting = this.model.getSetting(settingName);
            if ($option.is(":checked")) {
                setting.set('value', $option.val());
                setting.save();
            } else {
                setting.destroy();
            }
        },

        onJqlChanged: function(jql) {
            var jqlSetting = this.model.getSetting("JQL");
            if (jqlSetting.get("value") !== jql) {
                jqlSetting.set("value", jql);
                jqlSetting.save();

                if (jql.indexOf("Customer Request Type") !== -1) {
                    flag({
                        type: 'warning',
                        close: 'manual',
                        body: AJS.I18n.getText('jira.plugins.slack.admin.settings.project.customer.request.type.used')
                    })
                }
            }

        },

        _onCriteriaChanged: function(value) {
            var transitionSetting = this.model.getSetting("IssueTransition");
            if (transitionSetting) {

                transitionSetting.set("value", value);

                if (transitionSetting.get("checked")) {
                    transitionSetting.save();
                }
            }
        },

        _displayError: function(model, response, options) {
            this.$(".errors").html(JIRA.Templates.Slack.Project.ChannelMapping.errors({}));
        },

        _hideError: function() {
            this.$(".errors").html("");
        },

        updateAttributes: function() {
            this.$el.attr("data-configuration-group-id", this.model.get("configurationGroupId"));
        }
    });

    return MappingView;
});
