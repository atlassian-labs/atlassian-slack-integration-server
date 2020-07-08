package com.atlassian.plugins.slack.api.events;

public class NotificationBlockedEvent<SourceEventType> {
    private final SourceEventType sourceEvent;

    public NotificationBlockedEvent(final SourceEventType sourceEvent) {
        this.sourceEvent = sourceEvent;
    }

    public SourceEventType getSourceEvent() {
        return sourceEvent;
    }
}
