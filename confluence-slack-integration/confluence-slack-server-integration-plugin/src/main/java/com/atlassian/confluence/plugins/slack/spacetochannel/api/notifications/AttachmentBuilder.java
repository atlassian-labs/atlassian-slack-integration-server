package com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications;

import com.atlassian.confluence.core.SpaceContentEntityObject;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.github.seratch.jslack.api.model.Attachment;

public interface AttachmentBuilder {
    Attachment buildAttachment(SpaceContentEntityObject content);

    String getSlackPrettyDate(long epochMillis);

    String getSlackPrettyTime(long epochMillis);

    String getPleaseAuthenticateMessage();

    String getWelcomeMessage(String teamId);

    String getHelpMessage(String botUserId, String commandName);

    String getAccountMessage(ConfluenceUser user);

    String getInvalidCommandMessage();

    String userLink(ConfluenceUser user);

    String spaceLink(Space space);

    String calendarLink(Space space, String calendarName);

    String pageLink(SpaceContentEntityObject page);

    String baseUrl();

    String getNoSearchResultsMessage();

    String getSearchResultsTitleMessage(int resultsFound, String searchQuery);

    String searchLink(String query);

    Attachment searchFooter(String query);
}
