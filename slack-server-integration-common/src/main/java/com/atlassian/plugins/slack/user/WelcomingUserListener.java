package com.atlassian.plugins.slack.user;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.client.SlackClientProvider;
import com.atlassian.plugins.slack.api.webhooks.AppHomeOpenedSlackEvent;
import com.atlassian.plugins.slack.settings.SlackSettingService;
import com.atlassian.plugins.slack.util.AsyncExecutor;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.atlassian.plugins.slack.util.SlackHelper;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.message.I18nResolver;
import com.github.seratch.jslack.api.methods.request.chat.ChatPostMessageRequest;
import jakarta.ws.rs.core.UriBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class WelcomingUserListener extends AutoSubscribingEventListener {
    private final SlackClientProvider slackClientProvider;
    private final SlackSettingService slackSettingService;
    private final AsyncExecutor asyncExecutor;
    private final I18nResolver i18nResolver;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public WelcomingUserListener(final EventPublisher eventPublisher,
                                 final SlackClientProvider slackClientProvider,
                                 final SlackSettingService slackSettingService,
                                 final AsyncExecutor asyncExecutor,
                                 final I18nResolver i18nResolver,
                                 @Qualifier("salApplicationProperties") final ApplicationProperties applicationProperties) {
        super(eventPublisher);
        this.slackClientProvider = slackClientProvider;
        this.asyncExecutor = asyncExecutor;
        this.slackSettingService = slackSettingService;
        this.i18nResolver = i18nResolver;
        this.applicationProperties = applicationProperties;
    }

    @EventListener
    public void onAppHomeOpened(final AppHomeOpenedSlackEvent event) {
        asyncExecutor.run(() -> {
            if (!slackSettingService.isAppWelcomeMessageSent(event.getUser())) {
                final String baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE);
                final String welcomeMessage = i18nResolver.getText(
                        "plugins.slack.user.welcome.message",
                        baseUrl,
                        SlackHelper.escapeSignsForSlackLink(applicationProperties.getDisplayName()),
                        UriBuilder.fromPath(baseUrl).path("/plugins/servlet/slack/view-oauth-sessions").build(),
                        UriBuilder.fromPath(baseUrl).path("/plugins/servlet/slack/configure").build(),
                        i18nResolver.getText("plugins.slack.documentation.home"));

                final boolean success = slackClientProvider.withLink(event.getSlackEvent().getSlackLink())
                        .postMessage(ChatPostMessageRequest.builder()
                                .channel(event.getChannel())
                                .text(welcomeMessage)
                                .mrkdwn(true)
                                .build())
                        .isRight();

                if (success) {
                    slackSettingService.markAppWelcomeMessageSent(event.getUser());
                }
            }
        });
    }
}
