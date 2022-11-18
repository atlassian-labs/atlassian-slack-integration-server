package com.atlassian.confluence.plugins.slack.spacetochannel.listener;

import com.atlassian.crowd.event.user.UserDeletedEvent;
import com.atlassian.crowd.event.user.UserEditedEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.webhooks.TokensRevokedSlackEvent;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.plugins.slack.util.AutoSubscribingEventListener;
import com.atlassian.sal.api.user.UserKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

@Service
public class TokensRevokedEventListener extends AutoSubscribingEventListener {
    private static final Logger logger = LoggerFactory.getLogger(TokensRevokedEventListener.class);

    private final SlackLinkManager slackLinkManager;
    private final SlackUserManager slackUserManager;

    @Autowired
    public TokensRevokedEventListener(final EventPublisher eventPublisher,
                                      final SlackLinkManager slackLinkManager,
                                      final SlackUserManager slackUserManager) {
        super(eventPublisher);
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
