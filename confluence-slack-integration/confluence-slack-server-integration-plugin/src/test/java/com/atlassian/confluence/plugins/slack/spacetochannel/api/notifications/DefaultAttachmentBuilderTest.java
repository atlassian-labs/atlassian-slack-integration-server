package com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications;

import com.atlassian.confluence.content.CustomContentEntityObject;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.SpaceContentEntityObject;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.TinyUrl;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionType;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.PersonalInformation;
import com.atlassian.confluence.user.PersonalInformationManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.timezone.TimeZoneManager;
import com.atlassian.sal.api.user.UserKey;
import com.github.seratch.jslack.api.model.Attachment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.atlassian.plugins.slack.util.LinkHelper.decorateWithOrigin;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultAttachmentBuilderTest {
    private static final String BASE_URL = "url";
    private static final String DECORATED_BASE_URL = decorateWithOrigin("url", "site");
    private static final String PATH = "/path";

    @Mock
    private ApplicationProperties applicationProperties;
    @Mock(lenient = true)
    private I18nResolver i18nResolver;
    @Mock
    private PersonalInformationManager personalInformationManager;
    @Mock
    private LocaleManager localeManager;
    @Mock
    private SettingsManager settingsManager;
    @Mock
    private TimeZoneManager timeZoneManager;

    @Mock
    private SpaceContentEntityObject spaceContentEntityObject;
    @Mock
    private CustomContentEntityObject customContentEntityObject;
    @Mock
    private CustomContentEntityObject customContentEntityObjectParent;
    @Mock
    private AbstractPage page;
    @Mock
    private ContentEntityObject contentEntityObject;
    @Mock
    private Space space;
    @Mock
    private PersonalInformation personalInformation;
    @Mock
    private PersonalInformation personalInformation2;
    @Mock
    private ConfluenceUser confluenceUser;
    @Mock
    private ConfluenceUser confluenceUser2;
    @Mock
    private Settings settings;

    @InjectMocks
    private DefaultAttachmentBuilder target;

    @Test
    public void baseUrl_shouldReturnExpectedValue() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);

        String result = target.baseUrl();

        assertThat(result, is(BASE_URL));
    }

    @Test
    public void pageLink_shouldReturnExpectedValue() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(spaceContentEntityObject.getUrlPath()).thenReturn(PATH);
        when(spaceContentEntityObject.getDisplayTitle()).thenReturn("title <");
        when(spaceContentEntityObject.getType()).thenReturn("blogpost");

        String result = target.pageLink(spaceContentEntityObject);

        assertThat(result, is("<" + decorateWithOrigin(BASE_URL + PATH, "blogpost") + "|title &lt;>"));
    }

    @Test
    public void pageLink_shouldReturnExpectedValueForAbstractPage() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(page.getId()).thenReturn(1L);
        when(page.getDisplayTitle()).thenReturn("title <");
        when(page.getType()).thenReturn("page");

        String result = target.pageLink(page);

        assertThat(result, is("<" + decorateWithOrigin(BASE_URL + "/x/" + new TinyUrl(page).getIdentifier(), "page") + "|title &lt;>"));
    }

    @Test
    public void pageLink_shouldReturnExpectedValueForAnswer() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(customContentEntityObject.getPluginModuleKey()).thenReturn(QuestionType.ANSWER.pluginModuleKey());
        when(customContentEntityObject.getUrlPath()).thenReturn(PATH);
        when(customContentEntityObject.getParent()).thenReturn(customContentEntityObjectParent);
        when(customContentEntityObject.getType()).thenReturn("page");
        when(customContentEntityObjectParent.getDisplayTitle()).thenReturn("title <");

        String result = target.pageLink(customContentEntityObject);

        assertThat(result, is("<" + decorateWithOrigin(BASE_URL + PATH, "page") + "|title &lt;>"));
    }

    @Test
    public void pageLink_shouldReturnExpectedValueForQuestion() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(customContentEntityObject.getPluginModuleKey()).thenReturn(QuestionType.QUESTION.pluginModuleKey());
        when(customContentEntityObject.getUrlPath()).thenReturn(PATH);
        when(customContentEntityObject.getDisplayTitle()).thenReturn("title <");
        when(customContentEntityObject.getType()).thenReturn("page");

        String result = target.pageLink(customContentEntityObject);

        assertThat(result, is("<" + decorateWithOrigin(BASE_URL + PATH, "page") + "|title &lt;>"));
    }

    @Test
    public void pageLink_shouldReturnExpectedValueForGenericCustomContent() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(customContentEntityObject.getUrlPath()).thenReturn(PATH);
        when(customContentEntityObject.getDisplayTitle()).thenReturn("title <");
        when(customContentEntityObject.getType()).thenReturn("page");

        String result = target.pageLink(customContentEntityObject);

        assertThat(result, is("<" + decorateWithOrigin(BASE_URL + PATH, "page") + "|title &lt;>"));
    }

    @Test
    public void userLink_shouldReturnExpectedValue() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(personalInformationManager.getOrCreatePersonalInformation(confluenceUser)).thenReturn(personalInformation);
        when(personalInformation.getUrlPath()).thenReturn(PATH);
        when(confluenceUser.getFullName()).thenReturn("name <");

        String result = target.userLink(confluenceUser);

        assertThat(result, is("<" + decorateWithOrigin(BASE_URL + PATH, "user") + "|name &lt;>"));
    }

    @Test
    public void userLink_shouldReturnExpectedValueForNullUser() {
        when(i18nResolver.getText("plugins.slack.common.anonymous")).thenReturn("txt");

        String result = target.userLink(null);

        assertThat(result, is("txt"));
    }

    @Test
    public void calendarLink_shouldReturnExpectedValue() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(space.getUrlPath()).thenReturn(PATH);

        String result = target.calendarLink(space, "cal");

        assertThat(result, is("<" + decorateWithOrigin(BASE_URL + PATH + "/calendars", "calendar") + "|cal>"));
    }

    @Test
    public void getSlackPrettyDate_shouldReturnExpectedValue() {
        when(localeManager.getSiteDefaultLocale()).thenReturn(Locale.ENGLISH);
        when(timeZoneManager.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("America/Chicago"));

        String result = target.getSlackPrettyDate(1551985533000L);

        assertThat(result, is("<!date^" + (1551985533L) + "^{date_short_pretty}|March 7, 2019>"));
    }

    @Test
    public void getSlackPrettyTime_shouldReturnExpectedValue() {
        when(localeManager.getSiteDefaultLocale()).thenReturn(Locale.ENGLISH);
        when(timeZoneManager.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("America/Chicago"));

        // `DateTimeFormatter` whitespace separator may change among JDKs
        // Since `equals` method compares each character by its ascii value for equality,
        // horizontal whitespace character (\\h) is replaced with a space for consistency
        String result = target.getSlackPrettyTime(1551985533000L).replaceAll("\\h", " ");

        assertThat(result, is("<!date^" + (1551985533L) + "^{time}|1:05 PM>"));
    }

    @Test
    public void buildAttachment_shouldReturnExpectedValue() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(spaceContentEntityObject.getSpace()).thenReturn(space);
        when(spaceContentEntityObject.getUrlPath()).thenReturn(PATH);
        when(spaceContentEntityObject.getDisplayTitle()).thenReturn("page <");
        when(spaceContentEntityObject.getCreationDate()).thenReturn(new Date(1551985533000L));
        when(spaceContentEntityObject.getCreator()).thenReturn(confluenceUser);
        when(spaceContentEntityObject.getType()).thenReturn("page");
        when(space.getDisplayTitle()).thenReturn("space <");
        when(space.getUrlPath()).thenReturn(PATH);
        when(personalInformationManager.getOrCreatePersonalInformation(confluenceUser)).thenReturn(personalInformation);
        when(personalInformation.getUrlPath()).thenReturn(PATH);
        when(confluenceUser.getFullName()).thenReturn("name <");
        when(localeManager.getSiteDefaultLocale()).thenReturn(Locale.ENGLISH);
        when(timeZoneManager.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("America/Chicago"));

        when(i18nResolver.getText(eq("slack.activity.title"), anyString(), anyString()))
                .thenAnswer(args -> args.getArgument(1).toString() + " in " + args.getArgument(2));
        when(i18nResolver.getText(eq("slack.activity.footer"), anyString(), anyString()))
                .thenAnswer(args -> "Published " + args.getArgument(1).toString() + " by " + args.getArgument(2));

        Attachment result = target.buildAttachment(spaceContentEntityObject);

        String pageLink = "<" + decorateWithOrigin(BASE_URL + PATH, "page") + "|page &lt;>";
        String spaceLink = "<" + decorateWithOrigin(BASE_URL + PATH, "space") + "|space &lt;>";
        String userLink = "<" + decorateWithOrigin(BASE_URL + PATH, "user") + "|name &lt;>";
        String formattedDate = "<!date^" + (1551985533L) + "^{date_short_pretty}|March 7, 2019>";

        assertThat(result.getText(), is("*" + pageLink + "* in *" + spaceLink + "*"));
        assertThat(result.getFallback(), is("page < in space <"));
        assertThat(result.getFooter(), is("Published " + formattedDate + " by " + userLink));
        assertThat(result.getColor(), is("#2684ff"));
        assertThat(result.getMrkdwnIn(), contains("text", "pretext"));
    }

    @Test
    public void buildAttachment_shouldReturnExpectedValueWithModificationBySameUser() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(spaceContentEntityObject.getSpace()).thenReturn(space);
        when(spaceContentEntityObject.getUrlPath()).thenReturn(PATH);
        when(spaceContentEntityObject.getDisplayTitle()).thenReturn("page <");
        when(spaceContentEntityObject.getCreationDate()).thenReturn(new Date(1551985530000L));
        when(spaceContentEntityObject.getLastModificationDate()).thenReturn(new Date(1551985533000L));
        when(spaceContentEntityObject.getLastModifier()).thenReturn(confluenceUser);
        when(spaceContentEntityObject.getCreator()).thenReturn(confluenceUser);
        when(spaceContentEntityObject.getType()).thenReturn("page");
        when(space.getDisplayTitle()).thenReturn("space <");
        when(space.getUrlPath()).thenReturn(PATH);
        when(personalInformationManager.getOrCreatePersonalInformation(confluenceUser)).thenReturn(personalInformation);
        when(personalInformation.getUrlPath()).thenReturn(PATH);
        when(confluenceUser.getFullName()).thenReturn("name <");
        when(confluenceUser.getKey()).thenReturn(new UserKey("uk"));
        when(localeManager.getSiteDefaultLocale()).thenReturn(Locale.ENGLISH);
        when(timeZoneManager.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("America/Chicago"));

        when(i18nResolver.getText(eq("slack.activity.title"), anyString(), anyString()))
                .thenAnswer(args -> args.getArgument(1).toString() + " in " + args.getArgument(2));
        when(i18nResolver.getText(eq("slack.activity.footer.modified"), anyString(), anyString()))
                .thenAnswer(args -> "Updated " + args.getArgument(1).toString() + " by " + args.getArgument(2));

        Attachment result = target.buildAttachment(spaceContentEntityObject);

        String pageLink = "<" + decorateWithOrigin(BASE_URL + PATH, "page") + "|page &lt;>";
        String spaceLink = "<" + decorateWithOrigin(BASE_URL + PATH, "space") + "|space &lt;>";
        String userLink = "<" + decorateWithOrigin(BASE_URL + PATH, "user") + "|name &lt;>";
        String formattedDate = "<!date^" + (1551985533L) + "^{date_short_pretty}|March 7, 2019>";

        assertThat(result.getText(), is("*" + pageLink + "* in *" + spaceLink + "*"));
        assertThat(result.getFallback(), is("page < in space <"));
        assertThat(result.getFooter(), is("Updated " + formattedDate + " by " + userLink));
        assertThat(result.getColor(), is("#2684ff"));
        assertThat(result.getMrkdwnIn(), contains("text", "pretext"));
    }

    @Test
    public void buildAttachment_shouldReturnExpectedValueWithModificationByAnotherUser() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(spaceContentEntityObject.getSpace()).thenReturn(space);
        when(spaceContentEntityObject.getUrlPath()).thenReturn(PATH);
        when(spaceContentEntityObject.getDisplayTitle()).thenReturn("page <");
        when(spaceContentEntityObject.getCreationDate()).thenReturn(new Date(1551985530000L));
        when(spaceContentEntityObject.getLastModificationDate()).thenReturn(new Date(1551985533000L));
        when(spaceContentEntityObject.getCreator()).thenReturn(confluenceUser);
        when(spaceContentEntityObject.getLastModifier()).thenReturn(confluenceUser2);
        when(spaceContentEntityObject.getType()).thenReturn("blogpost");
        when(space.getDisplayTitle()).thenReturn("space <");
        when(space.getUrlPath()).thenReturn(PATH);
        when(personalInformationManager.getOrCreatePersonalInformation(confluenceUser)).thenReturn(personalInformation);
        when(personalInformationManager.getOrCreatePersonalInformation(confluenceUser2)).thenReturn(personalInformation2);
        when(personalInformation.getUrlPath()).thenReturn(PATH);
        when(personalInformation2.getUrlPath()).thenReturn(PATH + "2");
        when(confluenceUser.getFullName()).thenReturn("name <");
        when(confluenceUser.getKey()).thenReturn(new UserKey("uk"));
        when(confluenceUser2.getFullName()).thenReturn("name2");
        when(confluenceUser2.getKey()).thenReturn(new UserKey("uk2"));
        when(localeManager.getSiteDefaultLocale()).thenReturn(Locale.ENGLISH);
        when(timeZoneManager.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("America/Chicago"));

        when(i18nResolver.getText(eq("slack.activity.title"), anyString(), anyString()))
                .thenAnswer(args -> args.getArgument(1).toString() + " in " + args.getArgument(2));
        when(i18nResolver.getText(eq("slack.activity.footer"), anyString(), anyString()))
                .thenAnswer(args -> "Published " + args.getArgument(1).toString() + " by " + args.getArgument(2));
        when(i18nResolver.getText(eq("slack.activity.footer.modified"), anyString(), anyString()))
                .thenAnswer(args -> "Updated " + args.getArgument(1).toString() + " by " + args.getArgument(2));

        Attachment result = target.buildAttachment(spaceContentEntityObject);

        String pageLink = "<" + decorateWithOrigin(BASE_URL + PATH, "blogpost") + "|page &lt;>";
        String spaceLink = "<" + decorateWithOrigin(BASE_URL + PATH, "space") + "|space &lt;>";
        String userLink = "<" + decorateWithOrigin(BASE_URL + PATH, "user") + "|name &lt;>";
        String userLink2 = "<" + decorateWithOrigin(BASE_URL + PATH + "2", "user") + "|name2>";
        String formattedDate = "<!date^" + (1551985530L) + "^{date_short_pretty}|March 7, 2019>";
        String formattedDate2 = "<!date^" + (1551985533L) + "^{date_short_pretty}|March 7, 2019>";

        assertThat(result.getText(), is("*" + pageLink + "* in *" + spaceLink + "*"));
        assertThat(result.getFallback(), is("page < in space <"));
        assertThat(result.getFooter(), is("Published " + formattedDate + " by " + userLink +
                " | Updated " + formattedDate2 + " by " + userLink2));
        assertThat(result.getColor(), is("#2684ff"));
        assertThat(result.getMrkdwnIn(), contains("text", "pretext"));
    }

    @Test
    public void buildAttachment_shouldReturnExpectedValueForAnswer() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(customContentEntityObject.getSpace()).thenReturn(space);
        when(customContentEntityObject.getUrlPath()).thenReturn(PATH);
        when(customContentEntityObject.getCreationDate()).thenReturn(new Date(1551985533000L));
        when(customContentEntityObject.getCreator()).thenReturn(confluenceUser);
        when(customContentEntityObject.getPluginModuleKey()).thenReturn(QuestionType.ANSWER.pluginModuleKey());
        when(customContentEntityObject.getParent()).thenReturn(customContentEntityObjectParent);
        when(customContentEntityObject.getBodyAsStringWithoutMarkup()).thenReturn("ac");
        when(customContentEntityObject.getType()).thenReturn("page");
        when(customContentEntityObjectParent.getDisplayTitle()).thenReturn("page <");
        when(space.getDisplayTitle()).thenReturn("space <");
        when(space.getUrlPath()).thenReturn(PATH);
        when(personalInformationManager.getOrCreatePersonalInformation(confluenceUser)).thenReturn(personalInformation);
        when(personalInformation.getUrlPath()).thenReturn(PATH);
        when(confluenceUser.getFullName()).thenReturn("name <");
        when(localeManager.getSiteDefaultLocale()).thenReturn(Locale.ENGLISH);
        when(timeZoneManager.getDefaultTimeZone()).thenReturn(TimeZone.getTimeZone("America/Chicago"));

        when(i18nResolver.getText(eq("slack.activity.title"), anyString(), anyString()))
                .thenAnswer(args -> args.getArgument(1).toString() + " in " + args.getArgument(2));
        when(i18nResolver.getText(eq("slack.activity.footer"), anyString(), anyString()))
                .thenAnswer(args -> "Published " + args.getArgument(1).toString() + " by " + args.getArgument(2));

        Attachment result = target.buildAttachment(customContentEntityObject);

        String pageLink = "<" + decorateWithOrigin(BASE_URL + PATH, "page") + "|page &lt;>";
        String spaceLink = "<" + decorateWithOrigin(BASE_URL + PATH, "space") + "|space &lt;>";
        String userLink = "<" + decorateWithOrigin(BASE_URL + PATH, "user") + "|name &lt;>";
        String formattedDate = "<!date^" + (1551985533L) + "^{date_short_pretty}|March 7, 2019>";

        assertThat(result.getTitle(), is(pageLink + " in " + spaceLink));
        assertThat(result.getText(), is("ac"));
        assertThat(result.getFallback(), is("page < in space <"));
        assertThat(result.getFooter(), is("Published " + formattedDate + " by " + userLink));
        assertThat(result.getColor(), is("#2684ff"));
        assertThat(result.getMrkdwnIn(), contains("text", "pretext"));
    }

    @Test
    public void getWelcomeMessage_shouldReturnExpectedValue() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(settingsManager.getGlobalSettings()).thenReturn(settings);
        when(settings.getSiteTitle()).thenReturn("st");
        when(i18nResolver.getText(
                "confluence.plugins.slack.notification.workspace.connected.welcome",
                DECORATED_BASE_URL + "|st",
                BASE_URL + "/plugins/servlet/slack/configure?teamId=T")
        ).thenReturn("txt");

        String result = target.getWelcomeMessage("T");

        assertThat(result, is(":tada: txt"));
    }

    @Test
    public void getWelcomeMessage_shouldReturnExpectedValueWhenSiteTitleIsNotAvailable() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(applicationProperties.getDisplayName()).thenReturn("dn");
        when(settingsManager.getGlobalSettings()).thenReturn(settings);
        when(i18nResolver.getText(
                "confluence.plugins.slack.notification.workspace.connected.welcome",
                DECORATED_BASE_URL + "|dn",
                BASE_URL + "/plugins/servlet/slack/configure?teamId=T")
        ).thenReturn("txt");

        String result = target.getWelcomeMessage("T");

        assertThat(result, is(":tada: txt"));
    }

    @Test
    public void getAccountMessage_shouldReturnExpectedValue() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(personalInformationManager.getOrCreatePersonalInformation(confluenceUser)).thenReturn(personalInformation);
        when(personalInformation.getUrlPath()).thenReturn(PATH);
        when(confluenceUser.getFullName()).thenReturn("name <");
        String userLink = "<" + decorateWithOrigin(BASE_URL + PATH, "user") + "|name &lt;>";
        String oauthLink = BASE_URL + "/plugins/servlet/slack/view-oauth-sessions";
        when(i18nResolver.getText("confluence.plugins.slack.user.link.account.details.message", userLink, oauthLink))
                .thenReturn("txt");

        String result = target.getAccountMessage(confluenceUser);

        assertThat(result, is("txt"));
    }

    @Test
    public void getAccountMessage_shouldReturnExpectedValueForNullUser() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        String oauthLink = BASE_URL + "/plugins/servlet/slack/view-oauth-sessions";
        when(i18nResolver.getText("confluence.plugins.slack.user.link.no.account.message", oauthLink)).thenReturn("txt");

        String result = target.getAccountMessage(null);

        assertThat(result, is("txt"));
    }

    @Test
    public void getPleaseAuthenticateMessage_shouldReturnExpectedValue() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        String oauthLink = BASE_URL + "/plugins/servlet/slack/view-oauth-sessions";
        when(i18nResolver.getText("confluence.plugins.slack.user.link.confirm.account.message", oauthLink)).thenReturn("txt");

        String result = target.getPleaseAuthenticateMessage();

        assertThat(result, is("txt"));
    }

    @Test
    public void getHelpMessage_shouldReturnExpectedValueWhenCommand() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(settingsManager.getGlobalSettings()).thenReturn(settings);
        when(settings.getSiteTitle()).thenReturn("st");
        when(i18nResolver.getText("confluence.plugins.slack.command.show.help.search", "/cmd"))
                .thenReturn("searchDesc");
        when(i18nResolver.getText(
                "confluence.plugins.slack.command.show.help",
                "<@bot>",
                DECORATED_BASE_URL + "|st",
                "/cmd",
                "searchDesc")).thenReturn("txt");
        String result = target.getHelpMessage("bot", "/cmd");
        assertThat(result, is("txt"));
    }

    @Test
    public void getHelpMessage_shouldReturnExpectedValueWhenMention() {
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        when(settingsManager.getGlobalSettings()).thenReturn(settings);
        when(settings.getSiteTitle()).thenReturn("st");
        when(i18nResolver.getText(
                "confluence.plugins.slack.command.show.help",
                "<@bot>",
                DECORATED_BASE_URL + "|st",
                "<@bot>",
                "")).thenReturn("txt");
        String result = target.getHelpMessage("bot", null);
        assertThat(result, is("txt"));
    }

    @Test
    public void getInvalidCommandMessage_shouldReturnExpectedValue() {
        when(i18nResolver.getText("confluence.plugins.slack.command.invalid")).thenReturn("txt");
        String result = target.getInvalidCommandMessage();
        assertThat(result, is("txt"));
    }

    @Test
    public void getNoSearchResultsMessage_shouldReturnExpectedValue() {
        when(i18nResolver.getText("confluence.plugins.slack.command.search.empty")).thenReturn("txt");
        String result = target.getNoSearchResultsMessage();
        assertThat(result, is("txt"));
    }

    @Test
    public void getSearchResultsTitleMessage_shouldReturnExpectedValue() {
        String query = "someQuery";
        int resultsFound = 3;
        when(i18nResolver.getText("confluence.plugins.slack.command.search.title", resultsFound, query)).thenReturn("txt");
        String result = target.getSearchResultsTitleMessage(resultsFound, query);
        assertThat(result, is("txt"));
    }

    @Test
    public void searchLink_shouldReturnExpectedValue() {
        String query = "someQuery";
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        String result = target.searchLink(query);
        assertThat(result, is(decorateWithOrigin(BASE_URL + "/wiki/dosearchsite.action?queryString=someQuery", "search")));
    }

    @Test
    public void searchFooter_shouldReturnExpectedValue() {
        String query = "someQuery";
        when(i18nResolver.getText("confluence.plugins.slack.command.search.view.in.confluence")).thenReturn("txt");
        when(applicationProperties.getBaseUrl(UrlMode.CANONICAL)).thenReturn(BASE_URL);
        Attachment attachment = target.searchFooter(query);
        assertThat(attachment.getFallback(), is("txt"));
        assertThat(attachment.getActions().get(0).getText(), is("txt"));
        assertThat(attachment.getActions().get(0).getUrl(), is(decorateWithOrigin(BASE_URL + "/wiki/dosearchsite.action?queryString=someQuery", "search")));
    }
}
