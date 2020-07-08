package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.AbstractPageEvent;
import com.atlassian.sal.api.message.I18nResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BlogCreateContentNotificationTest {
    @Mock
    private AttachmentBuilder contentCardBuilder;
    @Mock
    private I18nResolver i18nResolver;

    @Mock
    private AbstractPageEvent event;
    @Mock
    private Object unknownEvent;

    @InjectMocks
    private BlogCreateContentNotification target;

    @Test
    public void supports_shouldReturnExpectedValue() {
        when(event.isBlogCreate()).thenReturn(true, false);
        assertThat(target.supports(event), is(true));
        assertThat(target.supports(event), is(false));
        assertThat(target.supports(unknownEvent), is(false));
    }

    @Test
    public void getActivityKey_shouldReturnExpectedValueForPageCreate() {
        assertThat(target.getActivityKey(event), is(Optional.of("slack.activity.blog-create")));
    }

    @Test
    public void getMessageAttachment_shouldReturnExpectedValue() {
        assertThat(target.getMessageAttachment(event), is(Optional.empty()));
    }

    @Test
    public void shouldDisplayInConfiguration_shouldReturnExpectedValue() {
        assertThat(target.shouldDisplayInConfiguration(), is(true));
    }

    @Test
    public void shouldSend_shouldReturnExpectedValue() {
        assertThat(target.shouldSend(event), is(true));
    }
}
