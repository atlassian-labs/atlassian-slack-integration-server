define("slack/widget/channelselector/channelmapping-service",
[
    "wrm/context-path",
    "jquery",
    "underscore"
],
function (
    wrmContextPath,
    $,
    _
) {
    var MIGRATION = "MIGRATION";

    var channelServicePromise = (function(teamId, loggedIn, projectKey) {
        var channelMap = {}; // { "channelId": { channelId: "channelId", channelName: "channelName" }, ... }
        var channels = []; // cache of [ { id: "channelId", text: "channelName } ] for use by select2 query. Derived from channelMap
        var listeners = {};
        var isMigration = MIGRATION === teamId;

        function rebuildChannelData() {
            channels = _.map(channelMap, function(item) {
                return {
                    id: item.channelId,
                    teamId: item.teamId,
                    teamName: item.teamName,
                    text: item.channelName,
                    isPrivate: item.isPrivate
                };
            });
            channels = _.sortBy(channels, function (item) {
                return item.text;
            });
            _.each(listeners, function(fn) {
                fn.apply(null, [ channels ]);
            });
        }

        function createChannel(name) {
            var dfd = $.Deferred();
            $.ajax({
                url: wrmContextPath() + '/slack/channels?channelName=' + window.encodeURIComponent(name) + '&teamId=' + teamId,
                type: 'POST',
                dataType: 'json',
                cache: false
            }).done(function(data) {
                addChannel(data);
                dfd.resolve(data);
            }).fail(function(err) {
                dfd.reject(err);
            });

            return dfd;
        }

        function addChannel(channel) {
            channelMap[channel.channelId] = channel;
            rebuildChannelData();
        }

        function removeChannel(channel) {
            delete channelMap[channel.channelId];
            rebuildChannelData();
        }

        function onChange(fn) {
            listeners[fn] = fn;
        }

        function offChange(fn) {
            delete listeners[fn];
        }

        function getChannelById(channelId) {
            return channelMap[channelId];
        }

        function getChannels() {
            return channels;
        }

        var channelService = {
            createChannel: createChannel,
            addChannel: addChannel,
            removeChannel: removeChannel,
            getChannelById: getChannelById,
            getChannels: getChannels,
            onChange: onChange,
            offChange: offChange
        };

        var channelsAvailablePromise = (function () {
            var channelLoader = $.Deferred();
            if (!teamId || (!isMigration && !loggedIn)) {
                channelLoader.resolve(channelService);
            } else {
                var restPath = isMigration ? 'migration' : 'channels';
                $.ajax({
                    url: wrmContextPath() + '/slack/' + restPath + '?projectKey=' +
                        (_.isString(projectKey) ? projectKey : "") + "&teamId=" + teamId,
                    dataType: 'json',
                    cache: false
                }).done(function (data) {
                    channelMap = _.object(_.map(data, function (item) {
                        return [item.channelId, item];
                    }));
                    rebuildChannelData();
                    channelLoader.resolve(channelService);
                }).fail(function (err) {
                    channelLoader.reject(err);
                });
            }
            return channelLoader.promise();
        })();
        return channelsAvailablePromise;
    });

    var promisesCache = {};
    var promiseBuilder = function (teamId, loggedIn, projectKey) {
        if (promisesCache[teamId] === undefined) {
            promisesCache[teamId] = channelServicePromise(teamId, loggedIn, projectKey);
        }

        return promisesCache[teamId];
    };
    promiseBuilder.reset = function(teamId) {
        if (teamId) {
            delete promisesCache[teamId];
        } else {
            promisesCache = {};
        }
    };

    return {
        channelServicePromise: promiseBuilder,
        MIGRATION: MIGRATION
    };
});
