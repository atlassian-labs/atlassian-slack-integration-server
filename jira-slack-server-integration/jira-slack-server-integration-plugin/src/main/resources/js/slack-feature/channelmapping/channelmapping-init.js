(function (
    $,
    Mapping,
    Mappings,
    MappingView,
    ChannelMappingPageView
) {
    function init() {
        var mappings = new Mappings();

        var $channelConfigurations = $("#slack-channel-configuration");

        _.each($channelConfigurations.find(".slack-channel-config"), function(i) {
            var $channelConfig = $(i);
            var mapping = new Mapping({
                teamId: $channelConfig.data("team-id"),
                teamName: $channelConfig.data("team-name"),
                channelId: $channelConfig.data("channel-id"),
                channelName: $channelConfig.data("channel-name"),
                configurationGroupId: $channelConfig.data("configuration-group-id"),
                projectKey: $channelConfig.data("project-key"),
                projectName: $channelConfig.data("project-name"),
                projectId: $channelConfig.data("project-id")
            });
            new MappingView({
                model: mapping,
                el: $channelConfig
            });

            mappings.add(mapping);
        }, this);

        new ChannelMappingPageView({
            el: $channelConfigurations,
            collection: mappings
        });
    }

    $(function() {
        init();
    });
})(
    require("jquery"),
    require("slack/feature/channelmapping/mapping"),
    require("slack/feature/channelmapping/mapping-collection"),
    require("slack/feature/channelmapping/mapping-view"),
    require("slack/feature/channelmapping/channelmapping-page")
);
