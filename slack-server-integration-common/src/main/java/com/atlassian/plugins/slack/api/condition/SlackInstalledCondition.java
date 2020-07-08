package com.atlassian.plugins.slack.api.condition;

import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;
import com.atlassian.plugins.slack.link.SlackLinkManager;

import java.util.Map;


public class SlackInstalledCondition implements Condition {
    private final SlackLinkManager slackLinkManager;

    public SlackInstalledCondition(final SlackLinkManager slackLinkManager) {
        this.slackLinkManager = slackLinkManager;
    }

    @Override
    public void init(final Map<String, String> params) throws PluginParseException {

    }

    @Override
    public boolean shouldDisplay(final Map<String, Object> context) {
        return slackLinkManager.isAnyLinkDefined();
    }
}
