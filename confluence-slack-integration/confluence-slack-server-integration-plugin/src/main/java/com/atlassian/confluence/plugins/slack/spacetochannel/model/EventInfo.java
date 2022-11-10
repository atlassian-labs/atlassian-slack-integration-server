package com.atlassian.confluence.plugins.slack.spacetochannel.model;

import com.atlassian.confluence.user.ConfluenceUser;

import java.util.Objects;
import java.util.Set;

public class EventInfo {
    private final ConfluenceUser trigger;
    private final String spaceKey;
    private final String typeName;
    private final String calendarName;
    private final String description;
    private final String name;
    private final Set<ConfluenceUser> invitees;
    private final long startTime;
    private final long endTime;
    private final String timeZoneId;
    private final boolean allDay;

    public EventInfo(final ConfluenceUser trigger,
                     final String spaceKey,
                     final String typeName,
                     final String calendarName,
                     final String description,
                     final String name,
                     final Set<ConfluenceUser> invitees,
                     final long startTime,
                     final long endTime,
                     final String timeZoneId,
                     final boolean allDay) {
        this.trigger = trigger;
        this.spaceKey = spaceKey;
        this.typeName = typeName;
        this.calendarName = calendarName;
        this.description = description;
        this.name = name;
        this.invitees = invitees;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeZoneId = timeZoneId;
        this.allDay = allDay;
    }

    public ConfluenceUser getTrigger() {
        return trigger;
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getCalendarName() {
        return calendarName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public Set<ConfluenceUser> getInvitees() {
        return invitees;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof EventInfo)) return false;
        final EventInfo eventInfo = (EventInfo) o;
        return startTime == eventInfo.startTime &&
                endTime == eventInfo.endTime &&
                allDay == eventInfo.allDay &&
                Objects.equals(trigger, eventInfo.trigger) &&
                Objects.equals(spaceKey, eventInfo.spaceKey) &&
                Objects.equals(typeName, eventInfo.typeName) &&
                Objects.equals(calendarName, eventInfo.calendarName) &&
                Objects.equals(description, eventInfo.description) &&
                Objects.equals(name, eventInfo.name) &&
                Objects.equals(invitees, eventInfo.invitees) &&
                Objects.equals(timeZoneId, eventInfo.timeZoneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trigger, spaceKey, typeName, calendarName, description, name, invitees, startTime, endTime, timeZoneId, allDay);
    }

    @Override
    public String toString() {
        return "EventInfo{" +
                "trigger=" + trigger +
                ", spaceKey='" + spaceKey + '\'' +
                ", typeName='" + typeName + '\'' +
                ", calendarName='" + calendarName + '\'' +
                ", description='" + description + '\'' +
                ", name='" + name + '\'' +
                ", invitees=" + invitees +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", timeZoneId='" + timeZoneId + '\'' +
                ", allDay=" + allDay +
                '}';
    }
}
