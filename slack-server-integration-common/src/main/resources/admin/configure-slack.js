require([
    'jquery'
], function (jQuery) {
    'use strict';

    jQuery(function () {
        "use strict";

        /**
         * Slack configuration state
         * @type {String} The current configuration state of slack
         */
        var state = jQuery("#slack-server-integration-configuration").val();

        // If the state element is not present, do not load the rest as this is the wrong page
        if (state) {
            var baseUrl = jQuery("#slack-base-url").val();
            var restBaseUrl = baseUrl + "/rest/slack/latest";

            /**
             * Misc other values/imports
             */
            var Templates = Slack.Templates.Configuration;
            var panel = jQuery("#slack-configuration-panel");

            var resources = {
                uninstalling: jQuery("#slack-server-resource-uninstalling").val()
            };

            /**
             * All views for this page
             */
            var Views = (function () {
                var confirmDialog;

                return {
                    /**
                     * Load error template
                     * @param msg {String} Error message to display
                     */
                    error: function (msg) {
                        panel.html(Templates.error({msg: msg}));
                    },
                    /**
                     * Load uninstall error template
                     */
                    uninstallError: function (msg) {
                        if (msg.status === 401) {
                            panel.html(Templates.uninstallPermissionError());
                        } else {
                            panel.html(Templates.uninstallConnectionError());
                        }
                    },
                    /**
                     * Confirms removal of the link to Slack
                     *
                     * @return promise resolved if the user confirms removed, rejected if they cancel
                     */
                    confirmUninstall: function () {
                        var deferred = jQuery.Deferred();

                        if (!confirmDialog) {
                            // TODO CONFDEV-28519 ADG 2 - new confirmation dialog (and remove related CSS)
                            confirmDialog = new AJS.Dialog(600, 300, "slack-remove-link-dialog");
                            confirmDialog.addHeader(AJS.I18n.getText("plugins.slack.admin.disconnect.team.header"), "remove-warning");
                            confirmDialog.addPanel("Message Panel", Templates.removeLinkMessage());
                            confirmDialog.addButton(AJS.I18n.getText("plugins.slack.admin.disconnect.team.confirm"), function () {
                                confirmDialog.hide();
                                Views.uninstall();
                                deferred.resolve();
                            });
                            confirmDialog.addCancel(AJS.I18n.getText("plugins.slack.admin.disconnect.team.cancel"), function () {
                                confirmDialog.hide();
                                deferred.reject();
                            });
                        }
                        confirmDialog.show();

                        return deferred.promise();
                    },
                    uninstall: function () {
                        panel.html(Templates.uninstalling({
                            uninstallingImage: resources.uninstalling
                        }));
                        jQuery('#slack-uninstalling .slack-install-message').spin('large');
                    }
                };
            })();

            /**
             * Remote APIs service
             */
            var Remote = {
                /**
                 * Begin a server request to test if slack API is available from Jira Server
                 * @returns {*}
                 */
                serverPing: function () {
                    return jQuery.ajax(restBaseUrl + "/connection/can-reach-slack", {
                        type: "POST",
                        dataType: 'json',
                        contentType: 'application/json',
                    });
                },

                /**
                 * Remove link with the given id
                 * @param teamId
                 * @returns {*}
                 */
                remove: function (teamId) {
                    return jQuery.ajax({
                        url: restBaseUrl + "/connection/" + teamId,
                        type: "DELETE",
                        dataType: 'json',
                        contentType: 'application/json',
                        data: JSON.stringify({})
                    })
                }
            };

            /**
             * Code related to the various actions on this page
             */
            var Actions = {
                /**
                 * Setup the initial state of the screen
                 * @param state
                 */
                initialize: function (state) {
                    switch (state) {
                        case "installed":
                            Actions.installed();
                            break;

                        case "configure":
                        default:
                            Actions.configure();
                    }
                },

                /**
                 * Utility function to redirect the client's browser (current url is not in history)
                 * @param url
                 */
                redirect: function (url) {
                    window.location.replace(url);
                },

                /**
                 * Utility function to redirect the client's browser while keeping the current url in the browser history.
                 */
                navigate: function (url) {
                    window.location.assign(url);
                },

                /**
                 * Load the configuration screen
                 *
                 * Trigger a server ping, if successful request
                 *  installation information and redirect to slack
                 *  plugin installation.
                 */
                configure: function () {
                    Remote.serverPing()
                        .done(function () {
                            jQuery("#slack-install").prop("disabled", false);
                        })
                        .fail(function () {
                            jQuery("#slack-server-failure").removeClass("hidden");
                        })
                        .always(function () {
                            jQuery("#slack-loading").hide();
                        });

                    jQuery("#slack-install").click(function (e) {
                        e.preventDefault();
                        Actions.navigate(baseUrl + "/plugins/servlet/slack/configure?action=add")
                    });
                },

                /**
                 * Load the installed screen
                 */
                installed: function () {
                    jQuery("#slack-uninstall").click(function (e) {
                        e.preventDefault();
                        if (jQuery(this).attr('aria-disabled')) {
                            return;
                        }
                        var configuration = jQuery('#slack-channel-configuration');
                        var teamId = configuration.data('slack-team-id');
                        Views.confirmUninstall().done(function () {
                            Remote.remove(teamId)
                                .done(function() {
                                    Actions.navigate(baseUrl + "/plugins/servlet/slack/configure")
                                })
                                .fail(Views.uninstallError);
                        });

                    });
                },
            };

            Actions.initialize(state);
        }
    }
)});
