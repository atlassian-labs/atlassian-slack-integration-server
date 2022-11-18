package com.atlassian.confluence.plugins.slack.spacetochannel.actions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;

import static com.atlassian.confluence.plugins.slack.spacetochannel.actions.SlackViewSpaceInstallationAction.CONTEXT_ATTRIBUTE_LABEL;
import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.FROM_SPACE_ATTRIBUTE_KEY;
import static com.atlassian.confluence.plugins.slack.spi.impl.ConfluenceConfigurationRedirectionManager.SPACE_ATTRIBUTE_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SlackViewSpaceInstallationActionTest {
    private static final String SPACE_KEY = "SPACE";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;

    @InjectMocks
    private SlackViewSpaceInstallationAction servlet;

    @Test
    public void execute_shouldRemoveContextAddAttrsAndReturnSuccess() {
        servlet.setServletRequestSupplier(() -> request);
        servlet.setKey(SPACE_KEY);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(CONTEXT_ATTRIBUTE_LABEL)).thenReturn(Collections.emptyMap());

        String result = servlet.execute();

        assertThat(result, is("success"));
        verify(session).removeAttribute(CONTEXT_ATTRIBUTE_LABEL);
        verify(session).setAttribute(FROM_SPACE_ATTRIBUTE_KEY, true);
        verify(session).setAttribute(SPACE_ATTRIBUTE_KEY, SPACE_KEY);
    }
}
