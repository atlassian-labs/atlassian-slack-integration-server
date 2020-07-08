package com.atlassian.plugins.slack.api.client;

import com.atlassian.plugins.slack.util.ErrorResponse;
import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.MethodsClient;
import com.github.seratch.jslack.api.methods.request.api.ApiTestRequest;
import com.github.seratch.jslack.api.methods.response.api.ApiTestResponse;
import io.atlassian.fugue.Either;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class DefaultSlackLimitedClientTest {
    @Mock
    private Slack slack;
    @Mock
    private MethodsClient methods;
    @Mock
    private ApiTestResponse apiResponse;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultSlackLimitedClient client;

    @Before
    public void setUp() throws Exception {
        when(slack.methods()).thenReturn(methods);
        when(methods.apiTest(any(ApiTestRequest.class))).thenReturn(apiResponse);
        when(apiResponse.isOk()).thenReturn(true);
        client = new DefaultSlackLimitedClient(slack);
    }

    @Test
    public void testApi_shouldCallsSlack_whenInvoked() {
        Either<ErrorResponse, Boolean> response = client.testApi();

        assertThat(response.isRight(), is(true));
    }
}
