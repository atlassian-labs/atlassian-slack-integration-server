package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.content.CustomContentEntityObject;
import com.atlassian.confluence.content.event.PluginContentCreatedEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionType;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackContentPermissionChecker;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfluenceQuestionsEventListenerTest {
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AttachmentBuilder attachmentBuilder;
    @Mock
    private SlackContentPermissionChecker permissionChecker;

    @Mock
    private PluginContentCreatedEvent event;
    @Mock
    private CustomContentEntityObject customContentEntityObject;
    @Mock
    private Space space;
    @Mock
    private ConfluenceUser confluenceUser;

    @Captor
    private ArgumentCaptor<QuestionEvent> captor;

    @InjectMocks
    private ConfluenceQuestionsEventListener target;

    @Test
    public void questionsEvent_shouldCallExpectedMethodsForQuestion() {
        when(event.getContent()).thenReturn(customContentEntityObject);
        when(customContentEntityObject.getSpace()).thenReturn(space);
        when(customContentEntityObject.getCreator()).thenReturn(confluenceUser);
        when(customContentEntityObject.getPluginModuleKey()).thenReturn(QuestionType.QUESTION.pluginModuleKey());
        when(attachmentBuilder.pageLink(customContentEntityObject)).thenReturn("l");

        target.questionsEvent(event);

        verify(eventPublisher).publish(captor.capture());

        final QuestionEvent event = captor.getValue();

        assertThat(event.getSpace(), sameInstance(space));
        assertThat(event.getLink(), is("l"));
        assertThat(event.getUser(), is(confluenceUser));
        assertThat(event.getAttachment(), nullValue());
        assertThat(event.isAnswer(), is(false));
    }

    @Test
    public void questionsEvent_shouldCallExpectedMethodsForAnswer() {
        when(event.getContent()).thenReturn(customContentEntityObject);
        when(customContentEntityObject.getSpace()).thenReturn(space);
        when(customContentEntityObject.getBodyAsStringWithoutMarkup()).thenReturn("bd");
        when(customContentEntityObject.getCreator()).thenReturn(confluenceUser);
        when(customContentEntityObject.getPluginModuleKey()).thenReturn(QuestionType.ANSWER.pluginModuleKey());
        when(attachmentBuilder.pageLink(customContentEntityObject)).thenReturn("l");

        target.questionsEvent(event);

        verify(eventPublisher).publish(captor.capture());

        final QuestionEvent event = captor.getValue();

        assertThat(event.getSpace(), sameInstance(space));
        assertThat(event.getLink(), is("l"));
        assertThat(event.getUser(), is(confluenceUser));
        assertThat(event.getAttachment(), notNullValue());
        assertThat(event.getAttachment().getText(), is("bd"));
        assertThat(event.isAnswer(), is(true));
    }

    @Test
    public void questionsEvent_shouldSkipIfRestricted() {
        when(attachmentBuilder.pageLink(customContentEntityObject)).thenReturn("l");
        when(customContentEntityObject.getPluginModuleKey()).thenReturn(QuestionType.QUESTION.pluginModuleKey());
        when(permissionChecker.doesContentHaveViewRestrictions(customContentEntityObject)).thenReturn(true);
        when(event.getContent()).thenReturn(customContentEntityObject);

        target.questionsEvent(event);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    public void questionsEvent_shouldSkipIfUnknownModule() {
        when(event.getContent()).thenReturn(customContentEntityObject);
        when(customContentEntityObject.getPluginModuleKey()).thenReturn("some.other.module");

        target.questionsEvent(event);

        verify(eventPublisher, never()).publish(any());
    }
}
