package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionType;
import com.atlassian.plugin.PluginAccessor;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QuestionContentNotificationTest {
    @Mock
    private AttachmentBuilder contentCardBuilder;
    @Mock
    private I18nResolver i18nResolver;
    @Mock
    private PluginAccessor pluginAccessor;

    @Mock
    private QuestionEvent event;
    @Mock
    private Object unknownEvent;
    @Mock
    private Attachment attachment;

    @InjectMocks
    private QuestionContentNotification target;

    @Test
    public void supports_shouldReturnExpectedValue() {
        when(event.isAnswer()).thenReturn(false, true);
        assertThat(target.supports(event), is(true));
        assertThat(target.supports(event), is(false));
        assertThat(target.supports(unknownEvent), is(false));
    }

    @Test
    public void shouldDisplayInConfiguration_shouldReturnExpectedValueWhenModuleIsDisable() {
        assertThat(target.shouldDisplayInConfiguration(), is(false));
    }

    @Test
    public void shouldDisplayInConfiguration_shouldReturnExpectedValueWhenModuleIsEnabled() {
        when(pluginAccessor.isPluginModuleEnabled(QuestionType.QUESTION.pluginModuleKey())).thenReturn(true);
        assertThat(target.shouldDisplayInConfiguration(), is(true));
    }

    @Test
    public void getActivityKey_shouldReturnExpectedValue() {
        assertThat(target.getActivityKey(event), is(Optional.of("slack.activity.question-asked")));
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
    public void shouldSend_shouldReturnExpectedValue() {
        assertThat(target.shouldSend(event), is(true));
    }
}
