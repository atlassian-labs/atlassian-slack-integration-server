require([
    'jquery',
    'wrm/context-path'
], function (
    $,
    wrmContextPath
) {
    function handleAjaxComplete(event, jqXhr, settings) {
        if (/userinfopopup\.action/.test(settings.url)) {
            $('.vcard').each(function () {
                var $vcard = $(this);
                var username = $(".userLogoLink", $vcard).attr('data-username');
                if (username && !$vcard.hasClass('slack-status-applied')) {
                    $vcard.addClass('slack-status-applied');
                    addSlackLinkToCard($vcard, username);
                }
            });
        }
    }

    function addSlackLinkToCard($vcard, username) {
        $.getJSON(wrmContextPath() + '/rest/slack/latest/users/' + encodeURI(username), function (response) {
            if (!response) {
                return;
            }

            var $slackIcon;
            if (response.length === 1) {
                $slackIcon = $(Confluence.Templates.Slack.User.slackIcon({
                    slackUsers: response
                }));

                // When user link is in a inline popup (.ajs-content-hover class), clicks are not propagated
                // to our root document node listener due to a Confluence script, so delegating listener does not work
                // So here a click listener added directly to link node
                $slackIcon.click(handleSlackUserLinkClicked);
            } else if (response.length > 1) {
                $slackIcon = $(Confluence.Templates.Slack.User.slackIcon({
                    slackUsers: response
                }));
                $slackIcon.click(function(event) {
                    event.preventDefault();
                });

                var slackSectionMarkup = Confluence.Templates.Slack.User.teamLinks({
                    slackUsers: response
                });
                var $slackSection = $(slackSectionMarkup);
                var $popupMenu = $vcard.closest('.contents').find('#user-popup-menu-admin-secondary');
                $popupMenu.append($slackSection);
                $popupMenu.find('.slack-user-link-url').click(handleSlackUserLinkClicked);
            }
            $vcard.find('h4').append($slackIcon);
        });
    }

    function handleSlackUserLinkClicked(event) {
        // Manually opens DM
        window.Slack.SlackLinks.openSlackUserUrl(event);
        // prevent handling if delegating listener in case Confluence scripts will change in future
        event.stopPropagation();
    }

    $(function () {
        $('body').bind('ajaxComplete', handleAjaxComplete);
    });
});
