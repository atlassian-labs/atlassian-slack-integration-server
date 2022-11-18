package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.ConfluenceCalendarEventCreatedEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.EventInfo;
import com.atlassian.confluence.plugins.slack.spacetochannel.notifications.CalendarEventCreatedNotification;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ConfluenceCalendarEventListener extends AutoSubscribingEventListener {
    private static final Logger log = LoggerFactory.getLogger(CalendarEventCreatedNotification.class);

    private static final String CONFLUENCE_CALENDAR_EVENT_CLASS =
            "com.atlassian.confluence.extra.calendar3.events.SubCalendarEventCreated";

    private final SpaceManager spaceManager;
    private final AttachmentBuilder attachmentBuilder;

    @Autowired
    public ConfluenceCalendarEventListener(final EventPublisher eventPublisher,
                                           final SpaceManager spaceManager,
                                           final AttachmentBuilder attachmentBuilder) {
        super(eventPublisher);
        this.spaceManager = spaceManager;
        this.attachmentBuilder = attachmentBuilder;
    }

    @EventListener
    public void calendarEvent(final Object event) {
        if (isConfluenceCalendarEvent(event)) {
            final Optional<EventInfo> eventInfo = getEventInfo(event);
            eventInfo.ifPresent(info -> {
                final Space space = spaceManager.getSpace(info.getSpaceKey());
                eventPublisher.publish(new ConfluenceCalendarEventCreatedEvent(
                        info,
                        space,
                        attachmentBuilder.calendarLink(space, info.getCalendarName())));
            });
        }
    }

    private boolean isConfluenceCalendarEvent(final Object event) {
        return CONFLUENCE_CALENDAR_EVENT_CLASS.equals(event.getClass().getName());
    }

    private Optional<EventInfo> getEventInfo(final Object event) {
        try {
            final PropertyUtilsBean propertyUtils = BeanUtilsBean.getInstance().getPropertyUtils();
            final Object innerEvent = propertyUtils.getProperty(event, "event");
            final ConfluenceUser trigger = (ConfluenceUser) propertyUtils.getProperty(event, "trigger");
            final String typeName = (String) propertyUtils.getProperty(innerEvent, "eventTypeName");
            final String description = (String) propertyUtils.getProperty(innerEvent, "description");
            final String name = (String) propertyUtils.getProperty(innerEvent, "name");
            final boolean allDay = (boolean) propertyUtils.getProperty(innerEvent, "allDay");
            final long startTime = (long) propertyUtils.getProperty(propertyUtils.getProperty(innerEvent, "startTime"), "millis");
            final long endTime = (long) propertyUtils.getProperty(propertyUtils.getProperty(innerEvent, "endTime"), "millis");

            @SuppressWarnings("unchecked") final Set<ConfluenceUser> invitees = ObjectUtils.defaultIfNull(
                    (Set<Object>) propertyUtils.getProperty(innerEvent, "invitees"),
                    Collections.emptySet())
                    .stream()
                    .flatMap(invitee -> {
                        try {
                            return Stream.of((ConfluenceUser) propertyUtils.getProperty(invitee, "user"));
                        } catch (Exception e) {
                            log.debug("Error parsing event", e);
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toSet());

            final Object subCalendar = propertyUtils.getProperty(event, "subCalendar");
            final String calendarName = (String) propertyUtils.getProperty(subCalendar, "name");
            final String timeZoneId = (String) propertyUtils.getProperty(subCalendar, "timeZoneId");
            final String spaceKey = (String) propertyUtils.getProperty(subCalendar, "spaceKey");

            return Optional.of(new EventInfo(
                    trigger,
                    spaceKey,
                    typeName,
                    calendarName,
                    description,
                    name,
                    invitees,
                    startTime,
                    endTime,
                    timeZoneId,
                    allDay
            ));
        } catch (Exception e) {
            log.debug("Error parsing event", e);
            return Optional.empty();
        }
    }
}
