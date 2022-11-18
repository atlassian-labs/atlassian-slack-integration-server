package com.atlassian.confluence.plugins.slack.spacetochannel.soy;

import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SlackChannelDefinition;
import com.atlassian.confluence.plugins.slack.spacetochannel.configuration.SpaceToChannelConfiguration;
import com.atlassian.plugins.slack.api.descriptor.NotificationTypeService;
import com.atlassian.plugins.slack.api.notification.NotificationType;
import com.atlassian.soy.renderer.JsExpression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IsSlackChannelNotificationEnabledFunctionTest {
    private static final String KEY = "key";
    private static final JsExpression EMPTY_EXPRESSION = new JsExpression("");

    @Mock
    private NotificationTypeService notificationTypeService;
    @Mock
    private SlackChannelDefinition slackChannelDefinition;
    @Mock
    private SpaceToChannelConfiguration spaceToChannelConfiguration;
    @Mock
    private NotificationType notificationType;

    @InjectMocks
    private IsSlackChannelNotificationEnabledFunction target;

    @Test
    public void apply_shouldReturnTrueIfNotifEnabled() {
        when(notificationTypeService.getNotificationTypeForKey(KEY))
                .thenReturn(Optional.of(notificationType));
        when(spaceToChannelConfiguration.isChannelNotificationEnabled(slackChannelDefinition, notificationType))
                .thenReturn(true);

        Boolean result = target.apply(spaceToChannelConfiguration, slackChannelDefinition, KEY);

        assertThat(result, is(true));
    }

    @Test
    public void apply_shouldReturnFalseIfNotifDisabled() {
        when(notificationTypeService.getNotificationTypeForKey(KEY))
                .thenReturn(Optional.of(notificationType));
        when(spaceToChannelConfiguration.isChannelNotificationEnabled(slackChannelDefinition, notificationType))
                .thenReturn(false);

        Boolean result = target.apply(spaceToChannelConfiguration, slackChannelDefinition, KEY);

        assertThat(result, is(false));
    }

    @Test
    public void apply_shouldReturnFalseIfNotifDoesNotExist() {
        when(notificationTypeService.getNotificationTypeForKey(KEY))
                .thenReturn(Optional.empty());

        Boolean result = target.apply(spaceToChannelConfiguration, slackChannelDefinition, KEY);

        assertThat(result, is(false));
    }

    @Test
    public void generate_shouldReturnFalseIfNot4thParam() {
        JsExpression result = target.generate(EMPTY_EXPRESSION, EMPTY_EXPRESSION, EMPTY_EXPRESSION);
        assertThat(result.getText(), is("false"));
    }

    @Test
    public void generate_shouldReturn4thParam() {
        JsExpression result = target.generate(EMPTY_EXPRESSION, EMPTY_EXPRESSION, EMPTY_EXPRESSION, new JsExpression("y"));
        assertThat(result.getText(), is("y"));
    }

    @Test
    public void getters() {
        assertThat(target.getName(), is("isSlackChannelNotificationEnabled"));
        assertThat(target.validArgSizes(), containsInAnyOrder(3, 4));
    }
}
