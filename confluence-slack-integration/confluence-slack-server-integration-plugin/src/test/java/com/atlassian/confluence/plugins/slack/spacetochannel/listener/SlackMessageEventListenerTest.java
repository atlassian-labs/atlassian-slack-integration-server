package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.confluence.content.CustomContentEntityObject;
import com.atlassian.confluence.content.CustomContentManager;
import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.pages.Comment;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.pages.TinyUrl;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.ContentSharedEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionType;
import com.atlassian.confluence.plugins.slack.util.ConfluenceUserImpersonator;
import com.atlassian.confluence.plugins.slack.util.SearchBuilder;
import com.atlassian.confluence.search.v2.ISearch;
import com.atlassian.confluence.search.v2.InvalidSearchException;
import com.atlassian.confluence.search.v2.SearchManager;
import com.atlassian.confluence.search.v2.SearchResults;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.webhooks.GenericMessageSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.LinkSharedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackSlashCommand;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.test.util.CommonTestUtil;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.sal.api.user.UserKey;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostEphemeralRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Attachment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
public class SlackMessageEventListenerTest {
    private static final String TEAM_ID = "T";
    private static final String CHANNEL_ID = "C";
    private static final String SLACK_USER_ID = "SU";
    private static final String USER = "USR";
    private static final String URL = "https://url.com";
    private static final UserKey userKey = new UserKey(USER);

    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AttachmentBuilder attachmentBuilder;
    @Mock
    private UserAccessor userAccessor;
    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private PageManager pageManager;
    @Mock
    private CustomContentManager customContentManager;
    @Mock
    private PermissionManager permissionManager;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private SearchManager searchManager;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;
    @Mock
    private ConfluenceUserImpersonator confluenceUserImpersonator;

    @Mock
    private SlackSlashCommand slackSlashCommand;
    @Mock
    private GenericMessageSlackEvent genericMessageSlackEvent;
    @Mock
    private LinkSharedSlackEvent linkSharedSlackEvent;
    @Mock
    private SlackLink slackLink;
    @Mock
    private SlackClient slackClient;
    @Mock
    private SlackUser slackUser;
    @Mock
    private ConfluenceUser confluenceUser;
    @Mock
    private SlackEvent slackEvent;
    @Mock
    private Page page;
    @Mock
    private Space space;
    @Mock
    private BlogPost blogPost;
    @Mock
    private CustomContentEntityObject customContentEntityObject;
    @Mock
    private Attachment attachment;
    @Mock
    private Attachment footerAttachment;
    @Mock
    private AsyncExecutor asyncExecutor;
    @Mock
    private SearchResults searchResults;
    @Mock
    private Comment comment;
    @Mock
    private SearchBuilder searchBuilder;
    @Mock
    private ISearch iSearch;

    @Captor
    private ArgumentCaptor<ChatPostMessageRequest> captor;
    @Captor
    private ArgumentCaptor<ChatPostEphemeralRequest> ephemeralCaptor;
    @Captor
    private ArgumentCaptor<ContentSharedEvent> contentSharedCaptor;
    @Captor
    private ArgumentCaptor<Calendar> calendar;
    @Captor
    private ArgumentCaptor<Map<String, Attachment>> unfurlsCaptor;
    @Captor
    private ArgumentCaptor<ISearch> searchCaptor;
    @Captor
    private ArgumentCaptor<ConfluenceUser> confluenceUserCaptor;

    @InjectMocks
    private SlackMessageEventListener target;

    @Test
    public void slashCommand_shouldSendHelp() {
        when(slackSlashCommand.getText()).thenReturn("help ");
        when(slackSlashCommand.getResponseUrl()).thenReturn("ru");
        when(slackSlashCommand.getSlackLink()).thenReturn(slackLink);
        when(slackSlashCommand.getCommandName()).thenReturn("/cmd");
        when(attachmentBuilder.getHelpMessage("bot", "/cmd")).thenReturn("helpmsg");
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(slackLink.getBotUserId()).thenReturn("bot");
        CommonTestUtil.bypass(asyncExecutor);

        target.slashCommand(slackSlashCommand);

        verify(slackClient).postResponse(eq("ru"), eq("ephemeral"), captor.capture());
        assertThat(captor.getValue().getText(), is("helpmsg"));
    }

    @Test
    public void slashCommand_shouldSendHelpWithEmptyMessage() {
        when(slackSlashCommand.getText()).thenReturn("   ");
        when(slackSlashCommand.getResponseUrl()).thenReturn("ru");
        when(slackSlashCommand.getSlackLink()).thenReturn(slackLink);
        when(slackSlashCommand.getCommandName()).thenReturn("/cmd");
        when(attachmentBuilder.getHelpMessage("bot", "/cmd")).thenReturn("helpmsg");
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(slackLink.getBotUserId()).thenReturn("bot");
        CommonTestUtil.bypass(asyncExecutor);

        target.slashCommand(slackSlashCommand);

        verify(slackClient).postResponse(eq("ru"), eq("ephemeral"), captor.capture());
        assertThat(captor.getValue().getText(), is("helpmsg"));
    }

    @Test
    public void slashCommand_shouldSendAccount() {
        when(slackSlashCommand.getText()).thenReturn(" Account ");
        when(slackSlashCommand.getResponseUrl()).thenReturn("ru");
        when(slackSlashCommand.getSlackLink()).thenReturn(slackLink);
        when(slackSlashCommand.getUserId()).thenReturn(SLACK_USER_ID);
        when(attachmentBuilder.getAccountMessage(confluenceUser)).thenReturn("accmsg");
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.of(slackUser));
        when(userAccessor.getExistingUserByKey(eq(userKey))).thenReturn(confluenceUser);
        when(slackUser.getUserKey()).thenReturn(USER);
        CommonTestUtil.bypass(asyncExecutor);

        target.slashCommand(slackSlashCommand);

        verify(slackClient).postResponse(eq("ru"), eq("ephemeral"), captor.capture());
        assertThat(captor.getValue().getText(), is("accmsg"));
    }

    @Test
    public void slashCommand_shouldRespondWithSearchResults() throws InvalidSearchException {
        String query = "something";
        when(slackSlashCommand.getText()).thenReturn("search " + query);
        when(slackSlashCommand.getResponseUrl()).thenReturn("ru");
        when(slackSlashCommand.getSlackLink()).thenReturn(slackLink);
        when(searchBuilder.buildSearch(eq(query), isNull(), anyInt(), anyInt()))
                .thenReturn(iSearch);
        when(searchManager.search(iSearch)).thenReturn(searchResults);
        when(searchManager.convertToEntities(searchResults, SearchManager.EntityVersionPolicy.LATEST_VERSION))
                .thenReturn(Collections.singletonList(page));
        when(attachmentBuilder.buildAttachment(page)).thenReturn(attachment);
        when(attachmentBuilder.searchFooter(query)).thenReturn(footerAttachment);
        when(attachmentBuilder.getSearchResultsTitleMessage(1, query)).thenReturn("search-res");
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        CommonTestUtil.bypass(asyncExecutor);

        target.slashCommand(slackSlashCommand);

        verify(slackClient).postResponse(eq("ru"), eq("ephemeral"), captor.capture());
        ChatPostMessageRequest message = captor.getValue();
        assertThat(message.getText(), is("search-res"));
        assertThat(message.getAttachments().get(0), is(attachment));
        assertThat(message.getAttachments().get(1), is(footerAttachment));
    }

    @Test
    public void slashCommand_shouldSearchPagesAndBlogPostsOnly() throws InvalidSearchException {
        String query = "something";
        when(slackSlashCommand.getText()).thenReturn("search " + query);
        when(slackSlashCommand.getResponseUrl()).thenReturn("ru");
        when(slackSlashCommand.getSlackLink()).thenReturn(slackLink);
        when(searchBuilder.buildSearch(eq(query), isNull(), anyInt(), anyInt()))
                .thenReturn(iSearch);
        when(searchManager.search(iSearch)).thenReturn(searchResults);
        when(searchManager.convertToEntities(searchResults, SearchManager.EntityVersionPolicy.LATEST_VERSION))
                .thenReturn(Arrays.asList(page, comment));
        when(attachmentBuilder.buildAttachment(page)).thenReturn(attachment);
        when(attachmentBuilder.searchFooter(query)).thenReturn(footerAttachment);
        when(attachmentBuilder.getSearchResultsTitleMessage(1, query)).thenReturn("search-res");
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        CommonTestUtil.bypass(asyncExecutor);

        target.slashCommand(slackSlashCommand);

        verify(slackClient).postResponse(eq("ru"), eq("ephemeral"), captor.capture());
        ChatPostMessageRequest message = captor.getValue();
        assertThat(message.getText(), is("search-res"));
        List<Attachment> attachments = message.getAttachments();
        assertThat(attachments.size(), is(2));
        assertThat(attachments, contains(attachment, footerAttachment));
    }

    @Test
    public void slashCommand_shouldSearchWithUserImpersonation() throws InvalidSearchException {
        String query = "something";
        when(slackSlashCommand.getText()).thenReturn("search " + query);
        when(slackSlashCommand.getResponseUrl()).thenReturn("ru");
        when(slackSlashCommand.getSlackLink()).thenReturn(slackLink);
        when(slackSlashCommand.getUserId()).thenReturn(SLACK_USER_ID);
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.of(slackUser));
        when(slackUser.getUserKey()).thenReturn(USER);
        when(userAccessor.getExistingUserByKey(userKey)).thenReturn(confluenceUser);
        when(confluenceUserImpersonator.impersonate(confluenceUserCaptor.capture(), any(Supplier.class), anyString()))
                .thenAnswer(answer((ConfluenceUser user, Supplier action, String reason) -> action.get()));
        when(searchBuilder.buildSearch(eq(query), any(ConfluenceUser.class), anyInt(), anyInt()))
                .thenReturn(iSearch);
        when(searchManager.search(any(ISearch.class))).thenReturn(searchResults);
        when(searchManager.convertToEntities(searchResults, SearchManager.EntityVersionPolicy.LATEST_VERSION))
                .thenReturn(Arrays.asList(page, comment));
        when(attachmentBuilder.buildAttachment(page)).thenReturn(attachment);
        when(attachmentBuilder.searchFooter(query)).thenReturn(footerAttachment);
        when(attachmentBuilder.getSearchResultsTitleMessage(1, query)).thenReturn("search-res");
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        CommonTestUtil.bypass(asyncExecutor);

        target.slashCommand(slackSlashCommand);

        assertThat(confluenceUserCaptor.getValue(), equalTo(confluenceUser));
    }

    @Test
    public void slashCommand_shouldSendInvalidCommand() {
        when(slackSlashCommand.getText()).thenReturn("x ");
        when(slackSlashCommand.getResponseUrl()).thenReturn("ru");
        when(slackSlashCommand.getSlackLink()).thenReturn(slackLink);
        when(attachmentBuilder.getInvalidCommandMessage()).thenReturn("invmsg");
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        CommonTestUtil.bypass(asyncExecutor);

        target.slashCommand(slackSlashCommand);

        verify(slackClient).postResponse(eq("ru"), eq("ephemeral"), captor.capture());
        assertThat(captor.getValue().getText(), is("invmsg"));
    }

    @Test
    public void messageReceived_shouldSkipUnsupportedMessageSubType() {
        when(genericMessageSlackEvent.getSubtype()).thenReturn("x");
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);

        target.messageReceived(genericMessageSlackEvent);

        verify(genericMessageSlackEvent, never()).getText();
    }

    @Test
    public void messageReceived_shouldSkipHiddenMessage() {
        when(genericMessageSlackEvent.isHidden()).thenReturn(true);
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);

        target.messageReceived(genericMessageSlackEvent);

        verify(genericMessageSlackEvent, never()).getText();
    }

    // message event

    @Test
    public void messageReceived_shouldSendHelpDirectMessage() {
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(genericMessageSlackEvent.getText()).thenReturn("help");
        when(genericMessageSlackEvent.getChannelType()).thenReturn("im");
        when(genericMessageSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackLink.getBotUserId()).thenReturn("bot");
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(attachmentBuilder.getHelpMessage("bot", null)).thenReturn("helpmsg");
        CommonTestUtil.bypass(asyncExecutor);

        target.messageReceived(genericMessageSlackEvent);

        verify(slackClient).postMessage(captor.capture());
        assertThat(captor.getValue().getText(), is("helpmsg"));
        assertThat(captor.getValue().getChannel(), is(CHANNEL_ID));
    }

    @Test
    public void messageReceived_shouldSendAccountOnMention() {
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(genericMessageSlackEvent.getText()).thenReturn("  account <@bot>");
        when(genericMessageSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackLink.getBotUserId()).thenReturn("bot");
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(attachmentBuilder.getAccountMessage(null)).thenReturn("accmsg");
        CommonTestUtil.bypass(asyncExecutor);

        target.messageReceived(genericMessageSlackEvent);

        verify(slackClient).postEphemeralMessage(ephemeralCaptor.capture());
        assertThat(ephemeralCaptor.getValue().getText(), is("accmsg"));
        assertThat(ephemeralCaptor.getValue().getChannel(), is(CHANNEL_ID));
    }

    @Test
    public void messageReceived_shouldSendConnectMessage() {
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(genericMessageSlackEvent.getText()).thenReturn(URL + "/test?pageId=123");
        when(genericMessageSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(genericMessageSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(attachmentBuilder.getPleaseAuthenticateMessage()).thenReturn("notconn");
        when(attachmentBuilder.baseUrl()).thenReturn(URL);
        when(pageManager.getAbstractPage(123)).thenReturn(page);
        CommonTestUtil.bypass(asyncExecutor);

        target.messageReceived(genericMessageSlackEvent);

        verify(slackClient).postEphemeralMessage(ephemeralCaptor.capture());
        assertThat(ephemeralCaptor.getValue().getAttachments().get(0).getText(), is("notconn"));
        assertThat(ephemeralCaptor.getValue().getChannel(), is(CHANNEL_ID));
        assertThat(ephemeralCaptor.getValue().getUser(), is(SLACK_USER_ID));
    }

    @Test
    public void messageReceived_shouldSharePageId() {
        when(genericMessageSlackEvent.getText()).thenReturn(URL + "/test?pageId=123");
        when(pageManager.getAbstractPage(123)).thenReturn(page);
        testSharePage();
    }

    @Test
    public void messageReceived_shouldSharePageWithTitle() {
        when(genericMessageSlackEvent.getText()).thenReturn(URL + "/display/TEAM/Page+1");
        when(pageManager.getPage("TEAM", "Page 1")).thenReturn(page);
        testSharePage();
    }

    @Test
    public void messageReceived_shouldSharePageWithTinyLink() {
        when(genericMessageSlackEvent.getText()).thenReturn(URL + "/x/" + new TinyUrl(123L).getIdentifier());
        when(pageManager.getAbstractPage(123L)).thenReturn(page);
        testSharePage();
    }

    private void testSharePage() {
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(genericMessageSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(genericMessageSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(genericMessageSlackEvent.getThreadTimestamp()).thenReturn("ts");
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(attachmentBuilder.baseUrl()).thenReturn(URL);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(false);
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.of(slackUser));
        when(userAccessor.getExistingUserByKey(eq(userKey))).thenReturn(confluenceUser);
        when(slackUser.getUserKey()).thenReturn(USER);
        when(permissionManager.hasPermission(confluenceUser, Permission.VIEW, page)).thenReturn(true);
        CommonTestUtil.bypass(asyncExecutor);
        when(attachmentBuilder.buildAttachment(page)).thenReturn(attachment);
        when(page.getSpace()).thenReturn(space);

        target.messageReceived(genericMessageSlackEvent);

        verify(eventPublisher, times(2)).publish(contentSharedCaptor.capture());
        assertThat(contentSharedCaptor.getValue().getTeamId(), is(TEAM_ID));
        assertThat(contentSharedCaptor.getValue().getChannelId(), is(CHANNEL_ID));
        assertThat(contentSharedCaptor.getValue().getThreadTs(), is("ts"));
        assertThat(contentSharedCaptor.getValue().getLink(), isEmptyString());
        assertThat(contentSharedCaptor.getValue().getUser(), nullValue());
        assertThat(contentSharedCaptor.getValue().getSpace(), is(space));
        assertThat(contentSharedCaptor.getValue().getAttachment(), sameInstance(attachment));
    }

    @Test
    public void messageReceived_shouldShareBlogPost() {
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(genericMessageSlackEvent.getText()).thenReturn(URL + "/display/TEAM/2019/02/06/Simple+blog");
        when(genericMessageSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(genericMessageSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(genericMessageSlackEvent.getThreadTimestamp()).thenReturn("ts");
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(attachmentBuilder.baseUrl()).thenReturn(URL);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(false);
        when(pageManager.getBlogPost(eq("TEAM"), eq("Simple blog"), calendar.capture())).thenReturn(blogPost);
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.of(slackUser));
        when(userAccessor.getExistingUserByKey(eq(userKey))).thenReturn(confluenceUser);
        when(slackUser.getUserKey()).thenReturn(USER);
        when(permissionManager.hasPermission(confluenceUser, Permission.VIEW, blogPost)).thenReturn(true);
        CommonTestUtil.bypass(asyncExecutor);
        when(attachmentBuilder.buildAttachment(blogPost)).thenReturn(attachment);
        when(blogPost.getSpace()).thenReturn(space);

        target.messageReceived(genericMessageSlackEvent);

        verify(eventPublisher, times(2)).publish(contentSharedCaptor.capture());
        assertThat(calendar.getValue().get(Calendar.DAY_OF_MONTH), is(6));
        assertThat(calendar.getValue().get(Calendar.MONTH), is(Calendar.FEBRUARY));
        assertThat(calendar.getValue().get(Calendar.YEAR), is(2019));

        assertThat(contentSharedCaptor.getValue().getTeamId(), is(TEAM_ID));
        assertThat(contentSharedCaptor.getValue().getChannelId(), is(CHANNEL_ID));
        assertThat(contentSharedCaptor.getValue().getThreadTs(), is("ts"));
        assertThat(contentSharedCaptor.getValue().getAttachment(), sameInstance(attachment));
    }

    @Test
    public void messageReceived_shouldShareQuestion() {
        when(customContentManager.getById(1736742L)).thenReturn(customContentEntityObject);
        when(genericMessageSlackEvent.getText()).thenReturn(URL + "/display/TEAM/questions/1736742/what-do-i-do-now");
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(genericMessageSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(genericMessageSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(genericMessageSlackEvent.getThreadTimestamp()).thenReturn("ts");
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(attachmentBuilder.baseUrl()).thenReturn(URL);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(false);
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.of(slackUser));
        when(userAccessor.getExistingUserByKey(eq(userKey))).thenReturn(confluenceUser);
        when(slackUser.getUserKey()).thenReturn(USER);
        when(permissionManager.hasPermission(confluenceUser, Permission.VIEW, customContentEntityObject)).thenReturn(true);
        CommonTestUtil.bypass(asyncExecutor);
        when(attachmentBuilder.buildAttachment(customContentEntityObject)).thenReturn(attachment);
        when(customContentEntityObject.getPluginModuleKey()).thenReturn(QuestionType.QUESTION.pluginModuleKey());
        when(customContentEntityObject.getSpace()).thenReturn(space);

        target.messageReceived(genericMessageSlackEvent);

        verify(eventPublisher, times(2)).publish(contentSharedCaptor.capture());

        assertThat(contentSharedCaptor.getValue().getTeamId(), is(TEAM_ID));
        assertThat(contentSharedCaptor.getValue().getChannelId(), is(CHANNEL_ID));
        assertThat(contentSharedCaptor.getValue().getThreadTs(), is("ts"));
        assertThat(contentSharedCaptor.getValue().getAttachment(), sameInstance(attachment));
    }

    @Test
    public void messageReceived_shouldShareAnswer() {
        when(customContentManager.getById(12345L)).thenReturn(customContentEntityObject);
        when(genericMessageSlackEvent.getText()).thenReturn(URL + "/display/TEAM/questions/1736742/answers/12345");
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(genericMessageSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(genericMessageSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(genericMessageSlackEvent.getThreadTimestamp()).thenReturn("ts");
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(attachmentBuilder.baseUrl()).thenReturn(URL);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(false);
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.of(slackUser));
        when(userAccessor.getExistingUserByKey(eq(userKey))).thenReturn(confluenceUser);
        when(slackUser.getUserKey()).thenReturn(USER);
        when(permissionManager.hasPermission(confluenceUser, Permission.VIEW, customContentEntityObject)).thenReturn(true);
        CommonTestUtil.bypass(asyncExecutor);
        when(attachmentBuilder.buildAttachment(customContentEntityObject)).thenReturn(attachment);
        when(customContentEntityObject.getPluginModuleKey()).thenReturn(QuestionType.ANSWER.pluginModuleKey());
        when(customContentEntityObject.getSpace()).thenReturn(space);

        target.messageReceived(genericMessageSlackEvent);

        verify(eventPublisher, times(2)).publish(contentSharedCaptor.capture());

        assertThat(contentSharedCaptor.getValue().getTeamId(), is(TEAM_ID));
        assertThat(contentSharedCaptor.getValue().getChannelId(), is(CHANNEL_ID));
        assertThat(contentSharedCaptor.getValue().getThreadTs(), is("ts"));
        assertThat(contentSharedCaptor.getValue().getAttachment(), sameInstance(attachment));
    }

    // linkShared event

    @Test
    public void linkShared_shouldSendConnectMessage() {
        when(linkSharedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(linkSharedSlackEvent.getLinks()).thenReturn(Collections.singletonList(createLink(
                URL + "/test?pageId=123")));
        when(linkSharedSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(linkSharedSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(linkSharedSlackEvent.getMessageTimestamp()).thenReturn("mts");
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(attachmentBuilder.getPleaseAuthenticateMessage()).thenReturn("notconn");
        when(attachmentBuilder.baseUrl()).thenReturn(URL);
        when(pageManager.getAbstractPage(123)).thenReturn(page);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(true);
        CommonTestUtil.bypass(asyncExecutor);

        target.linkShared(linkSharedSlackEvent);

        verify(slackClient).unfurlWithoutAuthentication(CHANNEL_ID, "mts", "notconn");
    }

    @Test
    public void linkShared_shouldSharePageId() {
        String link = URL + "/test?pageId=123";
        when(linkSharedSlackEvent.getLinks()).thenReturn(Collections.singletonList(createLink(link)));
        when(pageManager.getAbstractPage(123)).thenReturn(page);
        testLinkShared(link);
    }

    @Test
    public void linkShared_shouldSharePageWithTitle() {
        String link = URL + "/display/TEAM/Page+1";
        when(linkSharedSlackEvent.getLinks()).thenReturn(Collections.singletonList(createLink(link)));
        when(pageManager.getPage("TEAM", "Page 1")).thenReturn(page);
        testLinkShared(link);
    }

    @Test
    public void linkShared_shouldSharePageWithTinyLink() {
        String link = URL + "/x/" + new TinyUrl(123L).getIdentifier();
        when(linkSharedSlackEvent.getLinks()).thenReturn(Collections.singletonList(createLink(link)));
        when(pageManager.getAbstractPage(123L)).thenReturn(page);
        testLinkShared(link);
    }

    private void testLinkShared(String link) {
        when(linkSharedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(linkSharedSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(linkSharedSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(linkSharedSlackEvent.getMessageTimestamp()).thenReturn("mts");
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(slackClient.withUserTokenIfAvailable(slackUser)).thenReturn(Optional.of(slackClient));
        when(attachmentBuilder.buildAttachment(page)).thenReturn(attachment);
        when(attachmentBuilder.baseUrl()).thenReturn(URL);
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.of(slackUser));
        when(userAccessor.getExistingUserByKey(eq(userKey))).thenReturn(confluenceUser);
        when(slackUser.getUserKey()).thenReturn(USER);
        when(permissionManager.hasPermission(confluenceUser, Permission.VIEW, page)).thenReturn(true);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(true);
        CommonTestUtil.bypass(asyncExecutor);

        target.linkShared(linkSharedSlackEvent);

        verify(slackClient).unfurl(eq(CHANNEL_ID), eq("mts"), unfurlsCaptor.capture());
        assertThat(unfurlsCaptor.getValue(), hasKey(link));
        assertThat(unfurlsCaptor.getValue().get(link), sameInstance(attachment));
    }

    @Test
    public void linkShared_shouldShareBlogPost() {
        when(linkSharedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        String link = URL + "/display/TEAM/2019/02/06/Simple+blog";
        when(linkSharedSlackEvent.getLinks()).thenReturn(Collections.singletonList(createLink(link)));
        when(linkSharedSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(linkSharedSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(linkSharedSlackEvent.getMessageTimestamp()).thenReturn("mts");
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(slackClient.withUserTokenIfAvailable(slackUser)).thenReturn(Optional.of(slackClient));
        when(attachmentBuilder.buildAttachment(blogPost)).thenReturn(attachment);
        when(attachmentBuilder.baseUrl()).thenReturn(URL);
        when(pageManager.getBlogPost(eq("TEAM"), eq("Simple blog"), calendar.capture())).thenReturn(blogPost);
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.of(slackUser));
        when(userAccessor.getExistingUserByKey(eq(userKey))).thenReturn(confluenceUser);
        when(slackUser.getUserKey()).thenReturn(USER);
        when(permissionManager.hasPermission(confluenceUser, Permission.VIEW, blogPost)).thenReturn(true);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(true);
        CommonTestUtil.bypass(asyncExecutor);

        target.linkShared(linkSharedSlackEvent);

        verify(slackClient).unfurl(eq(CHANNEL_ID), eq("mts"), unfurlsCaptor.capture());
        assertThat(unfurlsCaptor.getValue(), hasKey(link));
        assertThat(unfurlsCaptor.getValue().get(link), sameInstance(attachment));
    }

    @Test
    public void linkShared_shouldShareQuestion() {
        when(customContentManager.getById(1736742L)).thenReturn(customContentEntityObject);
        String link = URL + "/display/TEAM/questions/1736742/what-do-i-do-now";
        when(linkSharedSlackEvent.getLinks()).thenReturn(Collections.singletonList(createLink(link)));
        when(linkSharedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(linkSharedSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(linkSharedSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(linkSharedSlackEvent.getMessageTimestamp()).thenReturn("mts");
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(slackClient.withUserTokenIfAvailable(slackUser)).thenReturn(Optional.of(slackClient));
        when(attachmentBuilder.buildAttachment(customContentEntityObject)).thenReturn(attachment);
        when(attachmentBuilder.baseUrl()).thenReturn(URL);
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.of(slackUser));
        when(userAccessor.getExistingUserByKey(eq(userKey))).thenReturn(confluenceUser);
        when(slackUser.getUserKey()).thenReturn(USER);
        when(permissionManager.hasPermission(confluenceUser, Permission.VIEW, customContentEntityObject)).thenReturn(true);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(true);
        CommonTestUtil.bypass(asyncExecutor);

        target.linkShared(linkSharedSlackEvent);

        verify(slackClient).unfurl(eq(CHANNEL_ID), eq("mts"), unfurlsCaptor.capture());
        assertThat(unfurlsCaptor.getValue(), hasKey(link));
        assertThat(unfurlsCaptor.getValue().get(link), sameInstance(attachment));
    }

    private LinkSharedSlackEvent.Link createLink(String url) {
        LinkSharedSlackEvent.Link l = new LinkSharedSlackEvent.Link();
        l.setUrl(url);
        return l;
    }
}
