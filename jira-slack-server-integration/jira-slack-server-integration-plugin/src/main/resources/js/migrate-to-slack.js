require([
    'jquery',
    'wrm/context-path',
    "slack/widget/channelselector/channelmapping-service",
    'jira/flag'
], function (
    $,
    wrmContextPath,
    ChannelMappingService,
    jiraFlag
) {
    'use strict';

    var contextPath = wrmContextPath();
    var migrateDialogUrl = contextPath + '/secure/MigrateToSlackAction.jspa';

    var dialogSubmitSelector = "#slack-migrate-select-channel-dialog-submit";
    var dialogId = "migrate-to-slack-dialog";
    var dialog = null;

    var PRIVATE_CHANNEL_CLASS = "private-channel";
    var PUBLIC_CHANNEL_CLASS = "public-channel";

    var fuseOptions = {
        shouldSort: true,
        findAllMatches: true, // display all channels, even with different names
        threshold:  1.0, // 1.0 is less restrictive as possible
        distance: 1000, // show
        maxPatternLength: 50, // max HC room name length
        minMatchCharLength: 1,
        location: 0,
        keys: [ "text" ]
    };

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

    function createItemDescriptor(item) {
        return new AJS.ItemDescriptor({
            value: item.teamId + "|" + item.id,
            label: item.text + "@" + item.teamName,
            html: escapeResult(item.text + " @ " + item.teamName),
            title: item.text + "@" + item.teamName,
            meta: {
                teamId: item.teamId,
                channelId: item.id,
                channelName: item.channelName || item.text,
                existing: true
            },
            styleClass: item.isPrivate ? PRIVATE_CHANNEL_CLASS : PUBLIC_CHANNEL_CLASS
        });
    }

    function makeSuggestionsHandler(channelsPromise) {
        return Class.extend({
            _formatResponse: function(channels) {
                var groupDescriptors = [];
                if (channels.length > 0) {
                    var suggestionsGroupDescriptor = new AJS.GroupDescriptor({
                        weight: 1,
                        label: AJS.I18n.getText("jira.plugins.slack.channelmapping.channels.all.label")
                    });

                    _.each(channels, function(item) {
                        suggestionsGroupDescriptor.addItem(createItemDescriptor(item));
                    });

                    groupDescriptors.push(suggestionsGroupDescriptor);
                }
                return groupDescriptors;
            },

            execute: function(query) {
                var deferred = jQuery.Deferred();
                var self = this;

                channelsPromise.done(function(channels) {
                    var searchStr = query.toLocaleLowerCase();
                    var exactMatch = false;
                    var filteredChannels = _.filter(channels, function(item) {
                        var lowerCasedItem = item.text.toLocaleLowerCase();
                        if (lowerCasedItem === searchStr) {
                            exactMatch = true;
                        }

                        return lowerCasedItem.indexOf(searchStr) >= 0;
                    });

                    var suggestions = self._formatResponse(filteredChannels);

                    JIRA.trace("ajax.request.completed.MigrateChannelSelector");
                    deferred.resolve(suggestions, query);
                }).fail(function() {
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
                });

                return deferred;
            }
        });
    }

    function enableDialogSubmit() {
        $(dialogSubmitSelector)
            .removeAttr('aria-disabled').removeAttr('disabled');
    }

    function disableDialogSubmit() {
        $('#slack-migrate-select-channel-dialog-submit').attr({
            'aria-disabled': 'true',
            'disabled': ''
        });
    }

    function dialogContentReadyCallback(e, dialog) {
        if (dialog.options.id === dialogId) {
            var configuration = dialog.options.configuration;

            var sanitizedRoomName = configuration.roomName
                .replace(/[\s]+/g, "-") // spaces by - to make it more similar
                .toLowerCase();

            var channelServicePromise = ChannelMappingService.channelServicePromise(
                ChannelMappingService.MIGRATION, true, getProjectKey());

            var channelsPromise = channelServicePromise.pipe(function(channelService) {
                    var rawChannels = channelService.getChannels();
                    var fuse = new Fuse(rawChannels, fuseOptions);
                    return fuse.search(sanitizedRoomName);
            });

            var channelSelector = new AJS.SingleSelect({
                element: $('#slack-migrate-channel-select'),
                width: "480px",
                suggestionsHandler: makeSuggestionsHandler(channelsPromise),
                submitInputVal: true,
                matchingStrategy: "(.*)()({0})(.*)" // expected groups: prefix, spaceOrParenthesis (ignored), match, suffix
            });

            channelSelector.model.$element.on("selected", function(e, item) {
                var selectedChannel = item.properties.value;
                if (selectedChannel && selectedChannel.id !== "") {
                    enableDialogSubmit();
                }
            });
            channelSelector.model.$element.on("unselect", function() {
                disableDialogSubmit();
            });

            var submitSelectChannelDialog = function () {
                dialog.showFooterLoadingIndicator();

                var channel = channelSelector.getSelectedDescriptor().meta();
                $.ajax({
                    url: wrmContextPath() + "/slack/mapping/" + encodeURI(configuration.projectKey) + "/migrate",
                    dataType: 'json',
                    type: "POST",
                    contentType: "application/json",
                    data: JSON.stringify({
                        teamId: channel.teamId,
                        channelId: channel.channelId,
                        projectKey: configuration.projectKey,
                        projectId: configuration.projectId,
                        values: $.extend({}, {
                            "MATCHER:ISSUE_CREATED": configuration.issueCreatedCheck ? "true" : undefined,
                            "MATCHER:ISSUE_ASSIGNMENT_CHANGED": configuration.issueAssignmentCheck ? "true" : undefined,
                            "MATCHER:ISSUE_COMMENTED": configuration.issueCommentedCheck ? "true" : undefined,
                            "MATCHER:ISSUE_TRANSITIONED": configuration.issueTransitionCheck ? configuration.transitionValues : undefined,
                            "FILTER:JQL_QUERY": configuration.jql || undefined,
                        })
                    })
                }).done(function () {
                    dialog.hide();

                    // success flag
                    jiraFlag.showSuccessMsg(null, AJS.I18n.getText("jira.plugins.slack.migration.success.message",
                        configuration.roomName, channel.channelName));

                    // store this config was migrated
                    localStorage.setItem('slack-migration-' + configuration.configurationGroup, 'true');

                    // add checked logo so user knows it was migrated with success
                    $('tbody[data-configuration-group-id=' + configuration.configurationGroup + '] .slack-logo-image')
                        .addClass('migrated');

                    JIRA.trace("slack.migration.from.hipchat.performed");
                }).fail(function (err) {
                    console.log("Error migrating configuration", err);
                    var $errorContainer = $("#" + dialogId).find(".dialog-errors");
                    var text = AJS.I18n.getText("jira.plugins.slack.migration.name.error");
                    $errorContainer.show().empty().append(text);
                }).always(function () {
                    dialog.hideFooterLoadingIndicator();
                });
            };

            // submit button
            disableDialogSubmit();
            $('#slack-migrate-select-channel-dialog-submit').on("click", submitSelectChannelDialog);
            $("#" + dialogId).find("form").on("before-submit", function (e) {
                e.preventDefault();

                if (channelSelector.getSelectedDescriptor()) {
                    submitSelectChannelDialog();
                }
            });

            // auto-select first channel (which is the most similar to the room name)
            channelsPromise.done(function(channels) {
                if (channels && channels.length) {
                    channelSelector.setSelection(createItemDescriptor(channels[0]));
                    enableDialogSubmit();
                }
            });
        }
    }

    $(function () {
        "use strict";

        $("#hipchat-room-configuration").each(function() {
            var page = $(this);
            page.find(".summary-menu").each(function() {
                var configGroup = $(this).closest('tbody').attr("data-configuration-group-id");
                var migrated = localStorage.getItem('slack-migration-' + configGroup);
                $(Slack.Templates.Migration.migrateToSlack({ migrated: migrated })).appendTo(this);
            });

            // remove migration button if editing HC config; users have to reload otherwise stale data would be migrated
            page.on('click', '.edit-notification', function(e) {
                $('.migrate-to-slack').remove();
            });

            // handle migration button click
            page.on('click', '.migrate-to-slack', function(e) {
                e.preventDefault();

                var menu = $(this).closest(".summary-menu");
                var configRow = menu.closest("tbody");
                var editCell = configRow.find(".edit");

                // fetch context data
                var roomName = configRow.attr("data-room-name");
                var projectId = configRow.attr("data-project-id");
                var projectKey = configRow.attr("data-project-key");
                var configurationGroup = configRow.attr("data-configuration-group-id");

                // fetch config
                var issueCreatedCheck = editCell.find('[data-notification-name=IssueCreate]').prop("checked");
                var issueAssignmentCheck = editCell.find('[data-notification-name=IssueAssignmentChanged]').prop("checked");
                var issueCommentedCheck = editCell.find('[data-notification-name=IssueCommented]').prop("checked");
                var issueTransitionCheck = editCell.find('[data-notification-name=IssueTransition]').prop("checked");
                var transitionValues = editCell.find('[data-id=status]').attr("data-value");
                var jql = editCell.find('.basic-search-container').attr("data-jql");

                // current configuration
                var configuration = {
                    configurationGroup: configurationGroup,
                    roomName: roomName,
                    projectKey: projectKey,
                    projectId: projectId,
                    issueCreatedCheck: issueCreatedCheck,
                    issueAssignmentCheck: issueAssignmentCheck,
                    issueCommentedCheck: issueCommentedCheck,
                    issueTransitionCheck: issueTransitionCheck,
                    transitionValues: transitionValues,
                    jql: jql
                };

                // create and show migration dialog
                $('#' + dialogId).remove();
                dialog = new JIRA.FormDialog({
                    id: dialogId,
                    ajaxOptions: {
                        url: migrateDialogUrl,
                        data: {
                            decorator: "dialog",
                            inline: "false",
                            roomName: configuration.roomName
                        }
                    },
                    configuration: configuration,
                    width: 500,
                    autoClose : true
                });
                dialog.show();
            });

            $(document).bind("dialogContentReady", {}, dialogContentReadyCallback);
        });

    }
)});
