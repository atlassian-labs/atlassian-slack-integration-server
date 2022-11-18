require([
    'confluence/slack/space-to-channel/internal/service',
    'confluence/slack/space-to-channel/internal/admin-space-picker',
    'underscore'
], function (
    serviceFactory,
    adminSpacePicker,
    _
) {
    'use strict';

    AJS.toInit(function($) {
        var space = AJS.Meta.get('space-key') ? {
            key: AJS.Meta.get('space-key'),
            name: AJS.Meta.get('space-name')
        } : null;
        var form = $('#confluence-space-to-channel-mapping-form');
        var notificationTypes = form.data('notification-types');

        var confirmDialog;
        var service = serviceFactory(space);
        var configService = service.configService;
        var channelServicePromise = service.channelServicePromise;
        var pendingSpaceChannelNotificationConfigs = (function() {
            var newChannelMap = {};

            return {
                addSpaceChannelNotificationConfig: function(space, channel) {
                    if (space && channel) {
                        var spaceChannelKey = space.key + '--' +  channel.channelId;
                        newChannelMap[spaceChannelKey] = {space: space, channel : channel};
                    }
                },
                removeChannelBySpaceKeyAndChannelId: function(spaceKey, channelId) {
                    var spaceChannelKey = spaceKey + '--' +  channelId;
                    var removed = !!newChannelMap[spaceChannelKey];
                    delete newChannelMap[spaceChannelKey];
                    return removed;
                }
            };
        })();

        function markSpaceLinkingDiscovered() {
            Confluence.FeatureDiscovery.forPlugin("com.atlassian.confluence.plugins.confluence-slack-server-integration-plugin")
                    .markDiscovered("slackSpaceIntegration");
        }

        channelServicePromise.fail(function (xhr) {
            AJS.messages.error("#slack-space-to-channel-messages", {
                title: AJS.I18n.getText('slack2-space-configuration.select.channel.picker.error')
            });
        });

        function channelQuery(query) {
            channelServicePromise.done(function(channelService) {
                var channels = channelService.getSelectableChannels();
                var trimmedSearchText = query.term.trim();
                if (!trimmedSearchText) {
                    query.callback({
                        results: [{
                            text: AJS.I18n.getText('slack2-space-configuration.select.channel.all.title'),
                            children: channels
                        }]
                    });
                    return;
                }

                var searchStrLower = trimmedSearchText.toLocaleLowerCase();
                var exactMatchingChannel = false;
                var filteredChannels = new Array(channels.length);
                var i, p, len = channels.length;
                for (i = 0, p = 0; i < len; i++) {
                    var item = channels[i];
                    var channelName = item.channelName;

                    if (channelName === searchStrLower) {
                        exactMatchingChannel = true;
                    }

                    if (!searchStrLower || channelName.indexOf(searchStrLower) >= 0) {
                        filteredChannels[p++] = item;
                    }
                }
                filteredChannels.length = p;

                var results = [];
                var matchCount = filteredChannels.length;
                if (matchCount >= 1) {
                    results.push({
                        text: AJS.I18n.getText('slack2-space-configuration.select.channel.matching.title'),
                        children: filteredChannels
                    });
                }
                if (!exactMatchingChannel && trimmedSearchText.length > 0) {
                    results.push({
                        text: AJS.I18n.getText('slack2-space-configuration.select.channel.create.title'),
                        children: [{
                            id: 0,
                            text: trimmedSearchText,
                            channelName: trimmedSearchText,
                            isCreateChannelItem: true,
                            isPrivate: false
                        }]
                    });
                }
                query.callback({ results: results });
            });
        }

        var channelRenderer = function(channel) {
            if (channel.isCreateChannelItem) {
                return Confluence.Templates.Slack.SpaceToChannel.Config.renderNewChannelItem({
                    name: channel.channelName,
                    isPrivate: channel.isPrivate
                });
            }
            if(!channel.channelName) {
                return channel.text;
            }
            return Confluence.Templates.Slack.SpaceToChannel.Config.renderChannelItem({
                name: channel.channelName,
                isPrivate: channel.isPrivate
            });
        };

        var enableAddChannelButton = function() {
            $('#slack-space-to-channel-add').removeAttr('aria-disabled').removeAttr('disabled')
        };

        var updateAddMappingButtonEnablement = function (channelService) {
            var addChannelButton = $('#slack-space-to-channel-add');
            var selectedChannel = getSelectedChannel();
            if (getSelectedSpace && selectedChannel && channelService.allowSelectionByName(selectedChannel.channelName)) {
                addChannelButton.removeAttr('aria-disabled').removeAttr('disabled')
            } else {
                addChannelButton.attr({
                    'aria-disabled': 'true',
                    'disabled': ''
                });
            }
        };

        var channelSelect = $('#slack-space-to-channel-add-select').auiSelect2({
            placeholder: AJS.I18n.getText('slack2-space-configuration.select.channel'),
            containerCssClass: 'select2-slack-channel-select',
            dropdownCssClass: 'select2-slack-channel-dropdown',
            width: '200px',
            query: channelQuery,
            allowClear: true,
            formatResult: channelRenderer
        });
        var selectedChannel = null;

        var $channelSearchInput = $(".select2-slack-channel-select .select2-input");
        if ($channelSearchInput.length && "placeholder" in $channelSearchInput[0]) {
            $channelSearchInput.attr("placeholder", AJS.I18n.getText("slack2-space-configuration.select.channel.picker.empty") + " ...");
        }


        channelSelect.on('change', function() {
            if (getSelectedSpace()) {
                enableAddChannelButton();
            }
        });

        channelSelect.on('select2-selected', function(event) {
            selectedChannel = event.choice;
        });

        channelServicePromise.done(function(channelService) {
            channelService.onChange(function(channels) {
                if (!selectedChannel || !channelService.allowSelectionByName(selectedChannel.channelName)) {
                    channelSelect.auiSelect2("val", "");
                }
                updateAddMappingButtonEnablement(channelService);
            })
        });

        var getSelectedChannel = function () {
            return selectedChannel;
        };

        var spaceSelect = $('#slack-space-select').auiSelect2(adminSpacePicker.build({
            select2Options: {
                "width":"250px",
                containerCssClass: "select2-slack-space-select"
            }
        }));

        var $searchInput = $(".select2-slack-space-select .select2-input");
        if ($searchInput.length && "placeholder" in $searchInput[0]) {
            $searchInput.attr("placeholder", AJS.I18n.getText("confluence.plugins.slack.admin.space.picker.empty") + " ...");
        }

        spaceSelect.on('change', function() {
            if (getSelectedChannel()) {
                enableAddChannelButton();
            }
        });

        /**
         * Get the currently selected space
         * @returns {{key:string, name:string}}
         */
        function getSelectedSpace() {
            if (space) {
                return space;
            }

            var selectedSpaceData = spaceSelect.auiSelect2("data");
            if (selectedSpaceData) {
                return {
                    key: selectedSpaceData.id,
                    name: selectedSpaceData.text
                };
            }

            return null;
        }

        function editSpaceChannelNotificationConfigLinkHandler(e) {
            e.preventDefault();
            // Only edit one section at a time
            editSpaceChannelNotificationConfig($(this).closest('.slack-channel-config'));
        }

        function editSpaceChannelNotificationConfig(channelConfigElem) {
            // Only edit one section at a time
            $('.slack-channel-config').removeClass('edit-active');
            channelConfigElem.addClass('edit-active');
        }

        function closeSpaceChannelNotificationConfig(e) {
            e.preventDefault();
            $(this).closest('.slack-channel-config').removeClass('edit-active');
        }

        function deleteSpaceChannelNotificationConfig(e) {
            e.preventDefault();
            var config = $(this).closest('.slack-channel-config');
            var removeData = {
                config: config,
                teamId: config.attr('data-team-id'),
                channelId: config.attr('data-channel-id'),
                channelName: config.attr('data-channel-name'),
                isPrivate: config.attr('data-channel-private') === "true",
                spaceKey: config.attr('data-space-key')
            };

            function completeDeleteChannelMapping(removeData) {
                var config = removeData.config;
                config.find('input').attr('disabled', 'disabled');
                config.spin("large");
                configService.removeChannelMapping(removeData.spaceKey, removeData.teamId, removeData.channelId).done(function () {
                    channelServicePromise.done(function(channelService) {
                        channelService.handleMappingToChannelRemoved({
                            teamId: removeData.teamId,
                            teamName: removeData.teamName,
                            channelId: removeData.channelId,
                            channelName: removeData.channelName,
                            isPrivate: removeData.isPrivate
                        });
                    });
                    config.spinStop();
                    config.remove();
                }).fail(function (jqXHR, textStatus, errorThrown) {
                    config.find('input').removeAttr('disabled');
                    config.spinStop();
                    displayUpdateConfigurationError(jqXHR.status, jqXHR.statusText);
                });
            }

            if (config.find(':checked').length) {
                // Mappings still exist confirm with user
                if (!confirmDialog) {
                    // TODO CONFDEV-28519 ADG 2 - new confirmation dialog (and remove related CSS)
                    confirmDialog = new AJS.Dialog(400, 200, "slack-remove-mapping-dialog");
                    confirmDialog.addHeader(AJS.I18n.getText("slack2-space-configuration.remove.dialog.header"), "remove-warning");
                    confirmDialog.addPanel("Message Panel", "<div class='remove-mapping-message'></div>");
                    confirmDialog.addButton(AJS.I18n.getText("slack2-space-configuration.remove.dialog.confirm"), function() {
                        completeDeleteChannelMapping(confirmDialog.removeData);
                        confirmDialog.hide();
                    });
                    confirmDialog.addCancel(AJS.I18n.getText("slack2-space-configuration.remove.dialog.cancel"), function() {
                        confirmDialog.hide();
                    });
                }
                // ensure current removeData is available for dialog click handler
                confirmDialog.removeData = removeData;

                $('#slack-remove-mapping-dialog .remove-mapping-message')
                    .text(AJS.I18n.getText("slack2-space-configuration.remove.dialog.message", removeData.channelName));
                confirmDialog.show();
            } else {
                // Just remove
                completeDeleteChannelMapping(removeData);
            }
        }

        // TODO CONFDEV-28519 ADG 2 - new error style?
        function displayUpdateConfigurationError(statusCode, statusText) {
            displayError(AJS.I18n.getText("slack2-space-configuration.update.error"), statusCode, statusText);
        }

        function displayError(title, statusCode, statusText) {
            var messageContainer = $('#slack-space-to-channel-messages');
            messageContainer.empty();
            AJS.messages.error("#slack-space-to-channel-messages", {
                title: title,
                body: "<p>" + AJS.escapeHtml(AJS.I18n.getText("slack2-space-configuration.update.error.reason", statusCode, statusText)) + "</p>"
            });
        }

        function addChannelMappingHandler(e) {
            e.preventDefault();
            var selectedChannel = getSelectedChannel();
            if (selectedChannel) {
                channelServicePromise.done(function(channelService) {
                    var addChannelButton = $('#slack-space-to-channel-add');
                    addChannelButton.attr('disabled','disabled');
                    var spinner = $("#slack-space-to-channel-add-mapping-spinner").spin();
                    channelService.getOrCreateChannel(selectedChannel)
                        .done(function (channel) {
                            addChannelButton.removeAttr('disabled');
                            spinner.spinStop();
                            addChannelMapping(channel);
                        })
                        .fail(function (newChannelName, statusCode, statusText) {
                            addChannelButton.removeAttr('disabled');
                            spinner.spinStop();
                            displayError(AJS.I18n.getText("slack2-space-configuration.select.channel.create.error.title", newChannelName), statusCode, statusText);
                        });
                });
            }
        }

        function addChannelMapping(channel) {
            var space = getSelectedSpace();

            if (space && channel) {
                var spaceChannelNotificationConfig = getSpaceChannelNotificationConfig(space, channel);
                if (spaceChannelNotificationConfig) {
                    editSpaceChannelNotificationConfig(spaceChannelNotificationConfig);
                } else {
                    addSpaceChannelNotificationConfig(space, channel);
                }
            }

            $(".slack-integration-steps").trigger('mapping-added.integration-steps');
        }

        function addSpaceChannelNotificationConfig(space, channel) {
            if (space && channel) {
                channelServicePromise.done(function(channelService) {
                    channelService.handleMappingToChannelAdded(channel);
                });
                var compareSortName = channel.channelName.toLocaleLowerCase();
                var renderParams = $.extend({
                    spaceName: space.name,
                    spaceKey: space.key,
                    spaceUrlPath: '/display/' + space.key,
                    notificationTypes: notificationTypes
                }, channel);
                var channelConfigMarkup = Confluence.Templates.Slack.SpaceToChannel.Config.renderSingleChannelMapping(renderParams);
                var insertBefore = $('.slack-channel-config').filter(function(index, el) {
                    var currentName = $(el).attr('data-channel-name').toLocaleLowerCase();
                    return currentName.localeCompare(compareSortName) > 0;
                }).first();

                pendingSpaceChannelNotificationConfigs.addSpaceChannelNotificationConfig(space, channel);

                if(insertBefore.length) {
                    insertBefore.before(channelConfigMarkup);
                } else {
                    $(".slack-channel-mapping-list").append(channelConfigMarkup);
                }

                /*
                 * To keep this simple, I'm doing a "check" per default notification. So potentially this means n REST calls for
                 * each new channel mapping (where n is the number of notifications active by default). In reality this is just one
                 * right now, but could be more.
                 *
                 * We could optimise to do a batch check, but I didn't think was necessary or worth the effort to build a new
                 * REST resource.
                 */
                _.each(notificationTypes, function(notificationType, index) {
                    if(notificationType.activeByDefault) {
                        mapSpaceChannelNotificationType(space.key, channel.teamId, channel.channelId, notificationType.key, true);
                    }
                });

                editSpaceChannelNotificationConfig(getSpaceChannelNotificationConfig(space, channel));
            }
        }

        function getSpaceChannelNotificationConfig(space, channel) {
            var spaceChannelNotificationConfig = $('.slack-channel-config[data-channel-id="' + channel.channelId + '"][data-space-key="' + space.key + '"]');
            if (spaceChannelNotificationConfig && spaceChannelNotificationConfig.length) {
                return spaceChannelNotificationConfig;
            } else {
                return null;
            }
        }

        function toggleSpaceChannelNotificationType(e) {
            var check = $(this);
            var notifName = check.attr('data-notification-name');
            var config = check.closest('.slack-channel-config');
            var teamId = config.attr('data-team-id');
            var channelId = config.attr('data-channel-id');
            var enable = check.is(':checked');
            var spaceKey = config.attr('data-space-key');
            mapSpaceChannelNotificationType(spaceKey, teamId, channelId, notifName, enable);
        }

        function mapSpaceChannelNotificationType(spaceKey, teamId, channelId, notifName, enable) {
            var initialLink = pendingSpaceChannelNotificationConfigs.removeChannelBySpaceKeyAndChannelId(spaceKey, channelId);
            var promise = configService.toggleNotification(spaceKey, teamId, channelId, notifName, enable, initialLink);

            if (space) {
                markSpaceLinkingDiscovered();
            }

            promise.always(function() {
                AJS.debug('update done for ', spaceKey, teamId, channelId, notifName, enable);
            }).fail(function( jqXHR, textStatus, errorThrown ) {
                AJS.debug('select notification failed', jqXHR.statusCode(), textStatus, jqXHR);
                displayUpdateConfigurationError(jqXHR.status, jqXHR.statusText);
            });
        }

        $('#slack-space-to-channel-add').click(addChannelMappingHandler);

        $('#slack-space-to-channel-configuration')
                .delegate('.edit-notification', 'click', editSpaceChannelNotificationConfigLinkHandler)
                .delegate('.close-edit-notification', 'click', closeSpaceChannelNotificationConfig)
                .delegate('.trash-channel-mapping', 'click', deleteSpaceChannelNotificationConfig)
                .delegate('.notification-type', 'click', toggleSpaceChannelNotificationType);
    });

});
