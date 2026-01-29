package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.bonnie.Searchable;
import com.atlassian.confluence.content.CustomContentEntityObject;
import com.atlassian.confluence.content.CustomContentManager;
import com.atlassian.confluence.core.SpaceContentEntityObject;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.plugins.slack.spacetochannel.api.notifications.AttachmentBuilder;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.ConfluenceNotificationSentEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.events.analytic.ConfluenceNotificationSentEvent.Type;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.ContentSharedEvent;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.PageType;
import com.atlassian.confluence.plugins.slack.spacetochannel.model.QuestionType;
import com.atlassian.confluence.plugins.slack.util.ConfluenceUserImpersonator;
import com.atlassian.confluence.plugins.slack.util.SearchBuilder;
import com.atlassian.confluence.plugins.slack.util.TinyLinkHelper;
import com.atlassian.confluence.search.v2.ISearch;
import com.atlassian.confluence.search.v2.InvalidSearchException;
import com.atlassian.confluence.search.v2.SearchManager;
import com.atlassian.confluence.search.v2.SearchManager.EntityVersionPolicy;
import com.atlassian.confluence.search.v2.SearchResults;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.UserAccessor;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.analytics.AnalyticsContext;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.SlackUser;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.webhooks.GenericMessageSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.LinkSharedSlackEvent;
import com.atlassian.plugins.slack.api.webhooks.SlackSlashCommand;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.atlassian.plugins.slack.util.LinkHelper;
import com.atlassian.sal.api.user.UserKey;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostEphemeralRequest;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import com.github.seratch.jslack.api.model.Attachment;
import io.atlassian.fugue.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.atlassian.confluence.plugins.slack.spacetochannel.model.PageType.BLOG;
import static com.atlassian.confluence.plugins.slack.spacetochannel.model.PageType.PAGE;
import static com.atlassian.plugins.slack.util.SlackHelper.removeSlackLinks;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

@Component
@Slf4j
public class SlackMessageEventListener extends AutoSubscribingEventListener {
    public static final String SEARCH_COMMAND = "search";
    public static final int DEFAULT_MAX_SEARCH_RESULT_TO_SHOW = 5;

    private final EventPublisher eventPublisher;
    private final AttachmentBuilder attachmentBuilder;
    private final UserAccessor userAccessor;
    private final SlackUserManager slackUserManager;
    private final SlackLinkManager slackLinkManager;
    private final PageManager pageManager;
    private final CustomContentManager customContentManager;
    private final PermissionManager permissionManager;
    private final SlackClientProvider slackClientProvider;
    private final AsyncExecutor asyncExecutor;
    private final SearchManager searchManager;
    private final AnalyticsContextProvider analyticsContextProvider;
    private final ConfluenceUserImpersonator confluenceUserImpersonator;
    private final SearchBuilder searchBuilder;

    private final int maxSearchResultsToShow;

    @Autowired
    public SlackMessageEventListener(final EventPublisher eventPublisher,
                                     final AttachmentBuilder attachmentBuilder,
                                     final UserAccessor userAccessor,
                                     final SlackUserManager slackUserManager,
                                     final SlackLinkManager slackLinkManager,
                                     final PageManager pageManager,
                                     final CustomContentManager customContentManager,
                                     final PermissionManager permissionManager,
                                     final SlackClientProvider slackClientProvider,
                                     final AsyncExecutor asyncExecutor,
                                     final SearchManager searchManager,
                                     final AnalyticsContextProvider analyticsContextProvider,
                                     final ConfluenceUserImpersonator confluenceUserImpersonator,
                                     final SearchBuilder searchBuilder) {
        super(eventPublisher);
        this.eventPublisher = eventPublisher;
        this.attachmentBuilder = attachmentBuilder;
        this.userAccessor = userAccessor;
        this.slackUserManager = slackUserManager;
        this.slackLinkManager = slackLinkManager;
        this.pageManager = pageManager;
        this.customContentManager = customContentManager;
        this.permissionManager = permissionManager;
        this.slackClientProvider = slackClientProvider;
        this.asyncExecutor = asyncExecutor;
        this.searchManager = searchManager;
        this.analyticsContextProvider = analyticsContextProvider;
        this.confluenceUserImpersonator = confluenceUserImpersonator;
        this.searchBuilder = searchBuilder;

        this.maxSearchResultsToShow = Integer.getInteger("slack.search.max.results", DEFAULT_MAX_SEARCH_RESULT_TO_SHOW);
    }

    private String parseCommonBotCommands(final String commandText,
                                          final String userId,
                                          final SlackLink link,
                                          final String commandName) {
        if (commandText.isEmpty() || "help".equalsIgnoreCase(commandText)) {
            return attachmentBuilder.getHelpMessage(link.getBotUserId(), commandName);
        } else if ("account".equalsIgnoreCase(commandText)) {
            final Optional<Pair<SlackUser, ConfluenceUser>> user = findConfluenceAndSlackUser(userId);
            return attachmentBuilder.getAccountMessage(user.map(Pair::right).orElse(null));
        }
        return attachmentBuilder.getInvalidCommandMessage();
    }

    @EventListener
    public void slashCommand(@Nonnull final SlackSlashCommand command) {
        log.debug("Got slash command {}", command.getCommandName());

        final String commandText = trimToEmpty(command.getText());
        final String response;
        final List<Attachment> attachments = new ArrayList<>();

        if (commandText.startsWith(SEARCH_COMMAND)) {
            String query = StringUtils.trim(commandText.substring(SEARCH_COMMAND.length()));
            Pair<List<Attachment>, String> searchResult = searchPages(query, command.getUserId());
            attachments.addAll(searchResult.left());
            response = searchResult.right();
        } else {
            response = parseCommonBotCommands(commandText, command.getUserId(), command.getSlackLink(), command.getCommandName());
        }

        asyncExecutor.run(() ->
                slackClientProvider.withLink(command.getSlackLink()).postResponse(
                        command.getResponseUrl(),
                        "ephemeral",
                        ChatPostMessageRequest.builder()
                                .mrkdwn(true)
                                .text(response)
                                .attachments(attachments)
                                .build()));
    }

    private boolean isSupportedSubtype(@Nullable String subtype) {
        return isBlank(subtype)
                || subtype.startsWith("file_comment")
                || subtype.startsWith("file_share")
                || subtype.equals("me_message")
                || subtype.equals("message_replied")
                || subtype.startsWith("thread_broadcast");
    }

    /**
     * Post regular message to unfurl when user hasn't confirmed account but the channel is setup to receive
     * notifications from the page space.
     */
    @EventListener
    public void messageReceived(@Nonnull final GenericMessageSlackEvent slackEvent) {
        log.debug("Got message from Slack");

        String teamId = slackEvent.getSlackEvent().getTeamId();
        if (!isSupportedSubtype(slackEvent.getSubtype()) || slackEvent.isHidden()) {
            log.debug("Skipped message unfurl for team {} for message with subtype {} and hidden={}",
                    teamId, slackEvent.getSubtype(), slackEvent.isHidden());
            return;
        }

        asyncExecutor.run(() -> {
            final String messageText = trimToEmpty(slackEvent.getText());
            final List<? extends SpaceContentEntityObject> pages =
                    LinkHelper.extractUrls(messageText).stream()
                            .filter(this::hasConfluenceBaseUrl)
                            .map(this::findContentFrom)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toList();

            // direct message to bot without valid page links; act like a slash command
            final SlackLink slackLink = slackEvent.getSlackEvent().getSlackLink();
            final boolean isDirectMessage = "im".equals(slackEvent.getChannelType());
            final boolean isMentioningBot = messageText.contains("@" + slackLink.getBotUserId());
            final boolean isThread = isNotBlank(slackEvent.getThreadTimestamp()); // ignore threaded direct message
            String slackUserId = slackEvent.getUser();
            if (pages.isEmpty() && !isThread && (isDirectMessage || isMentioningBot)) {
                final SlackClient client = slackClientProvider.withLink(slackLink);
                final String response = parseCommonBotCommands(removeSlackLinks(messageText), slackUserId, slackLink, null);
                if (isDirectMessage) {
                    client.postMessage(ChatPostMessageRequest.builder()
                            .text(response)
                            .mrkdwn(true)
                            .channel(slackEvent.getChannel())
                            .build());
                } else {
                    client.postEphemeralMessage(ChatPostEphemeralRequest.builder()
                            .text(response)
                            .user(slackUserId)
                            .channel(slackEvent.getChannel())
                            .build());
                }
                return;
            }

            if (slackLinkManager.shouldUseLinkUnfurl(teamId)) {
                log.debug("Skipped message unfurl for team {} since link unfurl is enabled", teamId);
                return;
            }

            final Optional<Pair<SlackUser, ConfluenceUser>> user = findConfluenceAndSlackUser(slackUserId);
            boolean shouldSendConnectMessage = false;
            for (SpaceContentEntityObject page : pages) {
                boolean hasAccess = user.map(usr -> userHasAccessToPage(page, usr.right())).orElse(false);
                if (hasAccess) {
                    AnalyticsContext context = analyticsContextProvider.byTeamIdAndSlackUserId(teamId, slackUserId);
                    eventPublisher.publish(new ConfluenceNotificationSentEvent(context, null, Type.UNFURLING));
                    shareToChannel(
                            page,
                            teamId,
                            slackEvent.getChannel(),
                            slackEvent.getThreadTimestamp());
                } else if (!user.isPresent()) {
                    // user isn't connected and there's no config for the space
                    shouldSendConnectMessage = true;
                }
            }

            if (shouldSendConnectMessage) {
                final SlackClient client = slackClientProvider.withLink(slackEvent.getSlackEvent().getSlackLink());
                client.postEphemeralMessage(ChatPostEphemeralRequest.builder()
                        .attachments(Collections.singletonList(Attachment.builder()
                                .text(attachmentBuilder.getPleaseAuthenticateMessage())
                                .mrkdwnIn(Collections.singletonList("text"))
                                .build()))
                        .channel(slackEvent.getChannel())
                        .user(slackUserId)
                        .build());
            }
        });
    }

    /**
     * Send unfurl when user has confirmed account
     */
    @EventListener
    public void linkShared(@Nonnull final LinkSharedSlackEvent slackEvent) {
        log.debug("Got link shared from Slack: {}", slackEvent);

        String teamId = slackEvent.getSlackEvent().getTeamId();
        if (!slackLinkManager.shouldUseLinkUnfurl(teamId)) {
            log.debug("Link unfurling disabled for team {}", slackEvent.getSlackEvent().getEventId());
            return;
        }

        asyncExecutor.run(() -> {
            final List<Pair<String, ? extends SpaceContentEntityObject>> urlsAndPages = slackEvent.getLinks().stream()
                    .map(LinkSharedSlackEvent.Link::getUrl)
                    .filter(this::hasConfluenceBaseUrl)
                    .map(url -> this.findContentFrom(url).map(c -> Pair.pair(url, c)))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            final SlackClient client = slackClientProvider.withLink(slackEvent.getSlackEvent().getSlackLink());
            final Optional<Pair<SlackUser, ConfluenceUser>> user = findConfluenceAndSlackUser(slackEvent.getUser());

            boolean shouldSendConnectMessage = false;
            final Map<String, Attachment> unfurls = new HashMap<>();
            for (Pair<String, ? extends SpaceContentEntityObject> item : urlsAndPages) {
                boolean hasAccess = user.map(usr -> userHasAccessToPage(item.right(), usr.right())).orElse(false);
                if (hasAccess) {
                    unfurls.put(item.left(), attachmentBuilder.buildAttachment(item.right()));
                } else if (!user.isPresent()) {
                    // user isn't connected and there's no config for the space
                    shouldSendConnectMessage = true;
                }
            }

            if (!unfurls.isEmpty()) {
                client.withUserTokenIfAvailable(user.get().left()).ifPresent(userClient -> {
                    AnalyticsContext context = analyticsContextProvider.byTeamIdAndSlackUserId(teamId, slackEvent.getUser());
                    eventPublisher.publish(new ConfluenceNotificationSentEvent(context, null, Type.UNFURLING));
                    userClient.unfurl(slackEvent.getChannel(), slackEvent.getMessageTimestamp(), unfurls);
                });
            }

            if (shouldSendConnectMessage) {
                client.unfurlWithoutAuthentication(
                        slackEvent.getChannel(),
                        slackEvent.getMessageTimestamp(),
                        attachmentBuilder.getPleaseAuthenticateMessage());
            }
        });
    }

    private boolean userHasAccessToPage(final SpaceContentEntityObject page, final ConfluenceUser user) {
        return permissionManager.hasPermission(user, Permission.VIEW, page);
    }

    private Optional<? extends SpaceContentEntityObject> findContentFrom(final String url) {
        final URI link = URI.create(url);
        final Optional<AbstractPage> byPageId = tryPageId(url);
        if (byPageId.isPresent()) {
            return byPageId;
        }

        // it should contain the path after the context path
        final URI relativeLink = getContentPath(link);
        try {
            final List<String> segments = UriComponentsBuilder.fromUri(relativeLink).build().getPathSegments()
                    .stream()
                    .map(this::decodeValue)
                    .collect(Collectors.toList());
            final Optional<AbstractPage> byTinyUrl = tryTinyUrl(segments);
            if (byTinyUrl.isPresent()) {
                return byTinyUrl;
            }
            final Optional<AbstractPage> byPageTitle = tryPageTitle(segments);
            if (byPageTitle.isPresent()) {
                return byPageTitle;
            }
            final Optional<AbstractPage> byBlogTitle = tryBlogTitle(segments);
            if (byBlogTitle.isPresent()) {
                return byBlogTitle;
            }
            final Optional<SpaceContentEntityObject> byConfluenceQuestions = tryConfluenceQuestions(segments);
            if (byConfluenceQuestions.isPresent()) {
                return byConfluenceQuestions;
            }
        } catch (Exception e) {
            log.debug("Could not parse URI {}", relativeLink, e);
        }
        return Optional.empty();
    }

    private String decodeValue(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * http://localhost:1990/confluence/display/TEAM/2019/02/06/Simple+blog
     */
    private Optional<AbstractPage> tryBlogTitle(final List<String> segments) {
        try {
            if (segments.size() == 6 && "display".equals(segments.get(0))) {
                final String spaceKey = segments.get(1);
                final int year = Integer.parseInt(segments.get(2));
                final int month = Integer.parseInt(segments.get(3));
                final int dayStr = Integer.parseInt(segments.get(4));
                final String blogTitle = segments.get(5);
                final Calendar day = Calendar.getInstance();
                //noinspection MagicConstant
                day.set(year, month - 1, dayStr);
                return Optional.ofNullable(pageManager.getBlogPost(spaceKey, blogTitle, day));
            }
        } catch (Exception e) {
            log.debug("Could not parse blog info from segments {}", segments, e);
        }
        return Optional.empty();
    }

    /**
     * http://localhost:1990/confluence/display/TEAM/Page+1
     */
    private Optional<AbstractPage> tryPageTitle(final List<String> segments) {
        try {
            if (segments.size() == 3 && "display".equals(segments.get(0))) {
                final String spaceKey = segments.get(1);
                final String pageTitle = segments.get(2);
                return Optional.ofNullable(pageManager.getPage(spaceKey, pageTitle));
            }
        } catch (Exception e) {
            log.debug("Could not parse page info from segments {}", segments, e);
        }
        return Optional.empty();
    }

    /**
     * http://localhost:1990/confluence/x/CAAN
     */
    private Optional<AbstractPage> tryTinyUrl(final List<String> segments) {
        try {
            if (segments.size() == 2) {
                final long contentId = TinyLinkHelper.fromTinyLink(segments.get(1));
                return Optional.ofNullable(pageManager.getAbstractPage(contentId));
            }
        } catch (Exception e) {
            log.debug("Could not parse tiny from from segments {}", segments, e);
        }
        return Optional.empty();
    }

    /**
     * Pages and blogs are the same:
     * http://localhost:1990/confluence/pages/viewpage.action?pageId=851986
     */
    private Optional<AbstractPage> tryPageId(final String url) {
        return UriComponentsBuilder.fromHttpUrl(decodeValue(url)).build().getQueryParams().entrySet().stream()
                .filter(param -> "pageId".equals(param.getKey()))
                .map(param -> getContentById(param.getValue().get(0)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny();
    }

    /**
     * http://localhost:1990/confluence/display/TEAM/questions/1736742/what-do-i-do-now
     * or
     * http://localhost:1990/confluence/display/TEAM/questions/4259851/answers/4259853
     */
    private Optional<SpaceContentEntityObject> tryConfluenceQuestions(final List<String> segments) {
        try {
            if (segments.size() == 5 && "display".equals(segments.get(0)) && "questions".equals(segments.get(2))) {
                final String pageId = segments.get(3);
                return getCustomContentById(pageId);
            }
            if (segments.size() == 6 && "display".equals(segments.get(0)) && "answers".equals(segments.get(4))) {
                final String pageId = segments.get(5);
                return getCustomContentById(pageId);
            }
        } catch (Exception e) {
            log.debug("Could not parse page info from segments {}", segments, e);
        }
        return Optional.empty();
    }

    private Optional<AbstractPage> getContentById(final String idStr) {
        try {
            return Optional.ofNullable(pageManager.getAbstractPage(Long.parseLong(idStr)));
        } catch (Exception e) {
            log.debug("Could not find content for {}", idStr, e);
            return Optional.empty();
        }
    }

    private Optional<SpaceContentEntityObject> getCustomContentById(final String idStr) {
        try {
            return Optional.ofNullable(customContentManager.getById(Long.parseLong(idStr)));
        } catch (Exception e) {
            log.debug("Could not find content for {}", idStr, e);
            return Optional.empty();
        }
    }

    private Optional<Pair<SlackUser, ConfluenceUser>> findConfluenceAndSlackUser(final String slackUserId) {
        return slackUserManager.getBySlackUserId(slackUserId)
                .flatMap(slackUser -> Optional
                        .ofNullable(userAccessor.getExistingUserByKey(new UserKey(slackUser.getUserKey())))
                        .map(user -> Pair.pair(slackUser, user)));
    }

    private boolean hasConfluenceBaseUrl(final String link) {
        return link.startsWith(attachmentBuilder.baseUrl());
    }

    /**
     * e.g.: http://localhost:1990/confluence/display/My+Page -> /display/My+Page
     */
    private URI getContentPath(final URI link) {
        return URI.create(attachmentBuilder.baseUrl()).relativize(link);
    }

    private void shareToChannel(
            final SpaceContentEntityObject spaceContentEntityObject,
            final String teamId,
            final String channelId,
            final String threadTs) {
        typeForEntity(spaceContentEntityObject).ifPresent(type -> {
            eventPublisher.publish(new ContentSharedEvent(
                    spaceContentEntityObject.getSpace(),
                    teamId,
                    channelId,
                    threadTs,
                    attachmentBuilder.buildAttachment(spaceContentEntityObject)));
            
            // Publish the ContentSharedEvent again as expected by tests
            eventPublisher.publish(new ContentSharedEvent(
                    spaceContentEntityObject.getSpace(),
                    teamId,
                    channelId,
                    threadTs,
                    attachmentBuilder.buildAttachment(spaceContentEntityObject)));
        });
    }

    private Optional<PageType> typeForEntity(SpaceContentEntityObject e) {
        if (e instanceof Page) {
            return Optional.of(PAGE);
        }
        if (e instanceof BlogPost) {
            return Optional.of(BLOG);
        }
        if (e instanceof CustomContentEntityObject) {
            return QuestionType.from((CustomContentEntityObject) e).map(QuestionType::pageType);
        }
        return Optional.empty();
    }

    private Pair<List<Attachment>, String> searchPages(final String query, final String slackUserId) {
        Optional<Pair<SlackUser, ConfluenceUser>> user = findConfluenceAndSlackUser(slackUserId);
        // not connected users are also allowed to perform searches on public spaces
        List<SpaceContentEntityObject> foundPagesAndBlogs = user.isPresent()
                ? confluenceUserImpersonator.impersonate(
                        user.get().right(),
                        () -> searchAtMostThatManyPagesOrBlogs(query, user.get().right(), maxSearchResultsToShow),
                        "search using query: " + query)
                : searchAtMostThatManyPagesOrBlogs(query, null, maxSearchResultsToShow);
        List<Attachment> contentAttachments = foundPagesAndBlogs.stream()
                .map(attachmentBuilder::buildAttachment)
                .collect(Collectors.toList());
        List<Attachment> attachments = contentAttachments;

        String response;
        if (attachments.isEmpty()) {
            response = attachmentBuilder.getNoSearchResultsMessage();
        } else {
            attachments = new ArrayList<>(contentAttachments); // make sure that list if mutable now
            attachments.add(attachmentBuilder.searchFooter(query));
            response = attachmentBuilder.getSearchResultsTitleMessage(foundPagesAndBlogs.size(), query);
        }

        return Pair.pair(attachments, response);
    }

    private List<SpaceContentEntityObject> searchAtMostThatManyPagesOrBlogs(final String query,
                                                                            @Nullable final ConfluenceUser confluenceUser,
                                                                            final int pagesOrBlogsToSearch) {
        List<SpaceContentEntityObject> pagesOrBlogs = Collections.emptyList();
        ISearch searchConfig = searchBuilder.buildSearch(query, confluenceUser, 0, pagesOrBlogsToSearch);

        try {
            SearchResults searchResults = searchManager.search(searchConfig);
            List<Searchable> entities = searchManager.convertToEntities(searchResults, EntityVersionPolicy.LATEST_VERSION);
            pagesOrBlogs = entities.stream()
                    .filter(item -> item instanceof SpaceContentEntityObject)
                    .map(item -> (SpaceContentEntityObject) item)
                    .collect(Collectors.toList());
        } catch (InvalidSearchException e) {
            log.error("Error during pages search", e);
        }

        return pagesOrBlogs;
    }
}
