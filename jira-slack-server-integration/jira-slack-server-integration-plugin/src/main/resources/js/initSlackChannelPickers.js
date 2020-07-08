// this handles post-function configuration page pickers
require([
    'jquery',
    'jira/ajs/select/multi-select',
    "slack/widget/channelselector/channelmapping-service"
], function(
    $,
    MultiSelect,
    ChannelMappingService
) {

    var PRIVATE_CHANNEL_CLASS = "private-channel";
    var PUBLIC_CHANNEL_CLASS = "public-channel";

    function escapeResult(markup) {
        var replace_map = {
            '\\': '&#92;',
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;',
            "/": '&#47;'
        };

        return String(markup).replace(/[&<>"'\/\\]/g, function (match) {
            return replace_map[match];
        });
    }

    function getProjectKey() {
        return $("[name='projectKey']").attr("content");
    }

    function makeSuggestionsHandler(teamProvider, loggedIn) {
        return Class.extend({
            _formatResponse: function(channels, query, team) {
                var groupDescriptors = [];
                if (channels.length > 0) {

                    var suggestionsGroupDescriptor = new AJS.GroupDescriptor({
                        weight: 0,
                        label: AJS.I18n.getText("jira.plugins.slack.channelmapping.channels.all.label")
                    });

                    _.each(channels, function(item) {
                        suggestionsGroupDescriptor.addItem(new AJS.ItemDescriptor({
                            value: team.id + "|" + item.id,
                            label: item.text + "@" + team.name,
                            html: escapeResult(item.text),
                            title: item.text + "@" + team.name,
                            meta: {
                                teamId: team.id,
                                channelId: item.id,
                                channelName: item.channelName || item.text,
                                existing: true
                            },
                            styleClass: item.isPrivate ? PRIVATE_CHANNEL_CLASS : PUBLIC_CHANNEL_CLASS
                        }));
                    });

                    groupDescriptors.push(suggestionsGroupDescriptor);
                }

                return groupDescriptors;
            },

            execute: function(query) {
                var deferred = jQuery.Deferred();
                var self = this;

                const team = teamProvider();
                var channelServicePromise = ChannelMappingService.channelServicePromise(team.id, loggedIn, getProjectKey());
                channelServicePromise.done(function(channelService) {
                    var channels = channelService.getChannels();
                    var searchStr = query.toLocaleLowerCase();
                    var exactMatch = false;
                    var filteredChannels = _.filter(channels, function(item) {
                        var lowerCasedItem = item.text.toLocaleLowerCase();
                        if (lowerCasedItem === searchStr) {
                            exactMatch = true;
                        }

                        return lowerCasedItem.indexOf(searchStr) >= 0;
                    });

                    var suggestions = self._formatResponse(filteredChannels, query, team);

                    JIRA.trace("ajax.request.completed.ChannelSelector");
                    deferred.resolve(suggestions, query);
                }).fail(function(err) {
                    if (loggedIn && (err.status === 403 || err.status === 401)) {
                        var dialog = new AJS.Dialog({
                            width: 600,
                            height: 200,
                            id: "oauth-failure",
                            closeOnOutsideClick: true
                        });
                        dialog.addHeader("Authentication Failure");
                        dialog.addPanel("panel 0", JIRA.Templates.Slack.Project.IssuePanel.oauthNoLongerValidMessage({}));
                        dialog.show();
                        deferred.reject(err);
                    } else {
                        var errorGroupDescriptor = new AJS.GroupDescriptor({
                            weight: 1,
                            footerHtml: JIRA.Templates.Slack.ChannelSelector.errorDropdownFooter({})
                        });
                        errorGroupDescriptor.addItem(new AJS.ItemDescriptor({
                            label: "filler",
                            html: "filler",
                            highlighted: true
                        }));

                        deferred.resolve([errorGroupDescriptor], query);
                    }
                });

                return deferred;
            }
        });
    }

    AJS.toInit(function() {
        var $channelPicker = $("#slackChannelSelect");
        if ($channelPicker.length) {
            new MultiSelect({
                element: $channelPicker,
                itemAttrDisplayed: "label",
                suggestionsHandler: makeSuggestionsHandler(
                    function() {
                        return {
                          id: $("#slack-dedicated-team-select").val(),
                          name: $("#slack-dedicated-team-select :selected").text()
                        } ;
                    },
                    true),
            });
        }
    });
});
