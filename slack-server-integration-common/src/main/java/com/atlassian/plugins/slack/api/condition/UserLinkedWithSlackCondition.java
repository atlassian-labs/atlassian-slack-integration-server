package com.atlassian.plugins.slack.api.condition;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugins.slack.user.SlackUserManager;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Map;

public class UserLinkedWithSlackCondition implements Condition {
    private final SlackUserManager slackUserManager;
    private final UserManager userManager;

    public UserLinkedWithSlackCondition(final SlackUserManager slackUserManager,
                                        @Qualifier("salUserManager") final UserManager userManager) {
        this.slackUserManager = slackUserManager;
        this.userManager = userManager;
    }

    @Override
    public void init(final Map<String, String> params) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(final Map<String, Object> context) {
        final UserKey userKey = userManager.getRemoteUserKey();
        return userKey != null && !slackUserManager.getByUserKey(userKey).isEmpty();
    }
}
