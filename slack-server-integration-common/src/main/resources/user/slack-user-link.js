require([
    'jquery',
    'wrm/context-path'
], function (
    $,
    wrmContextPath
) {

    function getRedirect() {
        var location = window.location;
        var contextPath = wrmContextPath();
        var path = location.pathname;
        if(path.indexOf(contextPath) === 0) {
            path = path.substr(contextPath.length);
        }
        var query = location.search;
        if(query) {
            query = query.substr(1); // remove leading ?
        }
        return {
            path: path,
            query: query
        };
    }

    function doRedirect(slackOauthUri) {
        window.location.assign(slackOauthUri);
    }

    function getFragmentFrom(container){
        if(container){
            var redirectHash = container.attr("href");
            if(redirectHash !== undefined &&
                    redirectHash.length > 1 &&
                    redirectHash.indexOf("#") === 0){
                return redirectHash.substr(1); // We remove the hash
            }
        }
        return "";
    }

    var centerSpinner = function() {
        var center = $("<div class=\"center-spinner\"></div>");
        center.css({
            position: "fixed",
            left: "50%",
            top: "50%"
        });
        $("body").append(center);
        center.spin("large");
    };

    /**
     * Starts the user link process.
     *
     * @param e the event if initiated by an event (optional).
     *          Meant to preventing the default behaviour of the event.
     */
    var startUserLink = function (e) {
        var redirect = getRedirect();
        var configuration = $('#slack-channel-configuration');
        var link = $((e || {}).target);
        var teamId = configuration.data('slack-team-id') || link.data('slack-team-id');
        if (teamId) {
            var postData = {
                url: wrmContextPath() + '/rest/slack/latest/oauth/begin/' + teamId,
                type: "POST",
                dataType: 'text',
                contentType: 'application/json',
                data: JSON.stringify({
                    redirect: redirect.path,
                    redirectQuery: redirect.query,
                    redirectFragment : e && e.target && getFragmentFrom($(e.target))
                })
            };

            $.ajax(postData)
                .done(doRedirect)
                .fail(function(jqXHR, status, error) {
                    console.log("Error requesting redirect URL", error);
                    $("div.center-spinner").remove();
                });

            e && e.preventDefault && e.preventDefault();

            centerSpinner();
        }
    };

    $(document).on('click', '.slack-user-link', startUserLink);

    var userUnlink = function (e) {
        var configuration = $('#slack-channel-configuration');
        var link = $(e.target);
        var teamId = configuration.data('slack-team-id') || link.data('slack-team-id');
        if (teamId) {
            var deleteData = {
                url: wrmContextPath() + '/rest/slack/latest/oauth/' + teamId,
                type: 'DELETE',
                contentType: 'application/json'
            };
            $.ajax(deleteData)
                .always(function() {
                    window.location.reload();
                });
            e && e.preventDefault && e.preventDefault();

            centerSpinner();
        }
    };

    $(document).on('click', '.slack-user-unlink', userUnlink);

    // exports
    window.Slack = window.Slack || {};
    window.Slack.UserLink = window.Slack.UserLink || {};
    window.Slack.UserLink.linkUser = startUserLink;
    window.Slack.UserLink.unlinkUser = userUnlink;

});
