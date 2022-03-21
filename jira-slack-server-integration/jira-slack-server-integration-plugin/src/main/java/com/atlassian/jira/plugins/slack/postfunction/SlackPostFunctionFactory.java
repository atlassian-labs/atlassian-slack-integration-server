package com.atlassian.jira.plugins.slack.postfunction;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.atlassian.jira.plugins.slack.util.PluginConstants;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.api.client.ConversationLoaderHelper;
import com.atlassian.plugins.slack.api.client.ConversationsAndLinks;
import com.atlassian.plugins.slack.api.client.SlackClient;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.rest.model.SlackChannelDTO;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.github.seratch.jslack.api.model.Conversation;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;
import io.atlassian.fugue.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is the factory class responsible for dealing with the UI for the post-function. This is typically where you put
 * default values into the velocity context and where you store user input.
 * <p>
 * NOTE: We add to keep the packaging to avoid having problems with previous workflows
 * cause the classpath is stored in the workflow xml
 */
public class SlackPostFunctionFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginFunctionFactory {
    private static final Logger log = LoggerFactory.getLogger(SlackPostFunctionFactory.class);

    private static final String LINK_RESOURCE = PluginConstants.PLUGIN_KEY + ":slack-user-link-support";
    static final String CHANNELS_TO_NOTIFY_JSON_PARAM = "channelsToNotifyJson";
    static final String JQL_FILTER_PARAM = "jql";
    static final String OWNER_PARAM = "owner";
    static final String MESSAGE_FILTER_PARAM = "message";

    private final SearchService searchService;
    private final JiraAuthenticationContext authenticationContext;
    private final SlackLinkManager slackLinkManager;
    private final PageBuilderService pageBuilderService;
    private final SlackClientProvider slackClientProvider;
    private final ApplicationProperties applicationProperties;
    private final ConversationLoaderHelper conversationLoaderHelper;

    public SlackPostFunctionFactory(
            final SearchService searchService,
            final JiraAuthenticationContext authenticationContext,
            final SlackLinkManager slackLinkManager,
            final PageBuilderService pageBuilderService,
            final SlackClientProvider slackClientProvider,
            @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties,
            final ConversationLoaderHelper conversationLoaderHelper) {
        this.slackLinkManager = slackLinkManager;
        this.searchService = searchService;
        this.authenticationContext = authenticationContext;
        this.pageBuilderService = pageBuilderService;
        this.slackClientProvider = slackClientProvider;
        this.applicationProperties = applicationProperties;
        this.conversationLoaderHelper = conversationLoaderHelper;
    }

    @Override
    protected void getVelocityParamsForInput(final Map<String, Object> velocityParams) {
        velocityParams.put("dto", new EditDto(getConfirmedLinks()));
        velocityParams.put("baseUrl", applicationProperties.getBaseUrl(UrlMode.ABSOLUTE));
        pageBuilderService.assembler().resources().requireWebResource(LINK_RESOURCE);
    }

    @Override
    protected void getVelocityParamsForEdit(final Map<String, Object> velocityParams,
                                            final AbstractDescriptor descriptor) {
        final FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
        final String jql = Strings.nullToEmpty((String) functionDescriptor.getArgs().get(JQL_FILTER_PARAM));
        final String value = Strings.nullToEmpty((String) functionDescriptor.getArgs().get(CHANNELS_TO_NOTIFY_JSON_PARAM));
        final String message = Strings.nullToEmpty((String) functionDescriptor.getArgs().get(MESSAGE_FILTER_PARAM));
        final String owner = Strings.nullToEmpty((String) functionDescriptor.getArgs().get(OWNER_PARAM));

        final EditDto editDto = new EditDto(
                getConfirmedLinks(),
                loadChannels(value),
                jql,
                isValidQuery(jql),
                message,
                owner);

        velocityParams.put("dto", editDto);
        velocityParams.put("baseUrl", applicationProperties.getBaseUrl(UrlMode.ABSOLUTE));
        pageBuilderService.assembler().resources().requireWebResource(LINK_RESOURCE);
    }

    @Override
    protected void getVelocityParamsForView(final Map<String, Object> velocityParams,
                                            final AbstractDescriptor descriptor) {
        if (!slackLinkManager.isAnyLinkDefined()) {
            velocityParams.put("dto", new ViewDto(false));
            return;
        }

        final FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
        final String jql = Strings.nullToEmpty((String) functionDescriptor.getArgs().get(JQL_FILTER_PARAM));

        final String value = (String) functionDescriptor.getArgs().get(CHANNELS_TO_NOTIFY_JSON_PARAM);
        final ViewDto viewDto = new ViewDto(loadChannels(value), jql, isValidQuery(jql));

        velocityParams.put("dto", viewDto);
        velocityParams.put("baseUrl", applicationProperties.getBaseUrl(UrlMode.ABSOLUTE));
        pageBuilderService.assembler().resources().requireWebResource(LINK_RESOURCE);
    }

    private List<SlackLink> getConfirmedLinks() {
        return slackLinkManager.getLinks().stream()
                .filter(link -> slackClientProvider
                        .withLink(link)
                        .withRemoteUserTokenIfAvailable()
                        .map(SlackClient::testToken)
                        .filter(Either::isRight)
                        .isPresent())
                .collect(Collectors.toList());
    }

    private List<SlackChannelDTO> loadChannels(final String json) {
        try {
            final List<ChannelToNotifyDto> channelsToNotifyIds = ChannelToNotifyDto.fromJson(json);
            final ConversationsAndLinks conversationsAndLinks = conversationLoaderHelper.conversationsAndLinksById(
                    channelsToNotifyIds,
                    item -> new ConversationKey(item.getTeamId(), item.getChannelId()),
                    (baseClient, channel) -> baseClient.withRemoteUserTokenIfAvailable().flatMap(
                            client -> client.getConversationsInfo(channel.getChannelId()).toOptional()));

            return channelsToNotifyIds.stream()
                    .flatMap(item -> {
                        final String teamId = item.getTeamId();
                        final String channelId = item.getChannelId();

                        final Optional<SlackLink> link = conversationsAndLinks.link(teamId);
                        if (!link.isPresent()) {
                            return Stream.empty();
                        }
                        final ConversationKey conversationKey = new ConversationKey(teamId, channelId);
                        final Optional<Conversation> conversation = conversationsAndLinks.conversation(conversationKey);
                        final String conversationName = conversation.map(Conversation::getName).orElseGet(() -> "id:" + channelId);
                        final boolean isPrivate = conversation.map(Conversation::isPrivate).orElse(false);
                        return Stream.of(new SlackChannelDTO(
                                teamId,
                                link.get().getTeamName(),
                                channelId,
                                conversationName,
                                isPrivate));
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error loading post function data", e);
            return Collections.emptyList();
        }
    }

    @Override
    public ImmutableMap<String, String> getDescriptorParams(Map<String, Object> formParams) {
        String channelsToNotifyJson;
        List<ChannelToNotifyDto> channelsToNotify = Collections.emptyList();
        try {
            String[] channelsIds = (String[]) formParams.get("channelsToNotify");
            channelsToNotify = channelsIds == null ? Collections.emptyList() :
                    Stream.of(channelsIds)
                            .map(item -> {
                                String[] parts = item.split("\\|");
                                return new ChannelToNotifyDto(parts[0], parts[1]);
                            })
                            .collect(Collectors.toList());
            channelsToNotifyJson = ChannelToNotifyDto.toJson(channelsToNotify);
        } catch (IOException e) {
            log.error("Error parsing channel data", e);
            channelsToNotifyJson = "";
        }
        String jql = extractSingleParam(formParams, "jql");
        String message = extractSingleParam(formParams, "message");
        String owner = extractSingleParam(formParams, "owner");
        boolean hasOwnerAValidToken = channelsToNotify.stream().allMatch(c -> slackClientProvider
                .withTeamId(c.getTeamId())
                .fold(
                        e -> false,
                        client -> client.withUserTokenIfAvailable(owner)
                                .flatMap(clientWithUser -> clientWithUser.getConversationsInfo(c.getChannelId()).toOptional())
                                .isPresent()
                ));

        return ImmutableMap.of(CHANNELS_TO_NOTIFY_JSON_PARAM,
                channelsToNotifyJson,
                JQL_FILTER_PARAM,
                jql,
                MESSAGE_FILTER_PARAM,
                message,
                OWNER_PARAM,
                hasOwnerAValidToken ? owner : authenticationContext.getLoggedInUser().getKey());
    }

    private boolean isValidQuery(String jql) {
        if (Strings.isNullOrEmpty(jql)) {
            return true;
        }
        // User is not used when parsing the query
        SearchService.ParseResult parseResult = searchService.parseQuery(null, jql);

        return parseResult.isValid();
    }

    public static class ViewDto {
        private final String jql;
        private final List<SlackChannelDTO> channelsToNotify;
        private final boolean responseError;
        private final boolean jqlValid;

        ViewDto(List<SlackChannelDTO> channelsToNotify, String jql, boolean jqlValid) {
            this.channelsToNotify = channelsToNotify;
            this.jql = jql;
            this.responseError = false;
            this.jqlValid = jqlValid;
        }

        ViewDto(boolean responseError) {
            this.channelsToNotify = Collections.emptyList();
            this.jql = null;
            this.responseError = responseError;
            this.jqlValid = false;
        }

        public Collection<SlackChannelDTO> getChannelsToNotify() {
            return channelsToNotify;
        }

        public String getJql() {
            return jql;
        }

        public boolean isResponseError() {
            return responseError;
        }

        public boolean isJqlValid() {
            return jqlValid;
        }
    }

    public static class EditDto {
        private final List<SlackLink> links;
        private final List<SlackChannelDTO> channelsToNotify;
        private final String jql;
        private final boolean responseError;
        private final boolean jqlValid;
        private final String message;
        private final String owner;

        EditDto(List<SlackLink> links,
                List<SlackChannelDTO> channelsToNotify,
                String jql,
                boolean jqlValid,
                String message,
                String owner) {
            this.links = links;
            this.channelsToNotify = channelsToNotify;
            this.jql = jql;
            this.owner = owner;
            this.responseError = false;
            this.jqlValid = jqlValid;
            this.message = message;
        }

        EditDto(final List<SlackLink> links) {
            this(links, Collections.emptyList(), null, true, null, null);
        }

        public Collection<SlackChannelDTO> getChannelsToNotify() {
            return channelsToNotify;
        }

        public String getJql() {
            return jql;
        }

        public boolean isResponseError() {
            return responseError;
        }

        public boolean isJqlValid() {
            return jqlValid;
        }

        public String getMessage() {
            return message;
        }

        public String getOwner() {
            return owner;
        }

        public List<SlackLink> getLinks() {
            return links;
        }
    }
}
