package com.atlassian.confluence.plugins.slack.spacetochannel.soy;

import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SlackChannelDefinition;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.soy.renderer.JsExpression;
import com.atlassian.soy.renderer.SoyClientFunction;
import com.atlassian.soy.renderer.SoyServerFunction;
import com.google.common.collect.ImmutableSet;

import java.util.Optional;
import java.util.Set;

public class IsSlackChannelNotificationEnabledFunction implements SoyServerFunction<Boolean>, SoyClientFunction {
    private static final int DEFAULT_BY_ACTIVE_ARG_INDEX = 3;
    private static final Set<Integer> VALID_ARG_SIZES = ImmutableSet.of(3, 4);

    private final NotificationTypeService notificationTypeService;

    public IsSlackChannelNotificationEnabledFunction(final NotificationTypeService notificationTypeService) {
        this.notificationTypeService = notificationTypeService;
    }

    @Override
    public String getName() {
        return "isSlackChannelNotificationEnabled";
    }

    // This implementation should be kept in sync with the implementation in /static/feature/repo-channel-table/is-notification-enabled.js
    @Override
    public Boolean apply(final Object... args) {
        final SpaceToChannelConfiguration spaceToChannelConfiguration = (SpaceToChannelConfiguration) args[0];
        final SlackChannelDefinition channel = (SlackChannelDefinition) args[1];
        final String notificationName = (String) args[2];
        final Optional<NotificationType> spaceToChannelNotificationOption =
                notificationTypeService.getNotificationTypeForKey(notificationName);
        return spaceToChannelNotificationOption.isPresent()
                && spaceToChannelConfiguration.isChannelNotificationEnabled(channel, spaceToChannelNotificationOption.get());
    }

    /**
     * Only parameter we care about for Javascript rendering is the 4th parameter - enabled, since
     * rendering on Javascript is only done when adding a new mapping, so there's no existing configuration
     * to check.
     * <p>
     * Enabled is set when activeByDefault is true for a notification type.
     */
    @Override
    public JsExpression generate(final JsExpression... jsExpressions) {
        JsExpression isEnabled = new JsExpression("false");
        if (jsExpressions.length > DEFAULT_BY_ACTIVE_ARG_INDEX) {
            isEnabled = jsExpressions[DEFAULT_BY_ACTIVE_ARG_INDEX];
        }
        return isEnabled;
    }

    @Override
    public Set<Integer> validArgSizes() {
        return VALID_ARG_SIZES;
    }
}
