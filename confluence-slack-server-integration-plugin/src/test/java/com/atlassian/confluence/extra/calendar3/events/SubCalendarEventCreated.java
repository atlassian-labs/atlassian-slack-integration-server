package com.atlassian.confluence.extra.calendar3.events;

import com.atlassian.confluence.user.ConfluenceUser;
import org.joda.time.Instant;

import java.util.Set;

@SuppressWarnings("unused")
public class SubCalendarEventCreated {
    private InnerEvent event;
    private ConfluenceUser trigger;
    private SubCalendar subCalendar;

    public SubCalendarEventCreated(final InnerEvent event,
                                   final ConfluenceUser trigger,
                                   final SubCalendar subCalendar) {
        this.event = event;
        this.trigger = trigger;
        this.subCalendar = subCalendar;
    }

    public InnerEvent getEvent() {
        return event;
    }

    public ConfluenceUser getTrigger() {
        return trigger;
    }

    public SubCalendar getSubCalendar() {
        return subCalendar;
    }

    public static class InnerEvent {
        private String eventTypeName;
        private String description;
        private String name;
        private boolean allDay;
        private Instant startTime;
        private Instant endTime;
        private Set<Invitee> invitees;

        public InnerEvent(final String eventTypeName,
                          final String description,
                          final String name,
                          final boolean allDay,
                          final Instant startTime,
                          final Instant endTime,
                          final Set<Invitee> invitees) {
            this.eventTypeName = eventTypeName;
            this.description = description;
            this.name = name;
            this.allDay = allDay;
            this.startTime = startTime;
            this.endTime = endTime;
            this.invitees = invitees;
        }

        public String getEventTypeName() {
            return eventTypeName;
        }

        public String getDescription() {
            return description;
        }

        public String getName() {
            return name;
        }

        public boolean isAllDay() {
            return allDay;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public Instant getEndTime() {
            return endTime;
        }

        public Set<Invitee> getInvitees() {
            return invitees;
        }
    }

    public static class Invitee {
        private ConfluenceUser user;

        public Invitee(final ConfluenceUser user) {
            this.user = user;
        }

        public ConfluenceUser getUser() {
            return user;
        }
    }

    public static class SubCalendar {
        private String name;
        private String timeZoneId;
        private String spaceKey;

        public SubCalendar(final String name, final String timeZoneId, final String spaceKey) {
            this.name = name;
            this.timeZoneId = timeZoneId;
            this.spaceKey = spaceKey;
        }

        public String getName() {
            return name;
        }

        public String getTimeZoneId() {
            return timeZoneId;
        }

        public String getSpaceKey() {
            return spaceKey;
        }
    }
}
