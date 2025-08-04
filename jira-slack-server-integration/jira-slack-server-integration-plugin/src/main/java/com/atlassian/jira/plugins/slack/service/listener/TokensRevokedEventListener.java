package com.atlassian.jira.plugins.slack.service.listener;

import com.atlassian.crowd.event.user.UserDeletedEvent;
import com.atlassian.crowd.event.user.UserEditedEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.user.flag.FlagDismissalService;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugins.slack.api.webhooks.TokensRevokedSlackEvent;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.atlassian.sal.api.user.UserKey;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class TokensRevokedEventListener extends AutoSubscribingEventListener {
    private static final Logger logger = LoggerFactory.getLogger(TokensRevokedEventListener.class);
    public static final String DISCONNECTED_FLAG = "slack.token.disconnected.flag";

    private final FlagDismissalService flagDismissalService;
    private final UserManager jiraUserManager;
    private final SlackLinkManager slackLinkManager;
    private final SlackUserManager slackUserManager;

    @Autowired
    public TokensRevokedEventListener(final EventPublisher eventPublisher,
                                      final FlagDismissalService flagDismissalService,
                                      @Qualifier("jiraUserManager") final UserManager jiraUserManager,
                                      final SlackLinkManager slackLinkManager,
                                      final SlackUserManager slackUserManager) {
        super(eventPublisher);
        this.flagDismissalService = flagDismissalService;
        this.jiraUserManager = jiraUserManager;
        this.slackLinkManager = slackLinkManager;
        this.slackUserManager = slackUserManager;
    }

    @EventListener
    public void tokensWereRevoked(@Nonnull final TokensRevokedSlackEvent event) {
        List<String> userIds = event.getUserIds();
        List<String> botIds = event.getBotIds();
        logger.debug("Following tokens were revoked. Users: {}, bots: {}", userIds, botIds);

        for (String userId : userIds) {
            slackUserManager.revokeToken(userId);

            // show warning to user
            slackUserManager.getBySlackUserId(userId)
                    .flatMap(slackUser -> Optional.ofNullable(jiraUserManager.getUserByKey(slackUser.getUserKey())))
                    .ifPresent(user -> flagDismissalService.removeDismissFlagForUser(DISCONNECTED_FLAG, user));
        }

        if (!botIds.isEmpty()) {
            String teamId = event.getSlackEvent().getTeamId();
            slackLinkManager.revokeToken(teamId);
        }
    }

    @EventListener
    public void userWasRemoved(@Nonnull final UserDeletedEvent event) {
        logger.debug("Following user was removed: {}", event);

        slackUserManager.getByUserKey(new UserKey(event.getUsername())).forEach(slackUserManager::delete);
    }

    @EventListener
    public void userWasEdited(@Nonnull final UserEditedEvent event) {
        boolean hasNameChanged = !Objects.equals(event.getUser().getName(), event.getOriginalUser().getName());
        boolean hasBecomeInactive = event.getOriginalUser().isActive() && !event.getUser().isActive();
        if (hasNameChanged || hasBecomeInactive) {
            logger.debug("Following user was updated and had either name changed or became inactive: {}", event);
            slackUserManager.getByUserKey(new UserKey(event.getOriginalUser().getName())).forEach(slackUserManager::delete);
        }
    }
}
