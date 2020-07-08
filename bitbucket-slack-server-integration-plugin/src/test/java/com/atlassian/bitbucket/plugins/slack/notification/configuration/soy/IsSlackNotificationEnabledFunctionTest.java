package com.atlassian.bitbucket.plugins.slack.notification.configuration.soy;

import com.atlassian.bitbucket.plugins.slack.notification.configuration.ChannelConfiguration;
import com.atlassian.soy.renderer.JsExpression;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IsSlackNotificationEnabledFunctionTest {
    @Mock
    private ChannelConfiguration channelConfiguration;

    @InjectMocks
    private IsSlackNotificationEnabledFunction target;

    @Test
    public void apply_shouldReturnExpectedValue() {
        when(channelConfiguration.isEnabled("T")).thenReturn(true, false);

        assertThat(target.apply("T", channelConfiguration), is(true));
        assertThat(target.apply("T", channelConfiguration), is(false));
    }

    @Test
    public void generate_shouldReturn4thParam() {
        JsExpression result = target.generate(new JsExpression("A"), new JsExpression("B"));
        assertThat(result.getText(),
                is("require('bitbucket-plugin-slack/feature/repo-channel-table/is-notification-enabled')(A, B)"));
    }

    @Test
    public void getters() {
        assertThat(target.getName(), is("isSlackNotificationEnabled"));
        assertThat(target.validArgSizes(), containsInAnyOrder(2));
    }
}
