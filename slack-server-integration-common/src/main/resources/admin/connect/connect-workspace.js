require([
    'jquery',
    'wrm/context-path'
], function (
    jQuery,
    wrmContextPath
) {
    'use strict';

    var contextPath = wrmContextPath();

    jQuery(function () {
        "use strict";

        var basicOption = jQuery("#basic-connection-type");
        var customOption = jQuery("#custom-connection-type");
        var credentialsTextArea = jQuery("#slack-credentials");
        var goToSlackButton = jQuery("#slack-go-to");
        var submitButton = jQuery("#slack-submit-credentials");
        var pasteButton = jQuery("#slack-paste-credentials");
        var slackLoading = jQuery("#slack-loading");
        var slackFailure = jQuery("#slack-server-failure");

        var restBaseUrl = contextPath + "/rest/slack/latest";

        jQuery(".active-tooltip").tooltip();

        /**
         * Remote APIs service
         */
        var Remote = {
            install: function () {
                var teamId = submitButton.data('team-id');
                var data = isBasic() ? credentialsTextArea.val() : JSON.stringify({
                    'client_id': jQuery('#client-id').val(),
                    'client_secret': jQuery('#client-secret').val(),
                    'verification_token': jQuery('#verification-token').val(),
                    'signing_secret': jQuery('#signing-secret').val(),
                    'access_token': jQuery('#access-token').val(),
                    'bot_access_token': jQuery('#bot-access-token').val(),
                    'custom': true
                });
                return jQuery.ajax(restBaseUrl + "/connection" + (teamId ? '/' + teamId : ''), {
                    type: "POST",
                    dataType: 'json',
                    contentType: 'application/json',
                    data: data
                })
            }
        };

        var isBasic = function() {
            return !jQuery('.basic-config.hidden').length;
        };

        /**
         * Code related to the various actions on this page
         */
        var Actions = {

            getClipboardText: function() {
                try {
                    return window.navigator.clipboard.readText();
                } catch(e) {
                    try {
                        return Promise.resolve(window.clipboardData.getData('Text'));
                    } catch(e) {
                        return Promise.reject();
                    }
                }
            },

            pasteClipboard: function() {
                Actions.getClipboardText().then(function(text) {
                    var clipboardContent = jQuery.trim(text || "");
                    if (clipboardContent && clipboardContent.length) {
                        try {
                            var jsonContent = JSON.parse(clipboardContent);
                            credentialsTextArea.val(JSON.stringify(jsonContent, undefined, 4));
                        } catch (e) {
                            credentialsTextArea.val(clipboardContent);
                        }
                    }
                }).catch(function() {});
            },

            /**
             * Setup the initial state of the screen
             * @param state
             */
            initialize: function (state) {

                // prevents enter key to submit native form
                jQuery('.custom-config form').on('submit', function(e) {
                   e.preventDefault();
                });

                // switchers between basic and custom/advanced
                basicOption.on('click', function() {
                    jQuery('.basic-config').removeClass('hidden');
                    jQuery('.custom-config').addClass('hidden');
                });
                customOption.on('click', function() {
                    jQuery('.custom-config').removeClass('hidden');
                    jQuery('.basic-config').addClass('hidden');
                });

                goToSlackButton.on('click', function() {
                    if (isBasic()) {
                        window.open("https://slack.com/apps/" + AJS.I18n.getText('slack.blueprint.app.id'));
                    } else {
                        var appId = goToSlackButton.data('app-id') || "";
                        window.open("https://api.slack.com/apps" + (appId ? "/" + appId : "?new_app=1"));
                    }
                });

                submitButton.on('click', function() {
                    if (submitButton.attr('aria-disabled')) {
                        return;
                    }
                   Actions.install();
                });

                pasteButton.on('click', function() {
                    if (pasteButton.attr('aria-disabled')) {
                        return;
                    }
                   Actions.pasteClipboard();
                });

                setInterval(function() {
                    if (!isBasic()) {
                        return;
                    }
                    var content = jQuery.trim(credentialsTextArea.val() || "");
                    if (!content || !content.length) {
                        Actions.getClipboardText().then(function(text) {
                            var clipboardContent = jQuery.trim(text || "");
                            if (clipboardContent && clipboardContent.length) {
                                try {
                                    var jsonContent = JSON.parse(clipboardContent);
                                    if (jsonContent.client_id && jsonContent.client_secret
                                        && jsonContent.verification_token && jsonContent.app_id
                                        && jsonContent.user_id && jsonContent.team_id
                                        && jsonContent.access_token && jsonContent.bot_user_id) {
                                        credentialsTextArea.val(JSON.stringify(jsonContent, undefined, 4));
                                    }
                                } catch (e) {
                                    //nothing
                                }
                            }
                        }).catch(function() {});
                    }
                }, 500);
            },

            navigate: function (url) {
                window.location.assign(url);
            },

            install: function () {
                slackLoading.removeClass("hidden");
                slackLoading.show();
                slackFailure.hide();

                pasteButton.attr('aria-disabled', 'true');
                submitButton.attr('aria-disabled', 'true');

                Remote.install()
                    .done(function (data) {
                        window.location = (contextPath + "/plugins/servlet/slack/configure?recentInstall=" + data.teamId);
                    })
                    .fail(function (e) {
                        slackFailure.find('.details')
                            .text((e.status !== 500 && e.responseText) || AJS.I18n.getText('plugins.slack.admin.connect.workspace.error.generic'));
                        slackFailure.removeClass("hidden");
                        slackFailure.show();
                    })
                    .always(function () {
                        slackLoading.hide();
                        pasteButton.removeAttr('aria-disabled');
                        submitButton.removeAttr('aria-disabled');
                    });
            },

        };

        Actions.initialize();
    }
)});
