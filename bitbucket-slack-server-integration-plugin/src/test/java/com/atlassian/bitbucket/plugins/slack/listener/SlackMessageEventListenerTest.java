package com.atlassian.bitbucket.plugins.slack.listener;

import com.atlassian.bitbucket.plugins.slack.model.Unfurl;
import com.atlassian.bitbucket.plugins.slack.notification.NotificationPublisher;
import com.atlassian.bitbucket.plugins.slack.notification.renderer.SlackNotificationRenderer;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.UserService;
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
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostEphemeralRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.block.SectionBlock;
import com.github.seratch.jslack.api.model.block.composition.MarkdownTextObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;

import static com.atlassian.bitbucket.plugins.slack.util.TestUtil.getFirstSectionText;
import static com.atlassian.plugins.slack.test.util.CommonTestUtil.bypass;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
@MockitoSettings(strictness = Strictness.LENIENT)
public class SlackMessageEventListenerTest {
    private static final String TEAM_ID = "T";
    private static final String CHANNEL_ID = "C";
    private static final String SLACK_USER_ID = "SU";
    private static final String USER = "1";
    private static final String URL = "https://url.com";
    private static final String BOT_ID = "someBotId";

    @Mock
    private SlackUserManager slackUserManager;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackClientProvider slackClientProvider;
    @Mock
    private AsyncExecutor asyncExecutor;
    @Mock
    private SlackNotificationRenderer slackNotificationRenderer;
    @Mock
    private UserService userService;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private UnfurlLinkExtractor unfurlLinkExtractor;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;

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
    private ApplicationUser applicationUser;
    @Mock
    private SlackEvent slackEvent;
    @Mock
    private Attachment attachment;

    @Captor
    private ArgumentCaptor<ChatPostMessageRequest> captor;
    @Captor
    private ArgumentCaptor<ChatPostEphemeralRequest> ephemeralCaptor;
    @Captor
    private ArgumentCaptor<ChatPostMessageRequest.ChatPostMessageRequestBuilder> messageCaptor;
    @Captor
    private ArgumentCaptor<Map<String, Attachment>> unfurlsCaptor;

    @InjectMocks
    private SlackMessageEventListener target;

    @BeforeEach
    void setUp() {
        bypass(asyncExecutor);
        when(slackNotificationRenderer.richTextSectionBlock(anyString())).thenCallRealMethod();
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(URL);
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(genericMessageSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(slackEvent.getTeamId()).thenReturn(TEAM_ID);
        when(slackLink.getBotUserId()).thenReturn(BOT_ID);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackClientProvider.withLink(slackLink)).thenReturn(slackClient);
        when(userService.getUserById(1)).thenReturn(applicationUser);
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.of(slackUser));
        when(slackUser.getUserKey()).thenReturn(USER);
    }

    @Test
    public void slashCommand_shouldSendHelp() {
        when(slackSlashCommand.getText()).thenReturn("help ");
        when(slackSlashCommand.getResponseUrl()).thenReturn("ru");
        when(slackSlashCommand.getSlackLink()).thenReturn(slackLink);
        when(slackSlashCommand.getCommandName()).thenReturn("/cmd");
        when(slackLink.getBotUserId()).thenReturn(BOT_ID);
        when(slackNotificationRenderer.getHelpMessage(BOT_ID, "/cmd")).thenReturn("helpmsg");

        target.slashCommand(slackSlashCommand);

        verify(slackClient).postResponse(eq("ru"), eq("ephemeral"), captor.capture());
        assertThat(getFirstSectionText(captor.getValue()), is("helpmsg"));
        assertThat(captor.getValue().getText(), is("helpmsg"));
    }

    @Test
    public void slashCommand_shouldSendHelpWithEmptyMessage() {
        when(slackSlashCommand.getText()).thenReturn("   ");
        when(slackSlashCommand.getResponseUrl()).thenReturn("ru");
        when(slackSlashCommand.getSlackLink()).thenReturn(slackLink);
        when(slackSlashCommand.getCommandName()).thenReturn("/cmd");
        when(slackLink.getBotUserId()).thenReturn(BOT_ID);
        when(slackNotificationRenderer.getHelpMessage(BOT_ID, "/cmd")).thenReturn("helpmsg");

        target.slashCommand(slackSlashCommand);

        verify(slackClient).postResponse(eq("ru"), eq("ephemeral"), captor.capture());
        assertThat(getFirstSectionText(captor.getValue()), is("helpmsg"));
        assertThat(captor.getValue().getText(), is("helpmsg"));
    }

    @Test
    public void slashCommand_shouldSendAccount() {
        when(slackSlashCommand.getText()).thenReturn(" Account ");
        when(slackSlashCommand.getResponseUrl()).thenReturn("ru");
        when(slackSlashCommand.getSlackLink()).thenReturn(slackLink);
        when(slackSlashCommand.getUserId()).thenReturn(SLACK_USER_ID);
        when(slackNotificationRenderer.getAccountMessage(applicationUser)).thenReturn("accmsg");
        when(userService.getUserById(eq(1))).thenReturn(applicationUser);

        target.slashCommand(slackSlashCommand);

        verify(slackClient).postResponse(eq("ru"), eq("ephemeral"), captor.capture());
        assertThat(getFirstSectionText(captor.getValue()), is("accmsg"));
        assertThat(captor.getValue().getText(), is("accmsg"));
    }

    @Test
    public void slashCommand_shouldSendInvalidCommand() {
        when(slackSlashCommand.getText()).thenReturn("x ");
        when(slackSlashCommand.getResponseUrl()).thenReturn("ru");
        when(slackSlashCommand.getSlackLink()).thenReturn(slackLink);
        when(slackNotificationRenderer.getInvalidCommandMessage()).thenReturn("invmsg");

        target.slashCommand(slackSlashCommand);

        verify(slackClient).postResponse(eq("ru"), eq("ephemeral"), captor.capture());
        assertThat(getFirstSectionText(captor.getValue()), is("invmsg"));
        assertThat(captor.getValue().getText(), is("invmsg"));
    }

    @Test
    public void messageReceived_shouldSkipUnsupportedMessageSubType() {
        when(genericMessageSlackEvent.getSubtype()).thenReturn("x");

        target.messageReceived(genericMessageSlackEvent);

        verify(genericMessageSlackEvent, never()).getText();
    }

    @Test
    public void messageReceived_shouldSkipHiddenMessage() {
        when(genericMessageSlackEvent.isHidden()).thenReturn(true);

        target.messageReceived(genericMessageSlackEvent);

        verify(genericMessageSlackEvent, never()).getText();
    }

    // message event

    @Test
    public void messageReceived_shouldSendHelpDirectMessage() {
        when(genericMessageSlackEvent.getText()).thenReturn("help");
        when(genericMessageSlackEvent.getChannelType()).thenReturn("im");
        when(genericMessageSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackLink.getBotUserId()).thenReturn(BOT_ID);

        when(slackNotificationRenderer.getHelpMessage(BOT_ID, null)).thenReturn("helpmsg");

        target.messageReceived(genericMessageSlackEvent);

        verify(slackClient).postMessage(captor.capture());
        assertThat(getFirstSectionText(captor.getValue()), is("helpmsg"));
        assertThat(captor.getValue().getText(), is("helpmsg"));
        assertThat(captor.getValue().getChannel(), is(CHANNEL_ID));
    }

    @Test
    public void messageReceived_shouldSendConnectMessage() {
        when(genericMessageSlackEvent.getText()).thenReturn(URL + "/test");
        when(genericMessageSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(slackNotificationRenderer.getPleaseAuthenticateMessage()).thenReturn("notconn");
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.empty());

        target.messageReceived(genericMessageSlackEvent);

        verify(slackClient).postEphemeralMessage(ephemeralCaptor.capture());
        assertThat(((MarkdownTextObject) ((SectionBlock) ephemeralCaptor.getValue().getBlocks().get(0)).getText()).getText(), is("notconn"));
        assertThat(ephemeralCaptor.getValue().getText(), is("notconn"));
        assertThat(ephemeralCaptor.getValue().getChannel(), is(CHANNEL_ID));
        assertThat(ephemeralCaptor.getValue().getUser(), is(SLACK_USER_ID));
    }

    @Test
    public void messageReceived_shouldSharePullRequest() {
        String url = URL + "/anything";

        when(genericMessageSlackEvent.getText()).thenReturn(url);
        when(genericMessageSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(genericMessageSlackEvent.getThreadTimestamp()).thenReturn("ts");
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(false);

        when(unfurlLinkExtractor.findLinksToUnfurl(singletonList(url), applicationUser))
                .thenReturn(singletonList(new Unfurl(url, attachment)));

        target.messageReceived(genericMessageSlackEvent);

        verify(notificationPublisher).sendMessageAsync(eq(TEAM_ID), eq(CHANNEL_ID), messageCaptor.capture());

        final ChatPostMessageRequest message = messageCaptor.getValue().build();
        assertThat(message.isMrkdwn(), is(true));
        assertThat(message.getThreadTs(), is("ts"));
        assertThat(message.getAttachments().get(0), is(attachment));
    }

    @Test
    public void messageReceived_shouldNotShareLinkIfNoUnfurlsAreFound() {
        String url = URL + "/anything";

        when(genericMessageSlackEvent.getText()).thenReturn(url);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(false);

        when(unfurlLinkExtractor.findLinksToUnfurl(singletonList(url), applicationUser))
                .thenReturn(emptyList());

        target.messageReceived(genericMessageSlackEvent);

        verify(notificationPublisher, never()).sendMessageAsync(anyString(), anyString(), any());
    }

    @Test
    public void messageReceived_shouldSendHelp_whenMessageStartsWithBotMentionHelp() {
        String text = "<@" + BOT_ID + "> help";
        String msg = "helpmsg";

        when(genericMessageSlackEvent.getText()).thenReturn(text);
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackLink.getBotUserId()).thenReturn(BOT_ID);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(false);
        when(slackNotificationRenderer.getHelpMessage(BOT_ID, null)).thenReturn(msg);

        when(unfurlLinkExtractor.findLinksToUnfurl(singletonList(text), applicationUser))
                .thenReturn(emptyList());

        target.messageReceived(genericMessageSlackEvent);

        verify(slackClient).postEphemeralMessage(ephemeralCaptor.capture());
        ChatPostEphemeralRequest message = ephemeralCaptor.getValue();
        assertThat(getFirstSectionText(message), is(msg));
    }

    @Test
    public void messageReceived_shouldSendHelpWithEmptyMessage_whenMessageStartsWithBotMentionHelp() {
        String text = "<@" + BOT_ID + ">   ";
        String msg = "helpmsg";

        when(genericMessageSlackEvent.getText()).thenReturn(text);
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackLink.getBotUserId()).thenReturn(BOT_ID);
        when(slackNotificationRenderer.getHelpMessage(BOT_ID, null)).thenReturn(msg);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(false);

        when(unfurlLinkExtractor.findLinksToUnfurl(singletonList(text), applicationUser))
                .thenReturn(emptyList());

        target.messageReceived(genericMessageSlackEvent);

        verify(slackClient).postEphemeralMessage(ephemeralCaptor.capture());
        ChatPostEphemeralRequest message = ephemeralCaptor.getValue();
        assertThat(getFirstSectionText(message), is(msg));
    }

    @Test
    public void messageReceived_shouldSendAccount_whenMessageStartsWithBotMentionAccount() {
        String text = "<@" + BOT_ID + "> account";
        String msg = "some account description";

        when(genericMessageSlackEvent.getText()).thenReturn(text);
        when(genericMessageSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(slackEvent.getSlackLink()).thenReturn(slackLink);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(false);
        when(slackNotificationRenderer.getAccountMessage(applicationUser)).thenReturn(msg);

        when(unfurlLinkExtractor.findLinksToUnfurl(singletonList(text), applicationUser))
                .thenReturn(emptyList());

        target.messageReceived(genericMessageSlackEvent);

        verify(slackClient).postEphemeralMessage(ephemeralCaptor.capture());
        ChatPostEphemeralRequest message = ephemeralCaptor.getValue();
        assertThat(getFirstSectionText(message), is(msg));
    }

    @Test
    public void messageReceived_shouldSendInvalidCommand_whenMessageStartsWithBotMention() {
        String text = "<@" + BOT_ID + "> bring me pizza";
        String msg = "invalid command message";

        when(genericMessageSlackEvent.getText()).thenReturn(text);
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(false);
        when(slackNotificationRenderer.getInvalidCommandMessage()).thenReturn(msg);

        when(unfurlLinkExtractor.findLinksToUnfurl(singletonList(text), applicationUser))
                .thenReturn(emptyList());

        target.messageReceived(genericMessageSlackEvent);

        verify(slackClient).postEphemeralMessage(ephemeralCaptor.capture());
        ChatPostEphemeralRequest message = ephemeralCaptor.getValue();
        assertThat(getFirstSectionText(message), is(msg));
    }

    // linkShared event

    @Test
    public void linkShared_shouldSendConnectMessage() {
        when(linkSharedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(linkSharedSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(linkSharedSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(linkSharedSlackEvent.getMessageTimestamp()).thenReturn("mts");
        when(slackNotificationRenderer.getPleaseAuthenticateMessage()).thenReturn("notconn");
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(true);
        when(slackUserManager.getBySlackUserId(SLACK_USER_ID)).thenReturn(Optional.empty());

        target.linkShared(linkSharedSlackEvent);

        verify(slackClient).unfurlWithoutAuthentication(CHANNEL_ID, "mts", "notconn");
    }

    @Test
    public void linkShared_shouldSharePullRequest() {
        String link = URL + "/any";

        when(linkSharedSlackEvent.getLinks()).thenReturn(singletonList(createLink(link)));
        when(linkSharedSlackEvent.getSlackEvent()).thenReturn(slackEvent);
        when(linkSharedSlackEvent.getChannel()).thenReturn(CHANNEL_ID);
        when(linkSharedSlackEvent.getUser()).thenReturn(SLACK_USER_ID);
        when(linkSharedSlackEvent.getMessageTimestamp()).thenReturn("mts");

        when(unfurlLinkExtractor.findLinksToUnfurl(singletonList(link), applicationUser))
                .thenReturn(singletonList(new Unfurl(link, attachment)));

        when(slackClient.withUserTokenIfAvailable(slackUser)).thenReturn(Optional.of(slackClient));
        when(slackLinkManager.shouldUseLinkUnfurl(TEAM_ID)).thenReturn(true);
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(URL);

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
