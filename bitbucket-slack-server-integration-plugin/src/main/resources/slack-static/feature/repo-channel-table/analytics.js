define('bitbucket-plugin-slack/feature/repo-channel-table/analytics', [
    'jquery',
    'lodash',
    'bitbucket/internal/util/analytics',
    'bitbucket/util/state',
    'exports'
], function (
    $,
    _,
    analytics,
    pageState,
    exports
) {
    'use strict';

    var EVENT_PREFIX = 'slack.mapping.';

    /**
     * Fires an analytics event for the repo mapping table.
     *
     * @param {String} name             - the name of the event to fire.
     * @param {RepoChannelMapping} mapping - Representing the mapping being altered.
     * @param {Object} extraAttributes  - Extra attributes to include in the event.
     */
    function triggerAnalytics(name, mapping, extraAttributes) {
        var isRepoSettings = !!pageState.getRepository();
        var attributes = _.extend({
            'repository.id': mapping.repoId,
            'channel.id': mapping.channelId,
            'location': isRepoSettings ? 'repo-settings' : 'global-settings'
        }, extraAttributes || {});

        analytics.add(EVENT_PREFIX + name, attributes);
    }

    /**
     * Extracts the mapping information from {@code $tbody}
     *
     * @param {jQuery} $tbody - The <tbody> for the row that has been changed.
     * @returns {Object.<String, boolean>} A map with the values that have changed.
     */
    function extractMappingInformation($tbody) {
        var notifications = {};
        $tbody.find('.notification-item').each(function (i, input) {
            notifications[$(input).attr('data-key')] = input.checked;
        });
        return notifications;
    }

    /**
     * Fires an analytics event for when a mapping has been added.
     *
     * @param {RepoChannelMapping} mapping - Representing the mapping being altered.
     */
    exports.mappingAdded = triggerAnalytics.bind(null, 'added');

    /**
     * Fires an analytics event for when a mapping has been changed.
     *
     * @param {RepoChannelMapping} mapping - Representing the mapping being altered.
     * @param {jQuery} $tbody           - The tbody of the row being altered.
     */
    exports.mappingsChanged = function mappingsChanged(mapping, $tbody) {
        triggerAnalytics('changed', mapping, extractMappingInformation($tbody));
    };

    /**
     * Fires an analytics event for when a mapping has been deleted.
     *
     * @param {RepoChannelMapping} mapping - Representing the mapping being altered.
     */
    exports.mappingDeleted = triggerAnalytics.bind(null, 'deleted');

});
