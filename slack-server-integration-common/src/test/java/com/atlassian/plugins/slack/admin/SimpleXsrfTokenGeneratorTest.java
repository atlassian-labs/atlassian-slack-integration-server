package com.atlassian.plugins.slack.admin;

import com.atlassian.security.random.DefaultSecureTokenGenerator;
import com.atlassian.security.random.SecureTokenGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest(DefaultSecureTokenGenerator.class)
@RunWith(PowerMockRunner.class)
public class SimpleXsrfTokenGeneratorTest {
    private static final String TOKEN = "someToken";
    private static final String TEAM = "someTeam";

    @Mock
    private SecureTokenGenerator tokenGenerator;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private SimpleXsrfTokenGenerator generator;

    @Before
    public void setUp() {
        when(request.getSession(anyBoolean())).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(TOKEN)).thenReturn(TEAM);
        mockStatic(DefaultSecureTokenGenerator.class);
        generator = new SimpleXsrfTokenGenerator();
    }

    @Test
    public void validateToken_shouldExtractTokenFromSession() {
        XsrfTokenGenerator.ValidationResult validationResult = generator.validateToken(request, TOKEN, TEAM);

        assertThat(validationResult, is(XsrfTokenGenerator.ValidationResult.VALID));
    }

    @Test
    public void getNewToken_shouldPutNewTokenToSession() {
        when(DefaultSecureTokenGenerator.getInstance()).thenReturn(tokenGenerator);
        when(tokenGenerator.generateToken()).thenReturn(TOKEN);

        String newToken = generator.getNewToken(request, TEAM);

        assertThat(newToken, is(TOKEN));
        verify(session).setAttribute(TOKEN, TEAM);
    }
}
