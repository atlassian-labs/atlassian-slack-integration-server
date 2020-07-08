define("slack/widget/channelselector/channelselector-view",
[
    "jquery",
    "backbone",
    "slack/widget/channelselector/channelmapping-service",
    "jira/ajs/select/single-select",
    "jira/ajs/list/group-descriptor",
    "jira/ajs/list/item-descriptor"
], function (
        $,
        Backbone,
        ChannelMappingService,
        SingleSelect,
        GroupDescriptor,
        ItemDescriptor
) {
    var PRIVATE_CHANNEL_CLASS = "private-channel";
    var PUBLIC_CHANNEL_CLASS = "public-channel";
    var SUGGEST_CHANNEL_CLASS = "suggest-create-channel";
    var MAX_RESULT_SUGGESTIONS = 1000;

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

    var optionCache = {};

    function makeSuggestionsHandler(teamProvider, loggedIn, suggestionProvider) {
        return Class.extend({
            _formatResponse: function(channels, query, showCreateChannel, suggestion) {
                var groupDescriptors = [];

                if (!showCreateChannel && !query && suggestion) {
                    var channelHeaderActionsGroupDescriptor = new GroupDescriptor({
                        weight: 0
                    });
                    channelHeaderActionsGroupDescriptor.addItem(new ItemDescriptor({
                        value: suggestion,
                        label: suggestion,
                        html: AJS.I18n.getText("jira.plugins.slack.channelmapping.channels.create.new.suggestion"),
                        title: AJS.I18n.getText("jira.plugins.slack.channelmapping.channels.create.new.suggestion"),
                        meta: {
                            channelId: suggestion,
                            channelName: suggestion,
                            existing: false
                        },
                        highlighted: true,
                        styleClass: SUGGEST_CHANNEL_CLASS
                    }));
                }

                groupDescriptors.push(channelHeaderActionsGroupDescriptor);

                if (channels.length > 0) {

                    var suggestionsGroupDescriptor = new GroupDescriptor({
                        weight: 1,
                        label: loggedIn? AJS.I18n.getText("jira.plugins.slack.channelmapping.channels.all.label") :
                                AJS.I18n.getText("jira.plugins.slack.channelmapping.channels.public.label")
                    });

                    var i, len = channels.length;
                    for (i = 0; i < len; i++) {
                        var item = channels[i];
                        var option = optionCache[item.id];
                        if (!option) {
                            option = new ItemDescriptor({
                                value: item.id,
                                label: item.text,
                                html: escapeResult(item.text),
                                title: item.text,
                                meta: {
                                    channelId: item.id,
                                    channelName: item.channelName || item.text,
                                    existing: true
                                },
                                styleClass: item.isPrivate ? PRIVATE_CHANNEL_CLASS : PUBLIC_CHANNEL_CLASS
                            });
                            optionCache[item.id] = option;
                        }
                        suggestionsGroupDescriptor.addItem(option);
                    }
                    groupDescriptors.push(suggestionsGroupDescriptor);
                }

                if (showCreateChannel) {
                    var createChannelGroupDescriptor = new GroupDescriptor({
                        weight: 2,
                        label: AJS.I18n.getText("jira.plugins.slack.channelmapping.channels.create.label")
                    });

                    var name = query.toLocaleLowerCase();
                    createChannelGroupDescriptor.addItem(new ItemDescriptor({
                        value: name,
                        label: name,
                        html: escapeResult(name) + "<span class='aui-lozenge aui-lozenge-subtle aui-lozenge-complete'>new</span>",
                        title: name,
                        meta: {
                            channelId: name,
                            channelName: name,
                            existing: false
                        },
                        highlighted: true,
                        styleClass: PUBLIC_CHANNEL_CLASS
                    }));

                    groupDescriptors.push(createChannelGroupDescriptor);
                }

                return groupDescriptors;
            },

            execute: function(query) {
                var deferred = jQuery.Deferred();
                var self = this;

                var teamId = teamProvider();
                var channelServicePromise = ChannelMappingService.channelServicePromise(teamId, loggedIn, getProjectKey());
                channelServicePromise.done(function(channelService) {
                    var channels = channelService.getChannels();
                    var searchStr = query.toLocaleLowerCase();
                    var exactMatch = false;

                    // create new channel suggestion
                    var originalChannelNameSuggestion = (suggestionProvider() || '')
                        .replace(/\s/g,'-')
                        .toLocaleLowerCase();
                    var channelNameIncrementer = 1;
                    var channelNameSuggestion = originalChannelNameSuggestion;

                    // filter according to dropdown text and also adjusts channel name suggestion
                    var filteredChannels = new Array(channels.length);
                    var i, p, len = channels.length;
                    for (i = 0, p = 0; i < len; i++) {
                        var item = channels[i];
                        if (item.text === searchStr) {
                            exactMatch = true;
                        }

                        if (!searchStr || item.text.indexOf(searchStr) >= 0) {
                            filteredChannels[p++] = item;
                        }

                        // adds suffix to new channel name suggestion in case name already is taken
                        if (channelNameSuggestion && channelNameSuggestion === item.text) {
                            channelNameSuggestion = originalChannelNameSuggestion + '-' + channelNameIncrementer++;
                        }
                    }
                    filteredChannels.length = p;

                    var showCreateChannel = loggedIn && (!exactMatch && searchStr.length > 0);
                    var suggestions = self._formatResponse(filteredChannels, query, showCreateChannel, channelNameSuggestion);

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
                        var errorGroupDescriptor = GroupDescriptor({
                            weight: 1,
                            footerHtml: JIRA.Templates.Slack.ChannelSelector.errorDropdownFooter({})
                        });
                        errorGroupDescriptor.addItem(new ItemDescriptor({
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

    function initChannelSelector($el, teamProvider, loggedIn, maxWidth, suggestionProvider) {
        return new SingleSelect({
            element: $el,
            width: maxWidth,
            suggestionsHandler: makeSuggestionsHandler(teamProvider, loggedIn, suggestionProvider),
            submitInputVal: true,
            maxInlineResultsDisplayed: MAX_RESULT_SUGGESTIONS,
            matchingStrategy: "(.*)()({0})(.*)" // expected groups: prefix, spaceOrParenthesis (ignored), match, suffix,
        });
    }

    var ChannelSelectorView = Backbone.View.extend({

        events: {
        },

        initialize: function(options) {

            if (!this.$el.length) {
                return;
            }

            this.channelSelector = initChannelSelector(this.$el, options.teamProvider, options.loggedIn, options.maxWidth, options.suggestionProvider);
            var self = this;
            this.channelSelector.model.$element.on("selected", function(e, itemDescriptor) {
                self.channelSelector.$field
                    .removeClass(SUGGEST_CHANNEL_CLASS)
                    .removeClass(PUBLIC_CHANNEL_CLASS)
                    .removeClass(PRIVATE_CHANNEL_CLASS);
                if (itemDescriptor.properties.value !== "") {
                    self.channelSelector.$field.addClass(itemDescriptor.properties.styleClass);
                    self.trigger("change");
                } else {
                    self.trigger("clear");
                }
            });

            this.channelSelector.model.$element.on("unselect", function(e) {
                self.trigger("clear");
            });
        },

        disable: function() {
            this.channelSelector.disable();
        },

        enable: function() {
            this.channelSelector.enable();
        },

        getSelectedChannel: function() {
            var meta = this.channelSelector.getSelectedDescriptor().meta();
            return {
                id: meta.channelId,
                channelName: meta.channelName,
                existing: meta.existing
            };
        },

        clearSelectedChannel: function() {
            this.channelSelector.clear();
            this.channelSelector._deactivate(); // We need that to get the placeholder back
        }

    });


    return ChannelSelectorView;
});
