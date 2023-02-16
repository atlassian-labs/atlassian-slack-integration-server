define("slack/feature/issuepanel/issuemention-view",
[
    "jquery",
    "slack/backbone",
    "slack/base",
    "jira/flag"
], function (
    $,
    Backbone,
    Slack,
    jiraFlag
) {
    var IssueMentionView = Slack.View.extend({

        events: {
            "click a.trigger-dialog-large": "openMentionDialog"
        },

        initialize: function(options) {
            this.loggedIn = options.loggedIn;
            this.isAdmin = options.isAdmin;
            this.issueKey = options.issueKey;

            this.model.on("sync", this.updateMentionCount, this);
            this.model.on("sync", this.ready, this);
            this.model.on("error", this.displayError, this);

            if (this.loggedIn) {
                this.model.fetch();
            }
        },

        close: function() {
            if (this.dialogReady) {
                $(document).unbind("dialogContentReady", this.dialogReady);
            }
            this.unbind();
        },

        openMentionDialog: function(e) {
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
                    width: 900
                });

                this.dialogReady = function() {
                    $('.slack-mentions-dialog-content .slack-message-content').each(function() {
                        var content = $(this);
                        try {
                            var html = slackdown
                                .parse(content.text(), content.data('team-id'))
                                .replace(/[\n]/g, "<br/>");
                            content.html(html);
                        } catch (e) {
                            //nothing
                        }
                    });

                    $('.slack-mentions-dialog-content .delete-issue-mention').on('click', function() {
                        e.preventDefault();
                        var link = $(this);
                        var channelId = link.data('channel-id');
                        var messageTimestamp = link.attr('data-message-ts');

                        if (channelId && messageTimestamp) {
                            $.ajax({
                                type: "DELETE",
                                url: AJS.contextPath() + '/slack/issue-mentions/' + channelId + '/' + messageTimestamp,
                                dataType: 'json'
                            }).done(function() {
                                link.closest("tr").remove();
                            }).fail(function() {
                                jiraFlag.showErrorMsg(null, AJS.I18n.getText("jira.plugins.slack.viewissue.panel.issue.mention.delete.error"));
                            });
                        }
                    });
                },
                $(document).bind("dialogContentReady", {}, this.dialogReady);
            }

            this.dialog.show();
        },

        updateMentionCount: function() {
            var html = JIRA.Templates.Slack.Project.IssuePanel.mentionedChannelsDescription({
                mentionCount: this.model.get("mentionCount"),
                channelCount: this.model.get("channelCount"),
                issueKey: this.issueKey,
                userLoggedIntoSlack: this.loggedIn
            });

            this.$(".slack-mentioned-channels-content").html(html);

            JIRA.trace("slack.mentioned.channels.fetched");
        },

        ready: function() {
            this.trigger("ready");
        },

        displayError: function(model, resp, options) {
            var errorsContainer = this.$(".slack-mentioned-channels-content");
            errorsContainer.empty();
            errorsContainer.append("<span class='errors'>" + AJS.I18n.getText("jira.plugins.slack.viewissue.panel.mentions.channels.error") + "</span>");
        }
    });

    return IssueMentionView;
});
