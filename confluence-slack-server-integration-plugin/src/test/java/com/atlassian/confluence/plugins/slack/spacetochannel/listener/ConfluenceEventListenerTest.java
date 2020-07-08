package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.event.events.content.blogpost.BlogPostCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageUpdateEvent;
import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.AbstractPageEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.PersonalNotificationService;
import com.atlassian.confluence.plugins.slack.spacetochannel.service.SlackContentPermissionChecker;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.message.I18nResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfluenceEventListenerTest {
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AttachmentBuilder attachmentBuilder;
    @Mock
    private SlackContentPermissionChecker permissionChecker;
    @Mock
    private PersonalNotificationService personalNotificationService;
    @Mock
    private I18nResolver i18nResolver;

    @Mock
    private PageCreateEvent pageCreateEvent;
    @Mock
    private PageUpdateEvent pageUpdateEvent;
    @Mock
    private BlogPostCreateEvent blogPostCreateEvent;
    @Mock
    private Page page;
    @Mock
    private BlogPost blogPost;
    @Mock
    private Space space;
    @Mock
    private ConfluenceUser confluenceUser;

    @Captor
    private ArgumentCaptor<AbstractPageEvent> captor;

    @InjectMocks
    private ConfluenceEventListener target;

    @Test
    public void pageCreateEvent_shouldCallExpectedMethods() {
        when(attachmentBuilder.pageLink(page)).thenReturn("l");
        when(pageCreateEvent.getPage()).thenReturn(page);
        when(page.getSpace()).thenReturn(space);
        when(page.isNew()).thenReturn(true);
        when(page.getCreator()).thenReturn(confluenceUser);

        target.pageCreateEvent(pageCreateEvent);

        verify(eventPublisher).publish(captor.capture());

        final AbstractPageEvent event = captor.getValue();

        assertThat(event.getSpace(), sameInstance(space));
        assertThat(event.getLink(), is("l"));
        assertThat(event.getUser(), is(confluenceUser));
        assertThat(event.isPageCreate(), is(true));
    }

    @Test
    public void pageCreateEvent_shouldSkipIfRestricted() {
        when(permissionChecker.doesContentHaveViewRestrictions(page)).thenReturn(true);
        when(pageCreateEvent.getPage()).thenReturn(page);

        target.pageCreateEvent(pageCreateEvent);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    public void pageUpdateEvent_shouldCallExpectedMethods() {
        when(attachmentBuilder.pageLink(page)).thenReturn("l");
        when(pageUpdateEvent.getPage()).thenReturn(page);
        when(page.getSpace()).thenReturn(space);
        when(page.isNew()).thenReturn(false);
        when(page.getLastModifier()).thenReturn(confluenceUser);

        target.pageUpdateEvent(pageUpdateEvent);

        verify(eventPublisher).publish(captor.capture());

        final AbstractPageEvent event = captor.getValue();

        assertThat(event.getSpace(), sameInstance(space));
        assertThat(event.getLink(), is("l"));
        assertThat(event.getUser(), is(confluenceUser));
        assertThat(event.isPageUpdate(), is(true));
    }

    @Test
    public void blogPostCreateEvent_shouldCallExpectedMethods() {
        when(attachmentBuilder.pageLink(blogPost)).thenReturn("l");
        when(blogPostCreateEvent.getBlogPost()).thenReturn(blogPost);
        when(blogPost.getSpace()).thenReturn(space);
        when(blogPost.isNew()).thenReturn(true);
        when(blogPost.getCreator()).thenReturn(confluenceUser);

        target.blogPostCreateEvent(blogPostCreateEvent);

        verify(eventPublisher).publish(captor.capture());

        final AbstractPageEvent event = captor.getValue();

        assertThat(event.getSpace(), sameInstance(space));
        assertThat(event.getLink(), is("l"));
        assertThat(event.getUser(), is(confluenceUser));
        assertThat(event.isBlogCreate(), is(true));
    }

}
