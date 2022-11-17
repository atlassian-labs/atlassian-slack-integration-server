package com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications;

import com.atlassian.confluence.content.CustomContentEntityObject;
import com.atlassian.confluence.core.SpaceContentEntityObject;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionType;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.PersonalInformationManager;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.timezone.TimeZoneManager;
import com.github.seratch.jslack.api.model.Action;
import com.github.seratch.jslack.api.model.Attachment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Collections;

import static com.atlassian.confluence.plugins.slack.util.TinyLinkHelper.tinyLink;
import static com.atlassian.plugins.slack.util.LinkHelper.absoluteUrl;
import static com.atlassian.plugins.slack.util.LinkHelper.decorateWithOrigin;
import static com.atlassian.plugins.slack.util.SlackHelper.escapeSignsForSlackLink;
import static org.apache.commons.lang3.StringUtils.defaultString;

@Component
@Slf4j
public class DefaultAttachmentBuilder implements AttachmentBuilder {
    private final ApplicationProperties applicationProperties;
    private final I18nResolver i18nResolver;
    private final PersonalInformationManager personalInformationManager;
    private final LocaleManager localeManager;
    private final SettingsManager settingsManager;
    private final TimeZoneManager timeZoneManager;

    @Autowired
    public DefaultAttachmentBuilder(
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties,
            final I18nResolver i18nResolver,
            final PersonalInformationManager personalInformationManager,
            final LocaleManager localeManager,
            final SettingsManager settingsManager,
            final TimeZoneManager timeZoneManager) {
        this.applicationProperties = applicationProperties;
        this.i18nResolver = i18nResolver;
        this.personalInformationManager = personalInformationManager;
        this.localeManager = localeManager;
        this.settingsManager = settingsManager;
        this.timeZoneManager = timeZoneManager;
    }

    @Override
    public Attachment buildAttachment(final SpaceContentEntityObject content) {
        final Space space = content.getSpace();
        final String answerContent = getAnswerContent(content);
        final String title = answerContent != null
                ? i18nResolver.getText("slack.activity.title", pageLink(content), spaceLink(space))
                : i18nResolver.getText("slack.activity.title", "*" + pageLink(content) + "*", "*" + spaceLink(space) + "*");
        return Attachment.builder()
                .title(answerContent != null ? title : null)
                .text(defaultString(answerContent, title))
                .fallback(i18nResolver.getText("slack.activity.title", getTitle(content), space.getDisplayTitle()))
                .footer(calculateFooterTimestampText(content))
                .color("#2684ff")
                .mrkdwnIn(Arrays.asList("text", "pretext"))
                .build();
    }

    private String calculateFooterTimestampText(final SpaceContentEntityObject content) {
        final ConfluenceUser creator = content.getCreator();
        final String slackPublishedTimestamp = i18nResolver.getText(
                "slack.activity.footer",
                getSlackPrettyDate(content.getCreationDate().getTime()),
                userLink(creator));

        final int version = content.getVersion();
        if (version == 1 || content.getLastModifier() == null) {
            return slackPublishedTimestamp;
        } else {
            final ConfluenceUser lastModifier = content.getLastModifier();
            final String slackUpdatedTimestamp = i18nResolver.getText(
                    "slack.activity.footer.modified",
                    getSlackPrettyDate(content.getLastModificationDate().getTime()),
                    userLink(lastModifier));

            final boolean isCreatorLastModifier = creator != null
                    && lastModifier != null
                    && creator.getKey().equals(lastModifier.getKey());
            if (isCreatorLastModifier) {
                return slackUpdatedTimestamp;
            } else {
                return slackPublishedTimestamp + " | " + slackUpdatedTimestamp;
            }
        }
    }

    private DateTimeFormatter getDateTimeFormatter() {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(localeManager.getSiteDefaultLocale());
    }

    @Override
    public String getSlackPrettyDate(final long epochMillis) {
        final String formattedDate = getDateTimeFormatter().format(Instant
                .ofEpochMilli(epochMillis)
                .atZone(timeZoneManager.getDefaultTimeZone().toZoneId()));
        return "<!date^" + (epochMillis / 1000) + "^{date_short_pretty}|" + formattedDate + ">";
    }

    @Override
    public String getSlackPrettyTime(final long epochMillis) {
        final DateTimeFormatter formatter = DateTimeFormatter
                .ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(localeManager.getSiteDefaultLocale());
        final String formattedDate = formatter.format(Instant
                .ofEpochMilli(epochMillis)
                .atZone(timeZoneManager.getDefaultTimeZone().toZoneId()));
        return "<!date^" + (epochMillis / 1000) + "^{time}|" + formattedDate + ">";
    }

    @Override
    public String getPleaseAuthenticateMessage() {
        return i18nResolver.getText("confluence.plugins.slack.user.link.confirm.account.message", oAuthSessionsPageUrl());
    }

    @Override
    public String getWelcomeMessage(final String teamId) {
        return ":tada: " + i18nResolver.getText(
                "confluence.plugins.slack.notification.workspace.connected.welcome",
                decorateWithOrigin(baseUrl(), "site") + "|" + escapeSignsForSlackLink(getApplicationTitle()),
                absoluteUrl("/plugins/servlet/slack/configure?teamId=" + teamId, applicationProperties));
    }

    @Override
    public String getHelpMessage(final String botUserId, final String commandName) {
        final String botMention = "<@" + botUserId + ">";
        final String handleText = defaultString(commandName, botMention);
        final String searchCommandDescription = commandName != null
                ? i18nResolver.getText("confluence.plugins.slack.command.show.help.search", commandName)
                : "";
        return i18nResolver.getText(
                "confluence.plugins.slack.command.show.help",
                botMention,
                decorateWithOrigin(baseUrl(), "site") + "|" + escapeSignsForSlackLink(getApplicationTitle()),
                handleText,
                searchCommandDescription);
    }

    private String getApplicationTitle() {
        final String title = settingsManager.getGlobalSettings().getSiteTitle();
        return StringUtils.isNotBlank(title)
                ? title
                : applicationProperties.getDisplayName();
    }

    @Override
    public String getAccountMessage(final ConfluenceUser user) {
        if (user != null) {
            return i18nResolver.getText(
                    "confluence.plugins.slack.user.link.account.details.message",
                    userLink(user),
                    oAuthSessionsPageUrl());
        }
        return i18nResolver.getText("confluence.plugins.slack.user.link.no.account.message", oAuthSessionsPageUrl());
    }

    @Override
    public String getInvalidCommandMessage() {
        return i18nResolver.getText("confluence.plugins.slack.command.invalid");
    }

    private String oAuthSessionsPageUrl() {
        return absoluteUrl("/plugins/servlet/slack/view-oauth-sessions", applicationProperties);
    }

    @Override
    public String userLink(final ConfluenceUser user) {
        if (user != null) {
            final String userLink = absoluteUrl(
                    personalInformationManager.getOrCreatePersonalInformation(user).getUrlPath(),
                    applicationProperties);
            return "<" + decorateWithOrigin(userLink, "user") + "|" + escapeSignsForSlackLink(user.getFullName()) + ">";
        } else {
            return i18nResolver.getText("plugins.slack.common.anonymous");
        }
    }

    @Override
    public String spaceLink(final Space space) {
        return "<" + decorateWithOrigin(absoluteUrl(space.getUrlPath(), applicationProperties), "space") + "|" +
                escapeSignsForSlackLink(space.getDisplayTitle()) + ">";
    }

    @Override
    public String calendarLink(final Space space, final String calendarName) {
        return "<" + decorateWithOrigin(absoluteUrl(space.getUrlPath() + "/calendars", applicationProperties), "calendar") + "|" +
                escapeSignsForSlackLink(calendarName) + ">";
    }

    @Override
    public String pageLink(final SpaceContentEntityObject page) {
        final String link = page instanceof AbstractPage
                ? tinyLink((AbstractPage) page, applicationProperties)
                : absoluteUrl(page.getUrlPath(), applicationProperties);

        return "<" + decorateWithOrigin(link, page.getType()) + "|" + escapeSignsForSlackLink(getTitle(page)) + ">";
    }

    @Override
    public String baseUrl() {
        return applicationProperties.getBaseUrl(UrlMode.CANONICAL);
    }

    private String getTitle(final SpaceContentEntityObject content) {
        if (content instanceof CustomContentEntityObject
                && QuestionType.isAnswerEvent((CustomContentEntityObject) content)) {
            return ((CustomContentEntityObject) content).getParent().getDisplayTitle();
        }
        return content.getDisplayTitle();
    }

    private String getAnswerContent(final SpaceContentEntityObject content) {
        if (content instanceof CustomContentEntityObject
                && QuestionType.isAnswerEvent((CustomContentEntityObject) content)) {
            return StringUtils.abbreviate(content.getBodyAsStringWithoutMarkup(), 200);
        }
        return null;
    }

    @Override
    public String getNoSearchResultsMessage() {
        return i18nResolver.getText("confluence.plugins.slack.command.search.empty");
    }

    @Override
    public String getSearchResultsTitleMessage(final int resultsFound, final String searchQuery) {
        return i18nResolver.getText("confluence.plugins.slack.command.search.title", resultsFound, searchQuery);
    }

    @Override
    public String searchLink(final String query) {
        String encodedQuery = query;
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to encode search query", e);
        }
        return decorateWithOrigin(absoluteUrl("/wiki/dosearchsite.action?queryString=" + encodedQuery, applicationProperties), "search");
    }

    @Override
    public Attachment searchFooter(final String query) {
        String buttonText = i18nResolver.getText("confluence.plugins.slack.command.search.view.in.confluence");
        return Attachment.builder()
                .fallback(buttonText)
                .actions(Collections.singletonList(Action.builder()
                        .type(Action.Type.BUTTON)
                        .text(buttonText)
                        .url(searchLink(query))
                        .build()))
                .build();
    }
}
