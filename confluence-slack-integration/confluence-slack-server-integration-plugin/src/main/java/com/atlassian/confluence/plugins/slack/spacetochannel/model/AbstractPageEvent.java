package com.atlassian.confluence.plugins.slack.spacetochannel.model;

import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.ConfluenceSlackEvent;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;

import java.util.Objects;

public class AbstractPageEvent implements ConfluenceSlackEvent {
    private final Space space;
    private final ConfluenceUser user;
    private final EventType eventType;
    private final PageType pageType;
    private final String link;

    public AbstractPageEvent(final AbstractPage target,
                             final EventType eventType,
                             final PageType pageType,
                             final String link) {
        this.space = target.getSpace();
        this.user = target.isNew() ? target.getCreator() : target.getLastModifier();
        this.eventType = eventType;
        this.pageType = pageType;
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

    public boolean isPageCreate() {
        return pageType.equals(PageType.PAGE) && eventType.equals(EventType.CREATE);
    }

    public boolean isPageUpdate() {
        return pageType.equals(PageType.PAGE) && eventType.equals(EventType.UPDATE);
    }

    public boolean isBlogCreate() {
        return pageType.equals(PageType.BLOG) && eventType.equals(EventType.CREATE);
    }

    public PageType getPageType() {
        return pageType;
    }

    public EventType getEventType() {
        return eventType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractPageEvent)) return false;
        final AbstractPageEvent that = (AbstractPageEvent) o;
        return Objects.equals(space, that.space) &&
                Objects.equals(user, that.user) &&
                eventType == that.eventType &&
                pageType == that.pageType &&
                Objects.equals(link, that.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(space, user, eventType, pageType, link);
    }

    @Override
    public String toString() {
        return "AbstractPageEvent{" +
                "space=" + space +
                ", user=" + user +
                ", eventType=" + eventType +
                ", pageType=" + pageType +
                ", link='" + link + '\'' +
                '}';
    }
}
