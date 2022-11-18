package com.atlassian.confluence.plugins.slack.spacetochannel.model;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.ConfluenceSlackEvent;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;

import java.util.Objects;

public class ConfluenceCalendarEventCreatedEvent implements ConfluenceSlackEvent {
    private final Space space;
    private final ConfluenceUser user;
    private final EventInfo eventInfo;
    private final String link;

    public ConfluenceCalendarEventCreatedEvent(final EventInfo eventInfo,
                                               final Space space,
                                               final String link) {
        this.space = space;
        this.user = eventInfo.getTrigger();
        this.eventInfo = eventInfo;
        this.link = link;
    }

    @Override
    public Space getSpace() {
        return space;
    }

    @Override
    public ConfluenceUser getUser() {
        return user;
    }

    @Override
    public String getLink() {
        return link;
    }

    public EventInfo getEventInfo() {
        return eventInfo;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfluenceCalendarEventCreatedEvent)) return false;
        final ConfluenceCalendarEventCreatedEvent that = (ConfluenceCalendarEventCreatedEvent) o;
        return Objects.equals(space, that.space) &&
                Objects.equals(user, that.user) &&
                Objects.equals(eventInfo, that.eventInfo) &&
                Objects.equals(link, that.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(space, user, eventInfo, link);
    }

    @Override
    public String toString() {
        return "ConfluenceCalendarEventCreatedEvent{" +
                "space=" + space +
                ", user=" + user +
                ", eventInfo=" + eventInfo +
                ", link='" + link + '\'' +
                '}';
    }
}
