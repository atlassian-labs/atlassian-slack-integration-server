package com.atlassian.plugins.slack.api.client;

import com.atlassian.plugins.slack.api.ConversationKey;
import com.atlassian.plugins.slack.api.SlackLink;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.github.seratch.jslack.api.model.Conversation;
import io.atlassian.fugue.Either;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
@Slf4j
public class ConversationLoaderHelper implements InitializingBean, DisposableBean {
    private final SlackClientProvider slackClientProvider;
    private final SlackLinkManager slackLinkManager;
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;
    private final ExecutorServiceHelper executorServiceHelper;
    private final int maxChannelsToLoad;

    private ExecutorService executorService;

    @Autowired
    public ConversationLoaderHelper(final SlackClientProvider slackClientProvider,
                                    final SlackLinkManager slackLinkManager,
                                    final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory,
                                    final ExecutorServiceHelper executorServiceHelper) {
        this.slackClientProvider = slackClientProvider;
        this.slackLinkManager = slackLinkManager;
        this.threadLocalDelegateExecutorFactory = threadLocalDelegateExecutorFactory;
        this.executorServiceHelper = executorServiceHelper;
        this.maxChannelsToLoad = Integer.getInteger("slack.client.max.channel.bulk.load", 50);
    }

    /**
     * Loads multiple conversations from any number of teams, in parallel, using a custom loader.
     * This function tries to map a list of source items (of any type you need) and retrieve all the conversations
     * associated with them, deduplicated, thus preventing unnecessary requests for the same channel.
     *
     * @param sourceItems Items that require conversation information
     * @param keyBuilder  Obtains team and channel ids from a source item
     * @param loader      Function that takes a client and a source item and tries to fetch a conversation
     * @param <T>         Type of the source item
     */
    public <T> ConversationsAndLinks conversationsAndLinksById(
            final Collection<T> sourceItems,
            final Function<T, ConversationKey> keyBuilder,
            final BiFunction<SlackClient, ConversationKey, Optional<Conversation>> loader) {

        // convert source items into channel keys
        final Set<ConversationKey> channels = sourceItems.stream()
                .map(keyBuilder::apply)
                .collect(toSet());

        // loads all links used here
        final Map<String, SlackLink> linksByTeamId = channels.stream()
                .map(ConversationKey::getTeamId)
                .distinct()
                .map(slackLinkManager::getLinkByTeamId)
                .flatMap(Either::toStream)
                .collect(Collectors.toMap(SlackLink::getTeamId, Function.identity()));

        // index links by channel
        final Map<String, SlackLink> linksByChannelId = channels.stream()
                .filter(key -> linksByTeamId.containsKey(key.getTeamId()))
                .collect(toMap(
                        ConversationKey::getChannelId,
                        key -> linksByTeamId.get(key.getTeamId()),
                        (k1, k2) -> k1));

        // warn about too many channels
        if (channels.size() > maxChannelsToLoad) {
            log.info("Number of Slack channels to load exceeds the maximum limit. Loading only {} from {}.",
                    maxChannelsToLoad, channels.size());
        }

        // starts loading conversations
        final List<Future<Optional<Pair<ConversationKey, Conversation>>>> futures = channels.stream()
                .limit(maxChannelsToLoad)
                .map(channel -> loadConversationFromList(channel, linksByChannelId, loader))
                .collect(toList());

        // collects results
        final Map<ConversationKey, Conversation> conversationsById = futures.stream()
                .map((Future<Optional<Pair<ConversationKey, Conversation>>> future) -> {
                    try {
                        Optional<Pair<ConversationKey, Conversation>> result = future.get();
                        return result;
                    } catch (Exception e) {
                        return Optional.<Pair<ConversationKey, Conversation>>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toMap(Pair<ConversationKey, Conversation>::getLeft, Pair<ConversationKey, Conversation>::getRight));

        return new ConversationsAndLinks(conversationsById, linksByTeamId, linksByChannelId);
    }

    /**
     * Load conversation using the provided values and loader but only for the first provided item.
     */
    private Future<Optional<Pair<ConversationKey, Conversation>>> loadConversationFromList(
            final ConversationKey conversationKey,
            final Map<String, SlackLink> linksByChannelId,
            final BiFunction<SlackClient, ConversationKey, Optional<Conversation>> loader) {
        return executorService.submit(() -> Optional
                .ofNullable(linksByChannelId.get(conversationKey.getChannelId()))
                .map(slackClientProvider::withLink)
                .flatMap(client -> loader.apply(client, conversationKey))
                .map(conv -> new ImmutablePair<>(conversationKey, conv)));
    }

    @Override
    public void afterPropertiesSet() {
        executorService = threadLocalDelegateExecutorFactory
                .createExecutorService(executorServiceHelper.createBoundedExecutorService());
    }

    @Override
    public void destroy() {
        if (executorService != null) {
            try {
                executorService.shutdown();
            } catch (Exception e) {
                // nothing
            }
        }
    }
}
