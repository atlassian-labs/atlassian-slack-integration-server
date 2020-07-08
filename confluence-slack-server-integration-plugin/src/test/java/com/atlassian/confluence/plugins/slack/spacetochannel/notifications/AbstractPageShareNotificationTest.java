package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.ContentSharedEvent;
import com.atlassian.plugins.slack.api.events.NotificationBlockedEvent;
import com.atlassian.plugins.slack.api.notification.BaseSlackEvent;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.model.Attachment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbstractPageShareNotificationTest {
    @Mock
    private AttachmentBuilder contentCardBuilder;
    @Mock
    private I18nResolver i18nResolver;

    @Mock
    private ContentSharedEvent event;
    @Mock
    private BaseSlackEvent unknownEvent;
    @Mock
    private Attachment attachment;

    @InjectMocks
    private AbstractPageShareNotification target;

    @Test
    public void supports_shouldReturnExpectedValue() {
        assertThat(target.supports(event), is(true));
        assertThat(target.supports(unknownEvent), is(false));
    }

    @Test
    public void getMessageAttachment_shouldReturnExpectedValue() {
        when(event.getAttachment()).thenReturn(attachment);
        assertThat(target.getMessageAttachment(event), is(Optional.of(attachment)));
    }

    @Test
    public void getMessageAttachment_shouldReturnExpectedValueForUnknownEvent() {
        assertThat(target.getMessageAttachment(event), is(Optional.empty()));
    }

    @Test
    public void getActivityKey_shouldReturnExpectedValue() {
        assertThat(target.getActivityKey(event), is(Optional.empty()));
    }

    @Test
    public void shouldDisplayInConfiguration_shouldReturnExpectedValue() {
        assertThat(target.shouldDisplayInConfiguration(), is(true));
    }

    @Test
    public void shouldSend_shouldReturnExpectedValue() {
        assertThat(target.shouldSend(event), is(true));
    }

    @Test
    public void buildNotificationBlockedEvent_shouldReturnExpectedValue() {
        Optional<NotificationBlockedEvent<ContentSharedEvent>> result = target.buildNotificationBlockedEvent(event);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), isA(NotificationBlockedEvent.class));
        assertThat(result.get().getSourceEvent(), sameInstance(event));
    }
}
