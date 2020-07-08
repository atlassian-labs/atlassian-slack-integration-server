require(['jquery', 'jira/lib/class', 'wrm/context-path'], function($, Class, wrmContextPath) {

    var NotificationPreview = Class.extend({

        TIMEOUT_MS: 100,

        init: function (options) {
            var instance = this;

            this.$source = options.source;
            this.$preview = options.preview;

            this.lastSourceValue = this.$source.val();

            // Render preview chrome
            this.$preview.html(JIRA.Templates.Slack.Notification.notificationPreview());

            // Poll the key field for updates
            this.$source.focus($.proxy(this._bindSourceHook, this));
            this.$source.blur($.proxy(this._unbindHook, this));

            // Allow for the preview to be triggered
            this.$source.bind("contentModified", function() {
                instance.renderPreview();
            });

            // Render preview immediately
            this.renderPreview();
        },

        renderPreview: function() {
            var message = this.$source.val();
            $.ajax({
                url: wrmContextPath() + "/slack/message/render",
                type: "POST",
                contentType: "application/json",
                data: JSON.stringify({
                    message: message
                })
            }).done(function(data) {
                var html = slackdown.parse(data.message).replace(/[\n]/g, "<br/>");
                $('.slack-message').html(html);
            }).fail(function(resp) {
                $('.slack-message').html(AJS.I18n.getText('slack.notification.preview.error') + '<br/>' + resp.responseText);
            });
        },

        onSourceTimeout: function() {
            // Only re-render if the value has changed
            var sourceValue = this.$source.val();
            if (sourceValue !== this.lastSourceValue) {
                this.renderPreview();
                this.lastSourceValue = sourceValue;
            }
        },

        _bindSourceHook: function(e) {
            this._bindHook(e, this.onSourceTimeout);
        },

        _bindHook: function(e, func) {
            var instance = this, el = $(e.target), hook;
            hook = function() {
                instance._unbindHook(e);
                func.apply(instance);
                if (el.is(":visible")) {
                    el.data("checkHook", setTimeout(hook, instance.TIMEOUT_MS));
                }
            };
            if (!el.data("checkHook")) {
                el.data("checkHook", setTimeout(hook, 0));
            }
        },

        _unbindHook: function(e) {
            var el = $(e.target);
            clearTimeout(el.data("checkHook"));
            el.removeData("checkHook");
        }
    });

    AJS.toInit(function() {
        var $source = $(".slack-notification-preview-source");
        if ($source.length) {
            new NotificationPreview({
                source: $source,
                preview: $(".slack-notification-preview")
            });
        }
    });
});
