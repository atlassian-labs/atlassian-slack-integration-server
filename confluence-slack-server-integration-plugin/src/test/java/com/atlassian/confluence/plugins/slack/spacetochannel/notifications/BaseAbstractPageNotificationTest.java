package com.atlassian.confluence.plugins.slack.spacetochannel.notifications;

import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.ConfluenceSlackEvent;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Attachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BaseAbstractPageNotificationTest {
    @Mock
    private AttachmentBuilder contentCardBuilder;
    @Mock
    private I18nResolver i18nResolver;

    @Mock
    private ConfluenceSlackEvent event;
    @Mock
    private Supplier<String> activityKeySupplier;
    @Mock
    private Supplier<Attachment> attachmentSupplier;
    @Mock
    private ConfluenceUser confluenceUser;
    @Mock
    private Space space;
    @Mock
    private Attachment attachment;

    private BaseAbstractPageNotification<ConfluenceSlackEvent> target;

    @BeforeEach
    public void setUp() {
        target = new BaseAbstractPageNotification<ConfluenceSlackEvent>(contentCardBuilder, i18nResolver) {
            @Override
            public boolean supports(final Object event) {
                return false;
            }

            @Override
            protected Optional<String> getActivityKey(final ConfluenceSlackEvent event) {
                return Optional.ofNullable(activityKeySupplier.get());
            }

            @Override
            protected Optional<Attachment> getMessageAttachment(final ConfluenceSlackEvent event) {
                return Optional.ofNullable(attachmentSupplier.get());
            }
        };
    }

    @Test
    public void getSlackMessage_shouldReturnExpectedValueForAttachment() {
        when(attachmentSupplier.get()).thenReturn(attachment);

        Optional<ChatPostMessageRequest.ChatPostMessageRequestBuilder> result = target.getSlackMessage(event);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().build().getText(), nullValue());
        assertThat(result.get().build().getAttachments(), contains(attachment));
        assertThat(result.get().build().isMrkdwn(), is(true));
    }

    @Test
    public void getSlackMessage_shouldReturnExpectedValueForActivityKey() {
        when(event.getSpace()).thenReturn(space);
        when(event.getUser()).thenReturn(confluenceUser);
        when(event.getLink()).thenReturn("link");
        when(activityKeySupplier.get()).thenReturn("act");
        when(contentCardBuilder.userLink(confluenceUser)).thenReturn("u");
        when(contentCardBuilder.spaceLink(space)).thenReturn("s");
        when(i18nResolver.getText("act", "u", "link", "s")).thenReturn("txt");

        Optional<ChatPostMessageRequest.ChatPostMessageRequestBuilder> result = target.getSlackMessage(event);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().build().getText(), is("txt"));
        assertThat(result.get().build().getAttachments(), empty());
        assertThat(result.get().build().isMrkdwn(), is(true));
    }

    @Test
    public void getSlackMessage_shouldReturnExpectedValueForBothAttachmentAncActivityKey() {
        when(event.getSpace()).thenReturn(space);
        when(event.getUser()).thenReturn(confluenceUser);
        when(event.getLink()).thenReturn("link");
        when(attachmentSupplier.get()).thenReturn(attachment);
        when(activityKeySupplier.get()).thenReturn("act");
        when(contentCardBuilder.userLink(confluenceUser)).thenReturn("u");
        when(contentCardBuilder.spaceLink(space)).thenReturn("s");
        when(i18nResolver.getText("act", "u", "link", "s")).thenReturn("txt");

        Optional<ChatPostMessageRequest.ChatPostMessageRequestBuilder> result = target.getSlackMessage(event);

        assertThat(result.isPresent(), is(true));
        assertThat(result.get().build().getText(), is("txt"));
        assertThat(result.get().build().getAttachments(), contains(attachment));
        assertThat(result.get().build().isMrkdwn(), is(true));
    }

    @Test
    public void getSlackMessage_shouldReturnEmptyIfNoMessageOrAttachmentIsProvided() {
        Optional<ChatPostMessageRequest.ChatPostMessageRequestBuilder> result = target.getSlackMessage(event);
        assertThat(result.isPresent(), is(false));
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
}
