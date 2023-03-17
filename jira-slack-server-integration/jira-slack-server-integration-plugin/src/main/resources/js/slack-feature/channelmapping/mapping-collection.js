define("slack/feature/channelmapping/mapping-collection",
[
    "slack/backbone",
    "slack/feature/channelmapping/mapping"
], function (
    Backbone,
    Mapping
) {
    var Mappings = Backbone.Collection.extend({
        model: Mapping,

        comparator: function(mapping1, mapping2) {
            var mapping1ProjectName = mapping1.get("projectName").toString().toLocaleLowerCase();
            var mapping2ProjectName = mapping2.get("projectName").toString().toLocaleLowerCase();
            var projectNameComparison = mapping1ProjectName.localeCompare(mapping2ProjectName);
            if (projectNameComparison !== 0) {
                return projectNameComparison;
            }

            var mapping1ChannelName = mapping1.get("channelName").toString().toLocaleLowerCase();
            var mapping2ChannelName = mapping2.get("channelName").toString().toLocaleLowerCase();
            var channelNameComparison = mapping1ChannelName.localeCompare(mapping2ChannelName);
            return channelNameComparison;
        }
    });

    return Mappings;
});
