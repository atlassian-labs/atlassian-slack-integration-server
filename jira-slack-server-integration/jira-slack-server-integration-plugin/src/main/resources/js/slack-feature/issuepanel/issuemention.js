define("slack/feature/issuepanel/issuemention",
[
    "jquery",
    "backbone"
], function (
    $,
    Backbone
) {

    var IssueMention = Backbone.Model.extend({

        initialize: function(options) {

        },

        url: function() {
            return AJS.contextPath() + '/slack/issue-mentions/' + this.get("issue_key") + '/channels';
        },

        parse: function(data) {
            this.set("mentionCount", data.mentionCount || 0);
            this.set("channelCount", data.channelCount || 0);
        }
    });

    return IssueMention;

});
