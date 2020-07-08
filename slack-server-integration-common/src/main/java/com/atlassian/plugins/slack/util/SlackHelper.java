package com.atlassian.plugins.slack.util;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class SlackHelper {
    public static String escapeSignsForSlackLink(final String linkText) {
        return linkText
                .replaceAll(">", "&gt;")
                .replaceAll("<", "&lt;");
    }

    public static String removeSlackLinks(final String slackText) {
        return trimToEmpty(slackText.replaceAll("<[^>]+>", ""));
    }
}
