define("slack/feature/issuepanel/dedicatedchannel",
[
    "jquery",
    "backbone"
], function (
    $,
    Backbone
) {

    var DedicatedChannel = Backbone.Model.extend({

        url: function () {
            return AJS.contextPath() + '/slack/dedicatedchannel/';
        },

        destroy: function () {
            var options = {
                data: JSON.stringify(this.toJSON()),
                contentType: 'application/json'
            };

            this.id = -1;
            return Backbone.Model.prototype.destroy.call(this, options);
        }
    });

    return DedicatedChannel;

});
