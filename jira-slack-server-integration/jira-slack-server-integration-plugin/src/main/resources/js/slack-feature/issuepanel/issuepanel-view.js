define("slack/feature/issuepanel/issuepanel-view",
[
    "jquery",
    "backbone",
    "slack/base",
    "slack/feature/issuepanel/dedicatedchannel",
    "slack/feature/issuepanel/dedicatedchannel-view",
    "slack/feature/issuepanel/issuemention",
    "slack/feature/issuepanel/issuemention-view",
    "slack/js-cookies"
], function (
    $,
    Backbone,
    Slack,
    DedicatedChannel,
    DedicatedChannelView,
    IssueMention,
    IssueMentionView,
    jsCookies
) {
        var slackLinkClickedKey = "slack.inapp.links.first.clicked";
        var slackNativeLinksFeatureDiscovery = "slack.inapp.links";
        var cookieExpiresInDays = 1;

        var IssuePanelView = Slack.View.extend({
        events: {
            "click #slack-issue-panel-configure-button": "configure",
            "click #slack-issue-panel-hide": "dismissPanel"
        },

        initialize: function(options) {
            this.initDedicatedView();
            this.initIssueMentionView();
            this.initPanelSettings();
            this.initPanelUserAccounts();
        },

        initDedicatedView: function() {
            var issuePanelDetails = this.getIssuePanelDetails();
            var dedicatedChannelOptions = {};
            if (issuePanelDetails.dedicatedChannelId) {
                dedicatedChannelOptions = {
                    issueKey: issuePanelDetails.issueKey,
                    teamId: issuePanelDetails.dedicatedTeamId,
                    channelId: issuePanelDetails.dedicatedChannelId
                };
            }
            var model = new DedicatedChannel(dedicatedChannelOptions);

            var options = _.extend(issuePanelDetails, {
                el: this.$("#slack-dedicated-channel-section"),
                model: model
            });
            this.dedicatedChannelView = new DedicatedChannelView(options);
        },

        initIssueMentionView: function() {
            var issuePanelDetails = this.getIssuePanelDetails();
            var options = _.extend({
                el: this.$("#slack-mentioned-channels-section"),
                model: new IssueMention({ issue_key: issuePanelDetails.issueKey })
            }, issuePanelDetails);
            this.issueMentionView = new IssueMentionView(options);

            this.issueMentionView.on("ready", this.ready, this);
        },

        close: function() {
            this.dedicatedChannelView.close();
            this.issueMentionView.close();
        },

        ready: function() {
            this.trigger("ready");
        },

        getIssuePanelDetails: function() {
            var $panelContainer = this.$el;
            var loggedIn = $panelContainer.data("logged-in");
            var canAssignChannel = $panelContainer.data("can-assign-channel");
            var dedicatedChannelId = $panelContainer.data("dedicated-channel-id");
            var dedicatedTeamId = $panelContainer.data("dedicated-team-id");
            var isAdmin = AJS.Meta.get("is-admin");
            var issueKey = AJS.Meta.get("issue-key");
            return {
                loggedIn: loggedIn,
                canAssignChannel: canAssignChannel,
                isAdmin: isAdmin,
                dedicatedChannelId: dedicatedChannelId,
                dedicatedTeamId: dedicatedTeamId,
                issueKey: issueKey
            };
        },

        dismissPanel: function(e) {
            e.preventDefault();

            var $panelContainer = this.$el;
            var projectKey = $panelContainer.data("project-key");
            var self = this;
            $.ajax({
                type: "POST",
                url: AJS.contextPath() + '/slack/issuepanel/hide?projectKey=' + projectKey,
                dataType: 'json',
                cache: false
            }).done(function() {
                self.close();
                $("#slack-viewissue-panel").remove();
            }).fail(function() {

            });
        },

        configure: function(e) {
            var target = $(e.currentTarget);
            window.location = target.data("configure-url");
        },

        initPanelSettings: function() {

            // We only show the panel if the user is a MAC user
            if(navigator.platform.toUpperCase().indexOf('MAC') >= 0 && AJS.Meta.get("issue-key") !== undefined){

                var $panel = $("#slack-panel-settings");
                $panel.removeClass("hidden");
                $panel.click(function (event) {
                    event.preventDefault();
                    showDialog($(event.target), false);
                });

                // If someone enabled/disabled the advanced settings
                $(document).on("click", "#slack-inapp-links", function (event) {
                    var enabled = $(event.target).is(":checked");
                    jsCookies.set("SLACK_NATIVE_LINK_ENABLED", enabled);

                    var analyticEvent  = "jira.slack.integration.issuepanel.native.link." + (enabled ? "enabled" : "disabled");
                    AJS.trigger('analyticsEvent', {name: analyticEvent});
                });

                $(document).on("click", ".slack-team-link-url", function () {
                    setUserClickedALink();
                });

                $(document).on("click", ".slack-channel-link-url", function () {
                    setUserClickedALink();
                });

                $(document).on("click", ".slack-user-link-url", function () {
                    setUserClickedALink();
                });

                validateFirstTime($panel);
            }

            var confirmDialog;

            function showDialog($target, firstTime) {
                if (!confirmDialog) {
                    // we need to remove any existing dialog, which may have been left behind from previous issues
                    // when a new issue is Ajax loaded
                    AJS.$("#inline-dialog-slack-link-dialog").remove();

                    confirmDialog = AJS.InlineDialog($target, "slack-link-dialog",
                            function (content, trigger, showPopup) {
                                var params = {isAppLinkEnable: jsCookies.get("SLACK_NATIVE_LINK_ENABLED") === "true"};
                                content.html(JIRA.Templates.Slack.Config.LinkSettings.enableInAppLinksForSlack(params));
                                showPopup();
                                return false;
                            }, {
                                hideDelay: null
                            });
                }
                confirmDialog.show();

                var url = AJS.contextPath() + '/rest/api/2/mypreferences?key=';

                // Now we set the variable so the feature discovery ends...
                $.ajax({
                    url: url + slackNativeLinksFeatureDiscovery,
                    contentType: 'application/json',
                    cache: false,
                    type: "PUT",
                    data: "true"
                });

                saveCookie(slackNativeLinksFeatureDiscovery, true);
            }


            /**
             * We clicked a link so we are going to store this in a cookie and a preference, and in the next
             * refresh if the user did not get the feature discovery then we show it.
             */
            function setUserClickedALink() {
                var linkClicked = getCookieForUser(slackLinkClickedKey);
                if(linkClicked === "undefined" || linkClicked === "false"){
                    saveCookie(slackLinkClickedKey , true);
                    var url = AJS.contextPath() + '/rest/api/2/mypreferences?key=' + window.encodeURIComponent(slackLinkClickedKey);
                    $.ajax({ url: url, contentType: 'application/json', cache: false, type: "PUT", data: "true"});
                }
            }

            /**
             * We save the cookie with the user id, to guarantee that
             * we can login/logout without having problems
             * @param cookie the cookie
             * @param value the value to store
             */
            function saveCookie(cookie, value){
                var userName = AJS.Meta.get("remote-user")
                var cookieName = cookie + "." + userName;
                jsCookies.set(cookieName, value, cookieExpiresInDays);
            }

            function getCookieForUser(cookie){
                var userName = AJS.Meta.get("remote-user")
                var cookieName = cookie + "." + userName;
                return jsCookies.get(cookieName);
            }

            /**
             * First we check the cookies, if the cookies say true/true then we already checked and we forget about this
             * If no cookies are set then we ask first if something was clicked, if it was then we check if it was shown or not
             * Depending on that we show the popup.
             * This is the way to verify that the user is using the functionality and showing the things they need.
             * @param $panel the panel
             */
            function validateFirstTime($panel) {

                var url = AJS.contextPath() + '/rest/api/2/mypreferences?key=';

                // If both cookies are true it means that the user clicked a link, and we already showed
                // the popup. This is much better than asking every time the issue loads for this 2 user preferences
                var linkClicked = getCookieForUser(slackLinkClickedKey);

                if (linkClicked === "true") {

                    if (getCookieForUser(slackNativeLinksFeatureDiscovery) === "true") {
                        return;
                    }

                    $.ajax({
                        url: url + slackNativeLinksFeatureDiscovery,
                        dataType: 'json',
                        cache: false
                    }).fail(function (data) {

                        if (data.status === 404) {

                            showDialog($panel, true);

                        }
                    }).success(function (onsuccess) {
                        // This happened in other browser or other session
                        saveCookie(slackNativeLinksFeatureDiscovery, true)
                    });

                } else if (linkClicked === undefined) { // Only if we don't know anything about the link we evaluate

                    $.ajax({
                        url: url + slackLinkClickedKey,
                        dataType: 'json',
                        cache: false
                    }).success(function (data) {

                        saveCookie(slackLinkClickedKey, true);
                        validateFirstTime($panel); //We do the logic again

                    }).fail(function (failure) {
                        saveCookie(slackLinkClickedKey, false);
                    });
                }
            }
        },

        initPanelUserAccounts: function() {

            var panel = $("#slack-remaining-accounts");
            var firstTime = true;
            panel.click(function(event) {
                event.preventDefault();
                showDialog($(event.target));

                if(firstTime){
                    AJS.trigger('analyticsEvent', {name: 'jira.slack.integration.user.native.link.discovery'});
                    firstTime = false;
                }
            });

            var dialog;
            function showDialog($target) {
                if (!dialog) {
                    // remove stale dialog if page is reloaded using Ajax
                    AJS.$("#inline-dialog-slack-user-account-link-dialog").remove();

                    dialog = AJS.InlineDialog($target, "slack-user-account-link-dialog",
                        function (content, trigger, showPopup) {
                            var nonConfirmedLinks = $("input[name='non-confirmed-user-link-account']")
                                .map(function() {
                                    return {
                                        teamId: $(this).data('team-id'),
                                        teamName: $(this).data('team-name')
                                    };
                                })
                                .get();
                            content.html(JIRA.Templates.Slack.Config.LinkSettings.slackRemainingLinkAccount({
                                nonConfirmedLinks: nonConfirmedLinks
                            }));
                            showPopup();
                            return false;
                        }, {
                            hideDelay: null
                        });
                }
                dialog.show();
            }
        }
    });

    return IssuePanelView;
});
