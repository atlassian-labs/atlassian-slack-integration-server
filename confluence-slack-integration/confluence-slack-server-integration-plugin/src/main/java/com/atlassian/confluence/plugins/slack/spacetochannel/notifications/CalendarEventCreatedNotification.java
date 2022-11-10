package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.SpaceToChannelNotification;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.ConfluenceCalendarEventCreatedEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.EventInfo;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.timezone.TimeZoneManager;
import com.atlassian.user.User;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CalendarEventCreatedNotification implements SpaceToChannelNotification<ConfluenceCalendarEventCreatedEvent> {
    private static final Logger log = LoggerFactory.getLogger(CalendarEventCreatedNotification.class);

    static final String CONFLUENCE_CALENDAR_PLUGIN_KEY =
            "com.atlassian.confluence.extra.team-calendars";

    private final AttachmentBuilder contentCardBuilder;
    private final I18nResolver i18nResolver;
    private final PluginAccessor pluginAccessor;
    private final TimeZoneManager timeZoneManager;

    @Autowired
    public CalendarEventCreatedNotification(final AttachmentBuilder contentCardBuilder,
                                            final I18nResolver i18nResolver,
                                            final PluginAccessor pluginAccessor,
                                            final TimeZoneManager timeZoneManager) {
        this.contentCardBuilder = contentCardBuilder;
        this.i18nResolver = i18nResolver;
        this.pluginAccessor = pluginAccessor;
        this.timeZoneManager = timeZoneManager;
    }

    @Override
    public boolean supports(final Object event) {
        return event instanceof ConfluenceCalendarEventCreatedEvent;
    }

    @Override
    public boolean shouldSend(final ConfluenceCalendarEventCreatedEvent event) {
        return true;
    }

    @Override
    public boolean shouldDisplayInConfiguration() {
        try {
            return pluginAccessor.isPluginEnabled(CONFLUENCE_CALENDAR_PLUGIN_KEY);
        } catch (Throwable e) {
            log.debug("Could not test Team Calendars plugin", e);
            return false;
        }
    }

    @Override
    public Optional<ChatPostMessageRequest.ChatPostMessageRequestBuilder> getSlackMessage(final ConfluenceCalendarEventCreatedEvent event) {
        final EventInfo info = event.getEventInfo();
        final Space space = event.getSpace();
        final String text = i18nResolver.getText(
                "slack.activity.calendar-event-asked",
                contentCardBuilder.userLink(info.getTrigger()),
                info.getTypeName(),
                StringUtils.defaultIfBlank(info.getDescription(), info.getName()),
                contentCardBuilder.calendarLink(space, info.getCalendarName()),
                getUserList(info.getInvitees(), info.getTrigger()),
                getDates(info.getStartTime(), info.getEndTime(), info.isAllDay()));

        return Optional.ofNullable(ChatPostMessageRequest.builder()
                .mrkdwn(true)
                .text(text));
    }

    private String getUserList(final Set<ConfluenceUser> invitees, final ConfluenceUser trigger) {
        if (invitees.isEmpty()) {
            return "";
        }

        if (invitees.size() == 1 && invitees.iterator().next().getKey().equals(trigger.getKey())) {
            return "";
        }

        final List<String> userLinks = invitees.stream()
                .sorted(Comparator.comparing(User::getFullName))
                .map(contentCardBuilder::userLink)
                .collect(Collectors.toList());

        if (userLinks.size() == 1) {
            return i18nResolver.getText("slack.activity.calendar-event-asked.for.users", userLinks.get(0));
        }

        return i18nResolver.getText("slack.activity.calendar-event-asked.for.users",
                String.join(", ", userLinks.subList(0, userLinks.size() - 1))
                        + i18nResolver.getText("slack.activity.calendar-event-asked.for.users.and")
                        + userLinks.get(userLinks.size() - 1));
    }

    private String getDates(final long startDate,
                            final long endDate,
                            final boolean allDay) {
        final ZoneId zone = timeZoneManager.getDefaultTimeZone().toZoneId();
        final LocalDate start = LocalDate.from(Instant.ofEpochMilli(startDate).atZone(ZoneOffset.UTC));
        final LocalDate end = LocalDate.from(Instant.ofEpochMilli(endDate).atZone(ZoneOffset.UTC));
        if (allDay) {
            final LocalDate realEnd = end.minusDays(1);
            if (start.isEqual(realEnd)) {
                return i18nResolver.getText("slack.activity.calendar-event-asked.single.date",
                        contentCardBuilder.getSlackPrettyDate(start.atStartOfDay(zone).toEpochSecond() * 1000));
            } else {
                return i18nResolver.getText("slack.activity.calendar-event-asked.date.interval",
                        contentCardBuilder.getSlackPrettyDate(start.atStartOfDay(zone).toEpochSecond() * 1000),
                        contentCardBuilder.getSlackPrettyDate(realEnd.atStartOfDay(zone).toEpochSecond() * 1000));
            }
        } else {
            if (start.isEqual(end)) {
                return i18nResolver.getText("slack.activity.calendar-event-asked.time.interval",
                        contentCardBuilder.getSlackPrettyDate(startDate),
                        contentCardBuilder.getSlackPrettyTime(startDate),
                        contentCardBuilder.getSlackPrettyTime(endDate));
            } else {
                return i18nResolver.getText("slack.activity.calendar-event-asked.time.full.interval",
                        contentCardBuilder.getSlackPrettyDate(startDate),
                        contentCardBuilder.getSlackPrettyTime(startDate),
                        contentCardBuilder.getSlackPrettyDate(endDate),
                        contentCardBuilder.getSlackPrettyTime(endDate));
            }
        }
    }

    @Override
    public Optional<Space> getSpace(final ConfluenceCalendarEventCreatedEvent event) {
        return Optional.ofNullable(event.getSpace());
    }
}
