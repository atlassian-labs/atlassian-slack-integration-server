define('bitbucket/slack/repository-to-channel/internal/service', [
    'underscore',
    'jquery'
],
/**
 * Internal services used to manage repository to channel configuration
 *
 * N.B. The exports tag allows us to define the AMD module name that gets exported in this file.
 * @exports confluence/slack/space-to-channel/internal/service
 */
function (
    _,
    $
) {
    'use strict';

    function getUserAndTeamInfo() {
        var slackConfig = $('#slack-channel-configuration');
        return {
            teamId: slackConfig.data('slack-team-id'),
            slackUserName: slackConfig.data('slack-user-name'),
        };
    }

    var Routes = {
        allChannels: function(repository) {
            return AJS.contextPath() + '/rest/slack/latest/channels?teamId=' + getUserAndTeamInfo().teamId
                + '&id=' + (repository ? repository.id : "");
        },
        channelConfig: function(repositoryId, teamId, channelId, notifName, initialLink) {
            var url = AJS.contextPath() + '/rest/slack/latest/config/' + encodeURI(repositoryId) + '/'
                + encodeURI(teamId) + '/' + encodeURI(channelId);
            if(notifName) {
                url += '/' + encodeURI(notifName);
            }
            if(initialLink) {
                url += '?initialLink=true';
            }
            AJS.log('url=', url);
            return url;
        },
        createChannel: function(newChannelName) {
            return AJS.contextPath() + '/rest/slack/latest/channels?channelName=' + encodeURI(newChannelName)
                + '&teamId=' + getUserAndTeamInfo().teamId;
        },
        saveOption: function (repositoryId, teamId, channelId, optionName, optionValue) {
            var url = AJS.contextPath() + '/rest/slack/latest/config/' + encodeURI(repositoryId) + '/'
                + encodeURI(teamId) + '/' + encodeURI(channelId) + '/option/' + encodeURI(optionName) + '/'
                + encodeURI(optionValue);
            return url;
        }
    };

    var configService = (function() {
        return {
            removeChannelMapping: function(spaceKey, teamId, channelId) {
                return $.ajax(Routes.channelConfig(spaceKey, teamId, channelId), {
                    type: 'DELETE'
                });
            },

            toggleNotification: function(repositoryId, teamId, channelId, notifName, enable, initialLink) {
                var requestType = enable ? 'PUT' : 'DELETE';
                return $.ajax(Routes.channelConfig(repositoryId, teamId, channelId, notifName, initialLink), {
                    type: requestType
                });
            },

            saveOption: function (repositoryId, teamId, channelId, optionName, optionValue) {
                return $.ajax(Routes.saveOption(repositoryId, teamId, channelId, optionName, optionValue), {
                    type: 'PUT'
                });
            }
        };
    })();

    // Encapsulate a channel service and data.
    // No protecting for mutation of passed in/out object by client as I don't want to where to cost of cloning both the array
    // and objects to prevent this, after all this is an internal service anyway.
    var channelServicePromise = (function(repository) {
        var allowMultipleMappingsToSameChannel = repository == null;
        var channelIdsToChannels = {}; // { "channelId": { channelId: "channelId", channelName: "channelName", isPrivate: boolean }, ... }
        var selectableChannels = []; // cache of [ { id: "channelId", channelName: "channelName", isPrivate: boolean } ] for use by select2 query. Derived from channelIdsToChannels
        var listeners = {};

        function rebuildChannelData() {
            selectableChannels = [];
            for (var channelId in channelIdsToChannels) {
                var channel = channelIdsToChannels[channelId];
                if (channel.selectable) {
                    selectableChannels.push({
                        teamId: channel.teamId,
                        teamName: channel.teamName,
                        id: channel.channelId,
                        channelName: channel.channelName,
                        text: channel.channelName, // required to display selected value in the dropdown
                        isPrivate: channel.isPrivate,
                        selectable: channel.selectable
                    });
                }
            }
            selectableChannels = _.sortBy(selectableChannels, function (item) { return item.channelName; });
            _.each(listeners, function(fn) {
                fn(selectableChannels);
            });
        }

        function addChannel(channel, isNew) {
            if (isNew) {
                channelIdsToChannels[channel.channelId] = channel;
            }
            rebuildChannelData();
        }

        function handleMappingToChannelRemoved(channel) {
            if (!allowMultipleMappingsToSameChannel) {
                channel.selectable = true;
                var channelLocal = channelIdsToChannels[channel.channelId];
                if (channelLocal) {
                    channelLocal.selectable = true;
                }
                rebuildChannelData();
            }
        }

        function handleMappingToChannelAdded(channel) {
            if (!allowMultipleMappingsToSameChannel) {
                channel.selectable = false;
                var channelLocal = channelIdsToChannels[channel.channelId];
                if (channelLocal) {
                    channelLocal.selectable = false;
                }
                rebuildChannelData();
            }
        }

        function onChange(fn) {
            listeners[fn] = fn;
        }

        function offChange(fn) {
            delete listeners[fn];
        }

        function getChannelById(channelId) {
            return channelIdsToChannels[channelId];
        }

        function findChannelByNameIgnoreCase(channelName) {
            var channelNameLower = channelName.toLocaleLowerCase();
            for (var channelId in channelIdsToChannels) {
                var channel = channelIdsToChannels[channelId];
                if (channel.channelName === channelNameLower) {
                    return channel;
                }
            }
            return null;
        }

        function allowSelectionByName(channelName) {
            if (allowMultipleMappingsToSameChannel) {
                return true;
            } else {
                var channel = findChannelByNameIgnoreCase(channelName);
                return channel != null && channel.selectable;
            }
        }

        function getOrCreateChannel(channel) {
            var deferred = $.Deferred();
            var existingChannel = findChannelByNameIgnoreCase(channel.channelName);
            if (existingChannel) {
                deferred.resolve(existingChannel);
                return deferred.promise();
            } else {
                $.ajax({
                    url: Routes.createChannel(channel.channelName),
                    type: 'POST',
                    dataType: 'json',
                    contentType: 'application/json',
                    cache: false
                }).done(function (slackChannelDefinition) {
                    var createdChannel = {
                        teamId: slackChannelDefinition.teamId,
                        teamName: slackChannelDefinition.teamName,
                        channelId: slackChannelDefinition.channelId,
                        channelName: slackChannelDefinition.channelName,
                        isPrivate: slackChannelDefinition.isPrivate,
                        selectable: true
                    };
                    channelIdsToChannels[createdChannel.channelId] = createdChannel;
                    rebuildChannelData();
                    deferred.resolve(createdChannel);
                }).fail(function (xhr) {
                    deferred.reject(channel.channelName, xhr.status, xhr.statusText)
                })
            }
            return deferred.promise();
        }

        function getSelectableChannels() {
            return selectableChannels;
        }

        var channelService = {
            handleMappingToChannelRemoved: handleMappingToChannelRemoved,
            handleMappingToChannelAdded: handleMappingToChannelAdded,
            getChannelById: getChannelById,
            findChannelByNameIgnoreCase: findChannelByNameIgnoreCase,
            allowSelectionByName: allowSelectionByName,
            getOrCreateChannel: getOrCreateChannel,
            getSelectableChannels: getSelectableChannels,
            onChange: onChange,
            offChange: offChange
        };

        var channelsAvailablePromise = (function () {
            var channelLoader = $.Deferred();
            var info = getUserAndTeamInfo();
            if (!info.teamId || !info.slackUserName) {
                return channelLoader.resolve(channelService);
            } else {
                $.ajax({
                    url: Routes.allChannels(repository),
                    dataType: 'json',
                    cache: false
                }).done(function(data) {
                    channelIdsToChannels = _.object(_.map(data, function (item) {
                        return [item.channelId, {
                            teamId: item.teamId,
                            teamName: item.teamName,
                            channelId: item.channelId,
                            channelName: item.channelName,
                            isPrivate: item.isPrivate,
                            selectable: true
                        }];
                    }));

                    if (repository) {
                        // remove channels already mapped
                        $('.slack-channel-config').each(function (index, el) {
                            var channelId = $(el).attr('data-channel-id');
                            if (channelId) {
                                var channel = channelIdsToChannels[channelId];
                                if (channel) {
                                    channel.selectable = false;
                                    rebuildChannelData();
                                }
                            }
                        });
                    }

                    rebuildChannelData();
                    channelLoader.resolve(channelService);
                }).fail(function (xhr) {
                    channelLoader.reject(xhr);
                });
            }
            return channelLoader.promise();
        })();

        return channelsAvailablePromise;
    });

    return function (repository) {
        var info = getUserAndTeamInfo();
        return {
            configService: configService,
            channelServicePromise: channelServicePromise(repository),
            slackUserName: info.slackUserName,
            teamId: info.teamId
        }
    };
});
