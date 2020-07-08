package com.atlassian.bitbucket.plugins.slack.notification.configuration.soy;

import com.atlassian.bitbucket.plugins.slack.notification.configuration.ChannelConfiguration;
import com.atlassian.soy.renderer.JsExpression;
import com.atlassian.soy.renderer.SoyClientFunction;
import com.atlassian.soy.renderer.SoyServerFunction;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class IsSlackNotificationEnabledFunction implements SoyServerFunction<Boolean>, SoyClientFunction {
    @Override
    public Boolean apply(Object... args) {
        if (!(args[0] instanceof String)) {
            throw new IllegalArgumentException("First arg was not of class String");
        }
        if (!(args[1] instanceof ChannelConfiguration)) {
            throw new IllegalArgumentException("Second arg was not of class ChannelConfiguration");
        }

        String testNotificationKey = (String) args[0];
        ChannelConfiguration channelConfig = (ChannelConfiguration) args[1];

        return channelConfig.isEnabled(testNotificationKey);
    }

    @Override
    public JsExpression generate(JsExpression... args) {
        return new JsExpression("require('bitbucket-plugin-slack/feature/repo-channel-table/is-notification-enabled')(" + args[0].getText() + ", " + args[1].getText() + ")");
    }

    @Override
    public String getName() {
        return "isSlackNotificationEnabled";
    }

    @Override
    public Set<Integer> validArgSizes() {
        return ImmutableSet.of(2);
    }
}
