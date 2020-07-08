package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.ConfluenceCalendarEventCreatedEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.EventInfo;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugins.slack.api.notification.BaseSlackEvent;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.timezone.TimeZoneManager;
import com.atlassian.sal.api.user.UserKey;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static com.atlassian.confluence.plugins.slack.spacetochannel.notifications.CalendarEventCreatedNotification.CONFLUENCE_CALENDAR_PLUGIN_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CalendarEventCreatedNotificationTest {
    private static final String USER = "USR";
    private static final String INVITEE = "IN";
    private static final String INVITEE2 = "IN2";
    private static final String INVITEE3 = "IN3";
    private static final UserKey userKey = new UserKey(USER);
    private static final UserKey inviteeKey = new UserKey(INVITEE);
    private static final String EVENT_TYPENAME = "travel";
    private static final String EVENT_DESC = "descr";
    private static final String EVENT_NAME = "my time off";
    private static final String EVENT_SUB_NAME = "Team Holidays";
    private static final String DEFAULT_TZ = "America/New_York";

    private static final long START_OF_SAME_DAY_UTC = 1552262400000L;
    private static final long START_OF_SAME_DAY_NY = 1552276800000L;
    private static final long END_OF_SAME_DAY_UTC = 1552348800000L;
    private static final long END_OF_NEXT_DAY_UTC = 1552435200000L;
    private static final long END_OF_NEXT_DAY_NY = 1552363200000L;
    private static final long START_DATE_UTC = 1552309511000L;
    private static final long END_DATE_UTC = 1552313422000L;
    private static final long NEXT_DAY_DATE_UTC = 1552396222000L;

    @Mock
    private AttachmentBuilder contentCardBuilder;
    @Mock
    private I18nResolver i18nResolver;
    @Mock
    private PluginAccessor pluginAccessor;
    @Mock
    private TimeZoneManager timeZoneManager;

    @Mock
    private ConfluenceCalendarEventCreatedEvent event;
    @Mock
    private EventInfo eventInfo;
    @Mock
    private BaseSlackEvent unknownEvent;
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

    @InjectMocks
    private CalendarEventCreatedNotification target;

    @Test
    public void supports_shouldReturnExpectedValue() {
        assertThat(target.supports(event), is(true));
        assertThat(target.supports(unknownEvent), is(false));
    }

    @Test
    public void shouldSend_shouldReturnTrue() {
        assertThat(target.shouldSend(event), is(true));
    }

    @Test
    public void shouldDisplayInConfiguration_shouldReturnTrueIfModuleIsEnabled() {
        when(pluginAccessor.isPluginEnabled(CONFLUENCE_CALENDAR_PLUGIN_KEY)).thenReturn(true);
        boolean result = target.shouldDisplayInConfiguration();
        assertThat(result, is(true));
    }

    @Test
    public void shouldDisplayInConfiguration_shouldReturnFalseIfModuleIsNotEnabled() {
        when(pluginAccessor.isPluginEnabled(CONFLUENCE_CALENDAR_PLUGIN_KEY)).thenReturn(false);
        boolean result = target.shouldDisplayInConfiguration();
        assertThat(result, is(false));
    }

    @Test
    public void getSpace_shouldReturnExpectedValue() {
        when(event.getSpace()).thenReturn(space);
        assertThat(target.getSpace(event), is(Optional.of(space)));
    }

    @Test
    public void getSpace_shouldReturnExpectedValueWhenNoSpaceIsProvided() {
        assertThat(target.getSpace(event), is(Optional.empty()));
    }

    @Test
    public void getSlackMessage_shouldReturnExpectedValueForAllDaySingleDate() {
        when(timeZoneManager.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone(DEFAULT_TZ));
        when(event.getSpace()).thenReturn(space);
        when(confluenceUser.getKey()).thenReturn(userKey);
        when(invitee.getKey()).thenReturn(inviteeKey);
        when(contentCardBuilder.userLink(confluenceUser)).thenReturn("u");
        when(contentCardBuilder.userLink(invitee)).thenReturn("inv");
        when(contentCardBuilder.calendarLink(space, EVENT_SUB_NAME)).thenReturn("c");
        when(contentCardBuilder.getSlackPrettyDate(START_OF_SAME_DAY_NY)).thenReturn("prettydate");
        when(i18nResolver.getText("slack.activity.calendar-event-asked.single.date", "prettydate")).thenReturn("dt");
        when(i18nResolver.getText("slack.activity.calendar-event-asked.for.users", "inv")).thenReturn("txtinv");
        when(i18nResolver.getText(
                "slack.activity.calendar-event-asked",
                "u",
                EVENT_TYPENAME,
                EVENT_DESC,
                "c",
                "txtinv",
                "dt")
        ).thenReturn("txt");

        populateEvent(true, START_OF_SAME_DAY_UTC, END_OF_SAME_DAY_UTC, invitee);
        Optional<ChatPostMessageRequest.ChatPostMessageRequestBuilder> result = target.getSlackMessage(event);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().build().getText(), is("txt"));
        assertThat(result.get().build().isMrkdwn(), is(true));
    }

    @Test
    public void getSlackMessage_shouldReturnExpectedValueForAllDayDifferentDates() {
        when(timeZoneManager.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone(DEFAULT_TZ));
        when(event.getSpace()).thenReturn(space);
        when(invitee.getFullName()).thenReturn(INVITEE);
        when(invitee2.getFullName()).thenReturn(INVITEE2);
        when(contentCardBuilder.userLink(confluenceUser)).thenReturn("u");
        when(contentCardBuilder.userLink(invitee)).thenReturn("inv");
        when(contentCardBuilder.userLink(invitee2)).thenReturn("inv2");
        when(contentCardBuilder.calendarLink(space, EVENT_SUB_NAME)).thenReturn("c");
        when(contentCardBuilder.getSlackPrettyDate(START_OF_SAME_DAY_NY)).thenReturn("prettydate1");
        when(contentCardBuilder.getSlackPrettyDate(END_OF_NEXT_DAY_NY)).thenReturn("prettydate2");
        when(i18nResolver.getText("slack.activity.calendar-event-asked.date.interval", "prettydate1", "prettydate2"))
                .thenReturn("dt");
        when(i18nResolver.getText("slack.activity.calendar-event-asked.for.users", "inv|inv2")).thenReturn("txtinv");
        when(i18nResolver.getText("slack.activity.calendar-event-asked.for.users.and")).thenReturn("|");
        when(i18nResolver.getText(
                "slack.activity.calendar-event-asked",
                "u",
                EVENT_TYPENAME,
                EVENT_DESC,
                "c",
                "txtinv",
                "dt")
        ).thenReturn("txt");

        populateEvent(true, START_OF_SAME_DAY_UTC, END_OF_NEXT_DAY_UTC, invitee, invitee2);
        Optional<ChatPostMessageRequest.ChatPostMessageRequestBuilder> result = target.getSlackMessage(event);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().build().getText(), is("txt"));
        assertThat(result.get().build().isMrkdwn(), is(true));
    }

    @Test
    public void getSlackMessage_shouldReturnExpectedValueForNotAllDayAndSameDate() {
        when(timeZoneManager.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone(DEFAULT_TZ));
        when(event.getSpace()).thenReturn(space);
        when(invitee.getFullName()).thenReturn(INVITEE);
        when(invitee2.getFullName()).thenReturn(INVITEE2);
        when(invitee3.getFullName()).thenReturn(INVITEE3);
        when(contentCardBuilder.userLink(confluenceUser)).thenReturn("u");
        when(contentCardBuilder.userLink(invitee)).thenReturn("inv");
        when(contentCardBuilder.userLink(invitee2)).thenReturn("inv2");
        when(contentCardBuilder.userLink(invitee3)).thenReturn("inv3");
        when(contentCardBuilder.calendarLink(space, EVENT_SUB_NAME)).thenReturn("c");
        when(contentCardBuilder.getSlackPrettyDate(START_DATE_UTC)).thenReturn("prettydate1");
        when(contentCardBuilder.getSlackPrettyTime(START_DATE_UTC)).thenReturn("prettytime1");
        when(contentCardBuilder.getSlackPrettyTime(END_DATE_UTC)).thenReturn("prettytime2");
        when(i18nResolver.getText("slack.activity.calendar-event-asked.time.interval", "prettydate1", "prettytime1", "prettytime2"))
                .thenReturn("dt");
        when(i18nResolver.getText("slack.activity.calendar-event-asked.for.users", "inv, inv2|inv3")).thenReturn("txtinv");
        when(i18nResolver.getText("slack.activity.calendar-event-asked.for.users.and")).thenReturn("|");
        when(i18nResolver.getText(
                "slack.activity.calendar-event-asked",
                "u",
                EVENT_TYPENAME,
                EVENT_DESC,
                "c",
                "txtinv",
                "dt")
        ).thenReturn("txt");

        populateEvent(false, START_DATE_UTC, END_DATE_UTC, invitee, invitee2, invitee3);
        Optional<ChatPostMessageRequest.ChatPostMessageRequestBuilder> result = target.getSlackMessage(event);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().build().getText(), is("txt"));
        assertThat(result.get().build().isMrkdwn(), is(true));
    }

    @Test
    public void getSlackMessage_shouldReturnExpectedValueForNotAllDayAndNotSameDate() {
        when(timeZoneManager.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone(DEFAULT_TZ));

        when(contentCardBuilder.userLink(confluenceUser)).thenReturn("u");
        when(contentCardBuilder.calendarLink(space, EVENT_SUB_NAME)).thenReturn("c");
        when(contentCardBuilder.getSlackPrettyDate(START_DATE_UTC)).thenReturn("prettydate1");
        when(contentCardBuilder.getSlackPrettyTime(START_DATE_UTC)).thenReturn("prettytime1");
        when(contentCardBuilder.getSlackPrettyDate(NEXT_DAY_DATE_UTC)).thenReturn("prettydate2");
        when(contentCardBuilder.getSlackPrettyTime(NEXT_DAY_DATE_UTC)).thenReturn("prettytime2");
        when(i18nResolver.getText("slack.activity.calendar-event-asked.time.full.interval", "prettydate1", "prettytime1",
                "prettydate2", "prettytime2")).thenReturn("dt");
        when(i18nResolver.getText(
                "slack.activity.calendar-event-asked",
                "u",
                EVENT_TYPENAME,
                EVENT_DESC,
                "c",
                "",
                "dt")
        ).thenReturn("txt");

        populateEvent(false, START_DATE_UTC, NEXT_DAY_DATE_UTC);
        Optional<ChatPostMessageRequest.ChatPostMessageRequestBuilder> result = target.getSlackMessage(event);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().build().getText(), is("txt"));
        assertThat(result.get().build().isMrkdwn(), is(true));
    }


    private void populateEvent(boolean allDay, long startDate, long endDate, ConfluenceUser... invites) {
        when(event.getEventInfo()).thenReturn(eventInfo);
        when(event.getSpace()).thenReturn(space);
        when(eventInfo.getTrigger()).thenReturn(confluenceUser);
        when(eventInfo.getTypeName()).thenReturn(EVENT_TYPENAME);
        when(eventInfo.getDescription()).thenReturn(EVENT_DESC);
        when(eventInfo.getName()).thenReturn(EVENT_NAME);
        when(eventInfo.isAllDay()).thenReturn(allDay);
        when(eventInfo.getStartTime()).thenReturn(startDate);
        when(eventInfo.getEndTime()).thenReturn(endDate);
        when(eventInfo.getInvitees()).thenReturn(invites.length == 0
                ? Collections.emptySet()
                : Arrays.stream(invites).collect(Collectors.toSet()));
        when(eventInfo.getCalendarName()).thenReturn(EVENT_SUB_NAME);
    }
}
