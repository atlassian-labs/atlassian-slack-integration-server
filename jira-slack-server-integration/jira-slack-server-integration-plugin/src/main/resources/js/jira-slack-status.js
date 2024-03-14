require([
    'jquery',
    'wrm/context-path'
], function (
    $,
    wrmContextPath
) {
    function handleAjaxComplete(event, jqXhr, settings) {
        if (jqXhr.responseText && /user\-hover\-info/.test(jqXhr.responseText)) {
            $('.user-hover-info').each(function () {
                var $vcard = $(this);
                var username = $("#avatar-image-link", $vcard).attr('title');
                if (username && !$vcard.hasClass('slack-status-applied')) {
                    $vcard.addClass('slack-status-applied');
                    addSlackLinkToCard($vcard, username);
                }
            });
        }
    }

    function addSlackLinkToCard($vcard, username) {
        $.getJSON(wrmContextPath() + '/slack/users/' + encodeURI(username), function (response) {
            if (!response) {
                return;
            }

            var $slackIcon;
            if (response.length === 1) {
                $slackIcon = $(Jira.Templates.Slack.User.slackIcon({
                    slackUsers: response
                }));
            } else if (response.length > 1) {
                $slackIcon = $(Jira.Templates.Slack.User.slackIcon({
                    slackUsers: response
                }));
                $slackIcon.click(function(event) {
                    event.preventDefault(); // do not redirect to Slack - user should select team first
                });

                var slackSectionMarkup = Jira.Templates.Slack.User.teamLinks({
                    slackUsers: response
                });
                var $slackSection = $(slackSectionMarkup);
                var $popupMenu = $vcard.closest('.hoverpopup').find('#user-hover-more-dropdown > ul');
                $popupMenu.append($slackSection);
            }
            $vcard.find('h4').append($slackIcon);
        });
    }

    $(function() {
        $(document).on('ajaxComplete', handleAjaxComplete);
    });
});
