require([
    "jira/util/formatter",
    "jquery",
    "jira/util/data/meta",
    "jira/util/events",
    "jira/util/events/types",
    "wrm/context-path",
    "slack/feature/issuepanel/issuepanel-view"
], function (
    formatter,
    $,
    Meta,
    Events,
    Types,
    wrmContextPath,
    IssuePanelView
) {

    /**
     * We track any hash actions to be able to know if we are being redirected
     * from other place
     */
    function evaluateImmediateActions() {
        var hash = window.location.hash;

        if (hash === "#choose-channel") {
            $("#slack-select-dedicated-channel").click();
            cleanHash();
        } else if (hash === "#open-issue-mentions") {
            $("#slack-issue-mentions-list").click();
            cleanHash();
        } else if(hash === "#delete-dedicated-channel") {
            $(".trash-dedicated-channel").click();
        }
    }

    function cleanHash() {
        window.location.hash = "";
    }

    function createIssuePanelView() {
        return new IssuePanelView({
            el: $("#slack-issue-panel-channels-container")
        });
    }

    var issuePanelView = null;
    function init() {
        getTemplate();

        Events.bind(Types.ISSUE_REFRESHED, function () {
            if (issuePanelView !== null){
                issuePanelView.close();
            }
            getTemplate();
        });
    }

    function getTemplate() {
        var issueKey = Meta.get("issue-key");
        var $issuePanel = $("#slack-issue-panel");
        if (!issueKey || !$issuePanel.length) {
            return;
        }
        var $spinner = $("#slack-issue-panel-spinner");
        var $errors = $("#slack-issue-panel-errors");
        $spinner.spin();
        return $.ajax({
            url: wrmContextPath() + "/slack/issuepanel/data/" + issueKey,
            cache: false,
            dataType: 'json',
            type: "GET"
        }).done(function (data) {
            var template = JIRA.Templates.Slack.Project.IssuePanel.slackPanel(data);
            $issuePanel.html(template);

            issuePanelView = createIssuePanelView();
            issuePanelView.on("ready", evaluateImmediateActions);
        }).fail(function () {
            $errors.append(formatter.I18n.getText("jira.plugins.slack.viewissue.panel.error.getting.data"));
        }).always(function() {
            $spinner.spinStop();
        });
    }

    $(function () {
        init();
    });
});
