define("slack/feature/issuepanel/dedicatedchannel-view",
[
    "jira/util/formatter",
    "aui/inline-dialog",
    "aui/dialog",
    "aui/message",
    "jquery",
    "slack/backbone",
    "slack/base",
    "slack/feature/issuepanel/dedicatedchannel",
    "slack/widget/channelselector/channelmapping-service",
    "slack/widget/channelselector/channelselector-view"
], function (
        formatter,
        InlineDialog,
        Dialog,
        messages,
        $,
        Backbone,
        Slack,
        DedicatedChannel,
        ChannelMappingService,
        ChannelSelectorView
) {
    var DedicatedChannelView = Slack.View.extend({

        events: {
            "click a.trigger-dialog-select-channel": "openSelectChannelDialog",
            "click .trash-dedicated-channel": "unassignDedicatedChannel"
        },

        initialize: function (options) {
            this.loggedIn = options.loggedIn;
            this.canAssignChannel = options.canAssignChannel;
            this.isAdmin = options.isAdmin;
            this.issueKey = options.issueKey;
        },

        close: function () {
            if (this.dialogContentReadycallback) {
                $(document).unbind("dialogContentReady", this.dialogContentReadycallback);
            }
            this.unbind();
        },

        createOrAssignDedicatedChannel: function (teamId, channelId) {
            var self = this;
            var issueKey = self.issueKey;
            var dfd = $.Deferred();

            var $errorsContainer = self.$("#slack-dedicated-channel-errors");
            self.clearError($errorsContainer);
            var $spinner = self.$('.slack-button-spinner');
            $spinner.css({display: "inline-block"});
            $spinner.spin();

            self.model.save({issueKey: issueKey, channelId: channelId, teamId: teamId})
                    .done(function (dedicatedChannel) {
                var html = JIRA.Templates.Slack.Project.IssuePanel.dedicatedChannelDetails({
                    loggedIn: self.loggedIn,
                    dedicatedChannel: dedicatedChannel,
                    created: true
                });

                self.$(".slack-dedicated-channel-content")
                        .hide()
                        .html(html)
                        .fadeIn({
                            complete: function () {
                                setTimeout(function () {
                                    self.$(".slack-dedicated-channel-icon").removeClass("success");
                                }, 1500);
                            }
                        });

                var $panelContainer = $("#slack-issue-panel-channels-container");
                $panelContainer.data("dedicated-team-id", dedicatedChannel.teamId);
                $panelContainer.data("dedicated-channel-id", dedicatedChannel.channelId);

                JIRA.trace("slack.dedicated.channel.created");
                // self.initInviteContributorsView(self.issueKey);
                dfd.resolve(dedicatedChannel);
            }).fail(function (err) {
                if (self.loggedIn && (err.status === 403 || err.status === 401)) {
                    var dialog = new Dialog({
                        width: 500,
                        height: 300,
                        id: "oauth-failure",
                        closeOnOutsideClick: true
                    });
                    dialog.addHeader("Authentication Failure");
                    dialog.addPanel("panel 0", JIRA.Templates.Slack.Project.IssuePanel.oauthNoLongerValidMessageDedicatedChannel({}));
                    dialog.show();
                } else {
                    self.displayError($errorsContainer,
                            channelId === undefined ?
                                    formatter.I18n.getText("jira.plugins.slack.viewissue.panel.dedicated.channel.create.error") :
                                    formatter.I18n.getText("jira.plugins.slack.viewissue.panel.dedicated.channel.assign.error"), err);
                }
                dfd.reject(err);
            }).always(function () {
                $spinner.spinStop();
                $spinner.css({display: "none"});
            });

            return dfd;
        },

        openSelectChannelDialog: function (e) {
            e.preventDefault();
            var target = e.currentTarget;
            if (this.dialog === undefined) {
                this.dialog = new JIRA.FormDialog({
                    id: target.id + "-dialog",
                    ajaxOptions: {
                        url: target.href,
                        data: {
                            decorator: "dialog",
                            inline: "false"
                        }
                    },
                    width: 400
                });
                this.initSelectChannelDialog(this.dialog);
            }

            this.dialog.show();
        },

        initSelectChannelDialog: function (dialog) {
            var self = this;
            self.dialogContentReadycallback = function (e, data) {
                $(".create-tip-icon").tooltip();

                if (data.options.id === "slack-select-dedicated-channel-dialog") {
                    if (!self.loggedIn) {
                        return;
                    }
                    var teamSelector = $('#slack-dedicated-team-select');

                    var channelSelector = new ChannelSelectorView({
                        el: $('#slack-dedicated-channel-select'),
                        loggedIn: self.loggedIn,
                        teamProvider: function() {
                            return teamSelector.val();
                        },
                        maxWidth: "480px",
                        suggestionProvider: function() {
                            return 'issue ' + self.issueKey;
                        }
                    });
                    teamSelector.on('change', function() {
                        channelSelector.clearSelectedChannel();
                        channelSelector.disable();
                        setTimeout(function() {
                            if (teamSelector.val()) {
                                channelSelector.enable();
                            }
                        }, 200);
                    });
                    var channelSelectorWatcher = {
                        channelSelector: channelSelector,
                        clearSelectedChannel: function () {
                            this.channelSelector.clearSelectedChannel();
                            this.disableSubmitButton();
                        },
                        enableSubmitButton: function () {
                            var selectedChannel = this.channelSelector.getSelectedChannel();
                            if (selectedChannel && selectedChannel.id !== "") {
                                $("#slack-select-dedicated-channel-dialog-submit").removeAttr('aria-disabled').removeAttr('disabled');
                            }
                        },
                        disableSubmitButton: function () {
                            $('#slack-select-dedicated-channel-dialog-submit').attr({
                                'aria-disabled': 'true',
                                'disabled': ''
                            });
                        },
                        channelSelected: function () {
                            return $('#slack-select-dedicated-channel-dialog-submit').attr("disabled") === undefined;
                        },
                        init: function () {
                            this.channelSelector.on("change", this.enableSubmitButton, this);
                            this.channelSelector.on("clear", this.disableSubmitButton, this);
                        }
                    };
                    channelSelectorWatcher.init();

                    var submitSelectChannelDialog = function () {
                        dialog.showFooterLoadingIndicator();

                        var dfd = $.Deferred();
                        var selectedChannel = channelSelector.getSelectedChannel();
                        if (!selectedChannel.existing) {
                            // create channel first
                            var channelServicePromise = ChannelMappingService.channelServicePromise(teamSelector.val(), self.loggedIn);
                            channelServicePromise.done(function (channelService) {
                                dfd = channelService.createChannel(selectedChannel.channelName);
                                dfd.fail(function(createChannelError) {
                                   dfd.reject(createChannelError);
                                });
                            }).fail(function (err) {
                                dfd.reject(err);
                            });
                        } else {
                            dfd.resolve(selectedChannel);
                        }

                        dfd.done(function (channel) {
                            $('#slack-select-dedicated-channel-dialog-close').click();

                            // The object returned by channelSelector has id, while the one returned by createChannel has channelId.
                            var channelId = channel.id || channel.channelId;
                            return self.createOrAssignDedicatedChannel(teamSelector.val(), channelId);
                        }).fail(function (err) {
                            var $errorContainer = $("#slack-select-dedicated-channel-dialog").find(".dialog-errors");
                            var text = formatter.I18n.getText("jira.plugins.slack.viewissue.panel.dedicated.channel.assign.error");
                            self.displayError($errorContainer, text, err);
                        }).always(function () {
                            dialog.hideFooterLoadingIndicator();
                        });
                    };

                    var $form = $("#slack-select-dedicated-channel-dialog").find("form");
                    $form.on("before-submit", function (e) {
                        e.preventDefault();

                        if (channelSelectorWatcher.channelSelected()) {
                            submitSelectChannelDialog();
                        }
                    });

                    channelSelectorWatcher.disableSubmitButton();

                    $('#slack-select-dedicated-channel-dialog-submit').on("click", submitSelectChannelDialog);
                }
            };
            $(document).bind("dialogContentReady", {}, self.dialogContentReadycallback);
        },

        clearError: function ($errorsContainer) {
            $errorsContainer.hide();
            $errorsContainer.empty();
        },

        displayError: function ($errorsContainer, title, err) {
            $errorsContainer.show();
            $errorsContainer.empty();

            var msg;
            if (err.status === 503) {
                msg = formatter.I18n.getText("jira.plugins.slack.viewissue.panel.unable.to.connect");
            } else {
                msg = title;
            }

            $errorsContainer.append(msg);
        },

        unassignDedicatedChannel: function (e) {
            e.preventDefault();

            var self = this;

            if (self.loggedIn) {
                var dfd = $.Deferred();

                var $errorsContainer = self.$("#slack-dedicated-channel-errors");
                self.clearError($errorsContainer);
                self.showSpinner();

                self.$(".trash-dedicated-channel").hide();
                self.model.destroy().always(function () {
                    self.hideSpinner();
                    var $unlinkButton = self.$(".trash-dedicated-channel");
                    $unlinkButton.data("hide-for-progress", false);
                }).done(function () {
                    self.model = new DedicatedChannel({});

                    var html = JIRA.Templates.Slack.Project.IssuePanel.dedicatedChannelForm({
                        canAssignChannel: self.canAssignChannel,
                        isAdmin: self.isAdmin,
                        issueKey: self.issueKey
                    });

                    self.$(".slack-dedicated-channel-content").html(html);
                    messages.success('#dedicated-channel-message-bar', {
                        title: formatter.I18n.getText('admin.common.words.success'),
                        body: formatter.I18n.getText("jira.plugins.slack.viewissue.panel.dedicated.channel.delete.success.message"),
                        fadeout: true
                    });

                    JIRA.trace("slack.dedicated.channel.deleted");
                    dfd.resolve();
                }).fail(function (err) {
                    self.displayError($errorsContainer, formatter.I18n.getText("jira.plugins.slack.viewissue.panel.dedicated.channel.unassign.error"), err);
                    dfd.reject(err);
                });

                return dfd;
            }

        },

        dialogConfig: function() {
            return {
                noBind: true,
                hideDelay: null
            };
        },

        showSpinner: function() {
            var $spinner = this.$('.slack-button-spinner');
            if ($spinner) {
                $spinner.css({display: "inline-block"});
                $spinner.spin();
            }
        },

        hideSpinner: function() {
            var $spinner = this.$('.slack-button-spinner');
            if ($spinner) {
                $spinner.spinStop();
                $spinner.css({display: "none"});
            }
        }

        });

    return DedicatedChannelView;
});
