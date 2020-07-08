package com.atlassian.plugins.slack.event;

import com.atlassian.plugins.slack.api.SlackLink;

public class SlackLinkedEvent {
    private final SlackLink link;

    public SlackLinkedEvent(final SlackLink link) {
        this.link = link;
    }

    public SlackLink getLink() {
        return link;
    }
}
