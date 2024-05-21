package com.atlassian.plugins.slack.api.condition;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.webresource.spi.condition.AbstractBooleanUrlReadingCondition;

import java.util.Map;

public class IsSlackEnabledUrlReadingCondition extends AbstractBooleanUrlReadingCondition implements Condition {
    private static final String SLACK_ENABLED_PARAM = "slack-enabled";
    private final SlackLinkManager slackLinkManager;

    public IsSlackEnabledUrlReadingCondition(final SlackLinkManager slackLinkManager) {
        this.slackLinkManager = slackLinkManager;
    }

    @Override
    public void init(final Map<String, String> stringStringMap) throws PluginParseException {
    }

    @Override
    public boolean shouldDisplay(final Map<String, Object> context) {
        return isConditionTrue();
    }

    @Override
    protected boolean isConditionTrue() {
        return slackLinkManager.isAnyLinkDefined();
    }

    @Override
    protected String queryKey() {
        return SLACK_ENABLED_PARAM;
    }
}
