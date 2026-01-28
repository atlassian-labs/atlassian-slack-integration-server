require([
    'bitbucket/slack/repository-to-channel/internal/service',
    'bitbucket/slack/repository-to-channel/internal/admin-repository-picker',
    'bitbucket/util/state',
    'underscore'
], function (
    serviceFactory,
    adminRepositoryPicker,
    bitbucketState,
    _
) {
    'use strict';

    AJS.toInit(function($) {
        var currentRepository = bitbucketState.getRepository();
        var repository = currentRepository ? {
            id: currentRepository.id,
            name: currentRepository.name,
            slug: currentRepository.slug,
            projectKey: currentRepository.project.key,
            projectName: currentRepository.project.name
        } : null;

        var confirmDialog;
        var service = serviceFactory(repository);
        var configService = service.configService;
        var channelServicePromise = service.channelServicePromise;

        channelServicePromise.fail(function (xhr) {
            AJS.messages.error("#slack-repository-to-channel-messages", {
                title: AJS.I18n.getText('slack2-repository-configuration.select.channel.picker.error')
            });
        });

        function channelQuery(query) {
            channelServicePromise.done(function(channelService) {
                var channels = channelService.getSelectableChannels();
                var trimmedSearchText = query.term.trim();
                if (!trimmedSearchText) {
                    query.callback({
                        results: [{
                            text: AJS.I18n.getText('slack2-repository-configuration.select.channel.all.title'),
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
                        text: AJS.I18n.getText('slack2-repository-configuration.select.channel.matching.title'),
                        children: filteredChannels
                    });
                }
                if (!exactMatchingChannel && trimmedSearchText.length > 0) {
                    results.push({
                        text: AJS.I18n.getText('slack2-repository-configuration.select.channel.create.title'),
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
                return slack.integration.plugin.channelSelector.renderNewChannelItem({
                    name: channel.channelName,
                    isPrivate: channel.isPrivate
                });
            }
            if(!channel.channelName) {
                return channel.text;
            }
            return slack.integration.plugin.channelSelector.renderChannelItem({
                name: channel.channelName,
                isPrivate: channel.isPrivate
            });
        };

        var enableAddChannelButton = function() {
            $('#slack-repository-to-channel-add').removeAttr('aria-disabled').prop('disabled', false);
        };

        var updateAddMappingButtonEnablement = function (channelService) {
            var addChannelButton = $('#slack-repository-to-channel-add');
            var selectedChannel = getSelectedChannel();
            if (getSelectedRepository() && selectedChannel && channelService.allowSelectionByName(selectedChannel.channelName)) {
                addChannelButton.removeAttr('aria-disabled').prop('disabled', false);
            } else {
                addChannelButton.attr({
                    'aria-disabled': 'true',
                    'disabled': ''
                });
            }
        };

        var channelSelect = $('#slack-repository-to-channel-add-select').auiSelect2({
            placeholder: AJS.I18n.getText('slack2-repository-configuration.select.channel'),
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
            $channelSearchInput.attr("placeholder", AJS.I18n.getText("slack2-repository-configuration.select.channel.picker.empty") + " ...");
        }


        channelSelect.on('change', function() {
            if (getSelectedRepository()) {
                enableAddChannelButton();
            }
        });

        channelSelect.on('select2-selected', function(event) {
            selectedChannel = event.choice;
        });

        channelServicePromise.done(function(channelService) {
            channelService.onChange(function(channels) {
                if (!selectedChannel || !channelService.allowSelectionByName(selectedChannel.channelName)) {
                    resetChannelAndRepositoryDropdowns();
                }
                updateAddMappingButtonEnablement(channelService);
            })
        });

        var getSelectedChannel = function () {
            return selectedChannel;
        };

        var repositorySelect = $('#slack-repository-select').auiSelect2(adminRepositoryPicker.build({
            select2Options: {
                "width":"250px",
                containerCssClass: "select2-slack-repository-select"
            }
        }));

        var $searchInput = $(".select2-slack-repository-select .select2-input");
        if ($searchInput.length && "placeholder" in $searchInput[0]) {
            $searchInput.attr("placeholder", AJS.I18n.getText("bitbucket.plugins.slack.admin.repository.picker.empty") + " ...");
        }

        repositorySelect.on('change', function() {
            if (getSelectedChannel()) {
                enableAddChannelButton();
            }
        });

        function resetChannelAndRepositoryDropdowns() {
            channelSelect.auiSelect2("val", "");
            repositorySelect.auiSelect2("val", "");
            selectedChannel = null; // clear global variable as well
        }

        /**
         * Get the currently selected repository
         * @returns {{key:string, name:string}}
         */
        function getSelectedRepository() {
            // for repository configuration: repo information is static and known after the page is loaded
            if (repository) {
                return repository;
            }

            // for global admin configuration: extract repo information from the selected item in repo dropdown
            var selectedRepositoryData = repositorySelect.auiSelect2("data");
            if (selectedRepositoryData) {
                return {
                    id: selectedRepositoryData.id,
                    slug: selectedRepositoryData.slug,
                    projectKey: selectedRepositoryData.projectKey,
                    name: selectedRepositoryData.name,
                    projectName: selectedRepositoryData.projectName
                };
            }

            return null;
        }

        function editRepositoryChannelNotificationConfigLinkHandler(e) {
            e.preventDefault();
            // Only edit one section at a time
            editRepositoryChannelNotificationConfig($(this).closest('.slack-channel-config'));
        }

        function editRepositoryChannelNotificationConfig(channelConfigElem) {
            // Only edit one section at a time
            $('.slack-channel-config').removeClass('edit-active');
            channelConfigElem.addClass('edit-active');

            // enable tooltips
            $('.slack-tooltip').tooltip();
        }

        function closeRepositoryChannelNotificationConfig(e) {
            e.preventDefault();
            $(this).closest('.slack-channel-config').removeClass('edit-active');
        }

        function deleteRepositoryChannelNotificationConfig(e) {
            e.preventDefault();
            var config = $(this).closest('.slack-channel-config');
            var removeData = {
                config: config,
                teamId: config.attr('data-team-id'),
                channelId: config.attr('data-channel-id'),
                channelName: config.attr('data-channel-name'),
                isPrivate: config.attr('data-channel-private') === "true",
                repositoryId: config.attr('data-repository-id')
            };

            function completeDeleteChannelMapping(removeData) {
                var config = removeData.config;
                config.find('input').attr('disabled', 'disabled');
                config.spin("large");
                configService.removeChannelMapping(removeData.repositoryId, removeData.teamId, removeData.channelId).done(function () {
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
                    config.find('input').prop('disabled', false);
                    config.spinStop();
                    displayUpdateConfigurationError(jqXHR.status, jqXHR.statusText);
                });
            }

            if (config.find(':checked').length) {
                // Mappings still exist confirm with user
                if (!confirmDialog) {
                    // TODO CONFDEV-28519 ADG 2 - new confirmation dialog (and remove related CSS)
                    confirmDialog = new AJS.Dialog(400, 200, "slack-remove-mapping-dialog");
                    confirmDialog.addHeader(AJS.I18n.getText("slack2-repository-configuration.remove.dialog.header"), "remove-warning");
                    confirmDialog.addPanel("Message Panel", "<div class='remove-mapping-message'></div>");
                    confirmDialog.addButton(AJS.I18n.getText("slack2-repository-configuration.remove.dialog.confirm"), function() {
                        completeDeleteChannelMapping(confirmDialog.removeData);
                        confirmDialog.hide();
                    });
                    confirmDialog.addCancel(AJS.I18n.getText("slack2-repository-configuration.remove.dialog.cancel"), function() {
                        confirmDialog.hide();
                    });
                }
                // ensure current removeData is available for dialog click handler
                confirmDialog.removeData = removeData;

                $('#slack-remove-mapping-dialog .remove-mapping-message')
                    .text(AJS.I18n.getText("slack2-repository-configuration.remove.dialog.message", removeData.channelName));
                confirmDialog.show();
            } else {
                // Just remove
                completeDeleteChannelMapping(removeData);
            }
        }

        // TODO CONFDEV-28519 ADG 2 - new error style?
        function displayUpdateConfigurationError(statusCode, statusText) {
            displayError(AJS.I18n.getText("slack2-repository-configuration.update.error"), statusCode, statusText);
        }

        function displayError(title, statusCode, statusText) {
            var messageContainer = $('#slack-repository-to-channel-messages');
            messageContainer.empty();
            AJS.messages.error("#slack-repository-to-channel-messages", {
                title: title,
                body: "<p>" + AJS.escapeHtml(AJS.I18n.getText("slack2-repository-configuration.update.error.reason", statusCode, statusText)) + "</p>"
            });
        }

        function addChannelMappingHandler(e) {
            e.preventDefault();
            var selectedChannel = getSelectedChannel();
            if (selectedChannel) {
                channelServicePromise.done(function(channelService) {
                    var addChannelButton = $('#slack-repository-to-channel-add');
                    addChannelButton.attr('disabled','disabled');
                    var spinner = $("#slack-repository-to-channel-add-mapping-spinner").spin();
                    channelService.getOrCreateChannel(selectedChannel)
                        .done(function (channel) {
                            addChannelButton.prop('disabled', false);
                            spinner.spinStop();
                            addChannelMapping(channel);
                        })
                        .fail(function (newChannelName, statusCode, statusText) {
                            addChannelButton.prop('disabled', false);
                            spinner.spinStop();
                            displayError(AJS.I18n.getText("slack2-repository-configuration.select.channel.create.error.title", newChannelName), statusCode, statusText);
                        });
                });
            }
        }

        function addChannelMapping(channel) {
            var repository = getSelectedRepository();

            if (repository && channel) {
                var repositoryChannelNotificationConfig = getRepositoryChannelNotificationConfig(repository, channel);
                if (repositoryChannelNotificationConfig) {
                    editRepositoryChannelNotificationConfig(repositoryChannelNotificationConfig);
                } else {
                    addRepositoryChannelNotificationConfig(repository, channel);
                }
            }

            $(".slack-integration-steps").trigger('mapping-added.integration-steps');
        }

        function addRepositoryChannelNotificationConfig(repository, channel) {
            if (repository && channel) {
                channelServicePromise.done(function(channelService) {
                    channelService.handleMappingToChannelAdded(channel);
                    resetChannelAndRepositoryDropdowns();
                    updateAddMappingButtonEnablement(channelService);
                });
                var renderParams = {
                    repository: {
                        name: repository.name,
                        id: repository.id,
                        slug: repository.slug,
                        project: {
                            key: repository.projectKey,
                            name: repository.projectName
                        },
                    },
                    channelConfig: {
                        channelDetails: channel,
                        activateByDefault: true // check all checkboxes by default
                    },
                };

                var channelConfigMarkup = bitbucket.slack.feature.repoChannelTable.renderSingleChannelMapping(renderParams);
                // just insert new config first; if there are a couple of them already in the table,
                // adding a new config to the bottom of middle of the table is hard to notice for user
                $(".slack-channel-mapping-list").prepend(channelConfigMarkup);

                /*
                 * To keep this simple, I'm doing a "check" per default notification. So potentially this means n REST calls for
                 * each new channel mapping (where n is the number of notifications active by default). In reality this is just one
                 * right now, but could be more.
                 *
                 * We could optimise to do a batch check, but I didn't think was necessary or worth the effort to build a new
                 * REST resource.
                 */
                mapRepositoryChannelNotificationType(repository.id, channel.teamId, channel.channelId, 'new', true, true);

                editRepositoryChannelNotificationConfig(getRepositoryChannelNotificationConfig(repository, channel));
            }
        }

        function getRepositoryChannelNotificationConfig(repository, channel) {
            var repositoryChannelNotificationConfig = $('.slack-channel-config[data-channel-id="' + channel.channelId + '"][data-repository-id="' + repository.id + '"]');
            if (repositoryChannelNotificationConfig && repositoryChannelNotificationConfig.length) {
                return repositoryChannelNotificationConfig;
            } else {
                return null;
            }
        }

        function toggleRepositoryChannelNotificationType(e) {
            var check = $(this);
            var notifName = check.attr('data-notification-name');
            var config = check.closest('.slack-channel-config');
            var teamId = config.attr('data-team-id');
            var channelId = config.attr('data-channel-id');
            var enable = check.is(':checked');
            var repositoryId = config.attr('data-repository-id');
            mapRepositoryChannelNotificationType(repositoryId, teamId, channelId, notifName, enable);
        }

        function saveRepositoryChannelOption(e) {
            var option = $(this);
            var optionName = option.attr('data-option-name');
            var optionValue = option.attr('value');
            var config = option.closest('.slack-channel-config');
            var repositoryId = config.attr('data-repository-id');
            var teamId = config.attr('data-team-id');
            var channelId = config.attr('data-channel-id');
            configService.saveOption(repositoryId, teamId, channelId, optionName, optionValue)
                .always(function() {
                    AJS.log('saved option', repositoryId, teamId, channelId, optionName, optionValue);
                }).fail(function(jqXHR, textStatus) {
                    AJS.log('option save failed', jqXHR.statusCode(), textStatus, jqXHR);
                    displayUpdateConfigurationError(jqXHR.status, jqXHR.statusText);
                });
        }

        function mapRepositoryChannelNotificationType(repositoryId, teamId, channelId, notifKey, enable, initialLink) {
            var promise = configService.toggleNotification(repositoryId, teamId, channelId, notifKey, enable, initialLink);

            promise.always(function() {
                AJS.log('update done for', repositoryId, teamId, channelId, notifKey, enable);
            }).fail(function( jqXHR, textStatus, errorThrown ) {

                // reset checkbox in case request fails in order to prevent tricking users
                if (notifKey === 'new') {
                    var groupId = '#notification-' + repositoryId + '-' + channelId;
                    $(groupId + ' :checkbox').prop('checked', !enable);
                } else {
                    var checkId = '#notification-' + repositoryId + '-' + channelId + '-' + notifKey;
                    $(checkId).prop('checked', !enable);
                }

                AJS.log('select notification failed', jqXHR.statusCode(), textStatus, jqXHR);
                displayUpdateConfigurationError(jqXHR.status, jqXHR.statusText);
            });
        }

        $('#slack-repository-to-channel-add').on('click', addChannelMappingHandler);

        $('#slack-channel-configuration')
                .on('click', '.edit-notification', editRepositoryChannelNotificationConfigLinkHandler)
                .on('click', '.close-edit-notification', closeRepositoryChannelNotificationConfig)
                .on('click', '.trash-channel-mapping', deleteRepositoryChannelNotificationConfig)
                .on('click', '.notification-type', toggleRepositoryChannelNotificationType)
                .on('click', '.notification-option', saveRepositoryChannelOption);
    });

});
