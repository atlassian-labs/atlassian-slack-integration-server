package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.extra.calendar3.events.SubCalendarEventCreated;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.ConfluenceCalendarEventCreatedEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.EventInfo;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfluenceCalendarEventListenerTest {
    private static final String SPACE_KEY = "S";
    private static final String EVENT_TYPENAME = "travel";
    private static final String EVENT_DESC = "descr";
    private static final String EVENT_NAME = "my time off";
    private static final String EVENT_SUB_NAME = "Team Holidays";
    private static final String EVENT_SUB_TZ = "America/Chicago";
    private static final String EVENT_SUB_SPACE = SPACE_KEY;
    private static final long START_TIME = 1552262400000L;
    private static final long END_TIME = 1552313422000L;

    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AttachmentBuilder attachmentBuilder;
    @Mock
    private SpaceManager spaceManager;

    @Mock
    private ConfluenceUser confluenceUser;
    @Mock
    private ConfluenceUser invitee;
    @Mock
    private ConfluenceUser invitee2;
    @Mock
    private ConfluenceUser invitee3;
    @Mock
    private Space space;

    @Captor
    private ArgumentCaptor<ConfluenceCalendarEventCreatedEvent> captor;

    @InjectMocks
    private ConfluenceCalendarEventListener target;

    @Test
    public void calendarEvent_shouldCallExpectedMethods() {
        when(spaceManager.getSpace(SPACE_KEY)).thenReturn(space);
        when(attachmentBuilder.calendarLink(space, EVENT_SUB_NAME)).thenReturn("lnk");

        target.calendarEvent(createEvent());

        verify(eventPublisher).publish(captor.capture());

        final ConfluenceCalendarEventCreatedEvent event = captor.getValue();

        assertThat(event.getSpace(), sameInstance(space));
        assertThat(event.getLink(), is("lnk"));
        assertThat(event.getUser(), is(confluenceUser));

        final EventInfo info = event.getEventInfo();
        assertThat(info.getTrigger(), sameInstance(confluenceUser));
        assertThat(info.getSpaceKey(), is(SPACE_KEY));
        assertThat(info.getCalendarName(), is(EVENT_SUB_NAME));
        assertThat(info.getTypeName(), is(EVENT_TYPENAME));
        assertThat(info.getName(), is(EVENT_NAME));
        assertThat(info.getDescription(), is(EVENT_DESC));
        assertThat(info.getInvitees(), hasSize(3));
        assertThat(info.getInvitees(), containsInAnyOrder(invitee, invitee2, invitee3));
        assertThat(info.getStartTime(), is(START_TIME));
        assertThat(info.getEndTime(), is(END_TIME));
        assertThat(info.getTimeZoneId(), is(EVENT_SUB_TZ));
        assertThat(info.isAllDay(), is(true));
    }

    private SubCalendarEventCreated createEvent() {
        return new SubCalendarEventCreated(
                new SubCalendarEventCreated.InnerEvent(
                        EVENT_TYPENAME,
                        EVENT_DESC,
                        EVENT_NAME,
                        true,
                        new org.joda.time.Instant(START_TIME),
                        new org.joda.time.Instant(END_TIME),
                        Stream.of(invitee, invitee2, invitee3)
                                .map(SubCalendarEventCreated.Invitee::new)
                                .collect(Collectors.toSet())
                ),
                confluenceUser,
                new SubCalendarEventCreated.SubCalendar(EVENT_SUB_NAME, EVENT_SUB_TZ, EVENT_SUB_SPACE)
        );
    }
}
