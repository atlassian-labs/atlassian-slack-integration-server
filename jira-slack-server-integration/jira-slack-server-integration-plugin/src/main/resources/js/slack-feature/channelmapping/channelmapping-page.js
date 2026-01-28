define("slack/feature/channelmapping/channelmapping-page",
[
    "jquery",
    "slack/backbone",
    "underscore",
    "slack/feature/channelmapping/mapping",
    "slack/feature/channelmapping/mapping-view",
    "slack/widget/channelselector/channelmapping-service",
    "slack/widget/channelselector/channelselector-view",
    'jira/flag'
], function (
    $,
    Backbone,
    _,
    Mapping,
    MappingView,
    ChannelMappingService,
    ChannelSelectorView,
    jiraFlag
) {
    function extractProjectNameAndKey(projectNameAndKey, projectId) {
        var key = "";
        var name = "";

        var match = /^(.*?)(?:\s\((\w+)\))?$/.exec(projectNameAndKey);

        if (match !== null) {
            key = match[2];
            name = match[1];
        }

        return {
            projectKey: key,
            projectId: projectId,
            projectName: name
        };
    }

    function getProjectKey() {
        return $("[name='projectKey']").attr("content");
    }

    var ChannelMappingPageView = Backbone.View.extend({

        events: {
            "click #slack-project-to-channel-add": "add",
            "submit .slack-channel-mapping-form": "submitForm"
        },

        initialize: function() {
            var configuration = $('#slack-channel-configuration');
            var teamId = configuration.data('slack-team-id');
            this.loggedIn = !!this.$el.data("slack-user-name");

            this.channelServicePromise = ChannelMappingService.channelServicePromise(teamId, this.loggedIn, getProjectKey());

            var self = this;
            this.channelSelector = new ChannelSelectorView({
                el: this.$('#slack-project-to-channel-add-select'),
                loggedIn: this.loggedIn,
                teamProvider: function() {
                    return teamId;
                },
                maxWidth: "200px",
                suggestionProvider: function() {
                    var projectName = self.getProjectNameFromField() || '';
                    var parentheses = projectName.indexOf('(');
                    if (parentheses > 0) {
                        projectName = projectName.substring(0, parentheses).trim();
                    }
                    return projectName;
                }
            });

            this.channelSelector.on("change", this.enableAddButton, this);
            this.channelSelector.on("clear", this.disableAddButton, this);

            this.channelServicePromise.done(function(channelService) {
                channelService.onChange(function() {
                    self.channelSelector.clearSelectedChannel();
                    self.disableAddButton();
                });
            });

            this.collection.on("add", this.addMapping, this);
            this.collection.on("remove", this.removeMapping, this);
            this.collection.on("change:edit", this.editMapping, this);
        },

        getProjectNameFromField: function() {
            return this.$("#project-field").val();
        },

        getProjectIdFromField: function() {
            var values = this.$("#project").val();
            if (_.isArray(values)) {
                return values[0];
            }

            if (_.isEmpty(values)) {
                return this.$el.data("project-id");
            }

            return values;
        },

        enableAddButton: function() {
            var selectedChannel = this.channelSelector.getSelectedChannel();
            var selectedProject = this.getProjectNameFromField();
            if (selectedChannel.id !== "" && selectedProject !== "") {
                this.$("#slack-project-to-channel-add").removeAttr('aria-disabled').prop('disabled', false);
            }
        },

        disableAddButton: function() {
            this.$('#slack-project-to-channel-add').attr({
                'aria-disabled': 'true',
                'disabled': ''
            });
        },

        submitForm: function(e) {
            e.preventDefault();
            if (this.$('#slack-project-to-channel-add').attr("disabled") === undefined) {
                this.add(e);
            }
        },

        add: function(e) {
            e.preventDefault();
            var selectedChannel = this.channelSelector.getSelectedChannel();
            var self = this;

            var spinner = this.$(".slack-channel-mapping-form .loading").spin();
            this.channelServicePromise.done(function(channelService) {
                if (!selectedChannel.existing) {
                    var channelName = selectedChannel.channelName;
                    channelService.createChannel(channelName).done(function(channel) {
                        self.addChannel(channel);
                    }).always(function() {
                        spinner.spinStop();
                    }).fail(function(resp) {
                        var html = JIRA.Templates.Slack.Project.ChannelMapping.channelCreateError({
                            triedName: channelName,
                            errorMessage: resp.responseText || ''
                        });
                        jiraFlag.showErrorMsg(null, html);
                    });
                } else {
                    var channel = channelService.getChannelById(selectedChannel.id);
                    if(channel) {
                        self.$(".slack-channel-mapping-list").next().remove();

                        self.addChannel(channel).fail(function() {
                            self.$(".slack-channel-mapping-list").after($(JIRA.Templates.Slack.Project.ChannelMapping.errors({})));
                        }).always(function() {
                            spinner.spinStop();
                        });
                    }
                }
            });
        },

        addChannel: function(channel) {
            var deferred = $.Deferred();
            if (channel) {
                var project = extractProjectNameAndKey(this.getProjectNameFromField(), this.getProjectIdFromField());
                var configuration = $('#slack-channel-configuration');
                var teamId = configuration.data('slack-team-id');
                var teamName = configuration.data('slack-team-name');

                this.collection.create({
                    teamId: teamId,
                    teamName: teamName,
                    channelId: channel.channelId,
                    channelName: channel.channelName,
                    projectName: project.projectName,
                    projectKey: project.projectKey,
                    projectId: project.projectId,
                    edit: true,
                    config: {
                        "MATCHER:ISSUE_CREATED": true
                    }
                }, {
                    wait: true,
                    success: function(resp) {
                        deferred.resolve(resp);
                    },
                    error: function(err) {
                        deferred.reject(err);
                    }
                });
            } else {
                deferred.reject();
            }

            return deferred;
        },

        addMapping: function(mapping) {
            var mappingView = new MappingView({
                model: mapping
            });

            var $mapping = mappingView.render().$el;

            var mappingIndex = this.collection.indexOf(mapping);
            if (mappingIndex > 0) {
                var previousMapping = this.collection.at(mappingIndex - 1);
                var previousMappingView = this.$("[data-configuration-group-id='" + previousMapping.get("configurationGroupId") + "']");
                previousMappingView.after($mapping);
            } else if (mappingIndex === 0) {
                this.$(".slack-channel-mapping-list").prepend($mapping);
            } else {
                this.$(".slack-channel-mapping-list").append($mapping);
            }

            this.$(".slack-integration-steps").trigger('mapping-added.integration-steps');

            this.channelSelector.clearSelectedChannel();
            this.disableAddButton();

            this.editMapping(mapping);
        },

        removeMapping: function(mapping) {
        },

        editMapping: function(mapping) {
            if (mapping.get("edit")) {
                this.collection.each(function(model) {
                    if (model !== mapping) {
                        model.set("edit", false);
                    }
                });
            }
        }
    });


    return ChannelMappingPageView;
});
