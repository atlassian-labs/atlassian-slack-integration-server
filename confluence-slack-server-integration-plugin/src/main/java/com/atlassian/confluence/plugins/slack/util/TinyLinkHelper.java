package com.atlassian.confluence.plugins.slack.util;

import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.TinyUrl;
import com.atlassian.sal.api.ApplicationProperties;
import lombok.experimental.UtilityClass;

import static com.atlassian.plugins.slack.util.LinkHelper.absoluteUrl;

@UtilityClass
public class TinyLinkHelper {
    public static String tinyLink(final AbstractPage page, final ApplicationProperties applicationProperties) {
        return absoluteUrl("/x/" + new TinyUrl(page).getIdentifier(), applicationProperties);
    }

    public static long fromTinyLink(final String identifier) {
        return new TinyUrl(identifier).getPageId();
    }
}
