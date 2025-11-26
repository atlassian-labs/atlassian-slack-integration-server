package com.atlassian.plugins.slack.admin;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.plugins.slack.analytics.AnalyticsContextProvider;
import com.atlassian.plugins.slack.api.SlackLinkDto;
import com.atlassian.plugins.slack.link.SlackLinkManager;
import com.atlassian.plugins.slack.spi.ConfigurationRedirectionManager;
import com.atlassian.plugins.slack.spi.SlackPluginResourceProvider;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.websudo.WebSudoManager;
import com.atlassian.sal.api.websudo.WebSudoSessionException;
import com.atlassian.templaterenderer.TemplateRenderer;
import io.atlassian.fugue.Either;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.Theory;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.PrintWriter;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigureServletTest {
    public static final String BASE_URL = "someBaseUrl";
    public static final String CONNECT_TEMPLATE = "admin/connect/connect-workspace.vm";
    public static final String CONFIGURE_TEMPLATE = "admin/configure-slack.vm";
    @Mock
    private TemplateRenderer templateRenderer;
    @Mock
    private ConfigurationRedirectionManager configurationRedirectionManager;
    @Mock
    private ApplicationProperties applicationProperties;
    @Mock
    private WebInterfaceManager webInterfaceManager;
    @Mock
    private SlackLinkManager slackLinkManager;
    @Mock
    private SlackPluginResourceProvider slackPluginResourceProvider;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private AnalyticsContextProvider analyticsContextProvider;
    @Mock
    private WebSudoManager webSudoManager;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private PrintWriter responseWriter;
    @Captor
    private ArgumentCaptor<String> templateNameCaptor;
    @Captor
    private ArgumentCaptor<Map<String, Object>> contextCaptor;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private ConfigureServlet servlet;

    @Before
    public void setUp() throws Exception {
        when(configurationRedirectionManager.getRedirectUri(request)).thenReturn(Optional.empty());
        when(applicationProperties.getBaseUrl(UrlMode.ABSOLUTE)).thenReturn(BASE_URL);
        when(request.getSession()).thenReturn(session);
        when(response.getWriter()).thenReturn(responseWriter);
        when(session.getAttributeNames()).thenReturn(emptyEnumeration());
    }

    @Test
    public void doGet_shouldRenderConnectPage_whenAddActionIsPassed() throws Exception {
        when(request.getParameter("action")).thenReturn("add");
        InOrder inOrder = Mockito.inOrder(webSudoManager, templateRenderer);

        servlet.doGet(request, response);

        inOrder.verify(webSudoManager).willExecuteWebSudoRequest(request);
        inOrder.verify(templateRenderer).render(templateNameCaptor.capture(), contextCaptor.capture(), eq(responseWriter));
        inOrder.verifyNoMoreInteractions();
        assertThat(templateNameCaptor.getValue(), is(CONNECT_TEMPLATE));
        Map<String, Object> context = contextCaptor.getValue();
        assertThat(context, hasEntry("baseUrl", BASE_URL));
        assertThat(context, hasEntry("slackPluginResourceProvider", slackPluginResourceProvider));
    }

    @Test
    public void doGet_shouldRenderConnectPage_whenEditActionIsPassed() throws Exception {
        String teamId = "someTeamId";
        SlackLinkDto link = new SlackLinkDto();
        when(request.getParameter("action")).thenReturn("edit");
        when(request.getParameter("teamId")).thenReturn(teamId);
        when(slackLinkManager.getLinkByTeamId(teamId)).thenReturn(Either.right(link));

        servlet.doGet(request, response);

        verify(templateRenderer).render(templateNameCaptor.capture(), contextCaptor.capture(), eq(responseWriter));
        assertThat(templateNameCaptor.getValue(), is(CONNECT_TEMPLATE));
        Map<String, Object> context = contextCaptor.getValue();
        assertThat(context, hasEntry("link", link));
        assertThat(context, hasEntry("baseUrl", BASE_URL));
        assertThat(context, hasEntry("slackPluginResourceProvider", slackPluginResourceProvider));
    }

    @Test
    public void doGet_shouldRenderConfigurationPage_whenUnknownActionAndRecentInstallIdArePassed() throws Exception {
        String teamId = "someTeamId";
        String recentInstallId = teamId;
        SlackLinkDto link = new SlackLinkDto();
        when(request.getParameter("action")).thenReturn("unknownAction");
        when(request.getParameter("recentInstall")).thenReturn(recentInstallId);
        when(slackLinkManager.getLinkByTeamId(teamId)).thenReturn(Either.right(link));

        servlet.doGet(request, response);

        verify(templateRenderer).render(templateNameCaptor.capture(), contextCaptor.capture(), eq(responseWriter));
        assertThat(templateNameCaptor.getValue(), is(CONFIGURE_TEMPLATE));
        Map<String, Object> context = contextCaptor.getValue();
        assertThat(context, hasEntry("link", link));
        assertThat(context, hasEntry("recentInstall", recentInstallId));
        assertThat(context, hasEntry("slackPluginResourceProvider", slackPluginResourceProvider));
    }

    @Test
    public void doGet_shouldRenderConfigurationPage_whenUnknownActionAndNoRecentInstallIdIdPassed() throws Exception {
        SlackLinkDto link = new SlackLinkDto();
        when(request.getParameter("action")).thenReturn("unknownAction");
        when(slackLinkManager.getLinks()).thenReturn(singletonList(link));

        servlet.doGet(request, response);

        verify(templateRenderer).render(templateNameCaptor.capture(), contextCaptor.capture(), eq(responseWriter));
        assertThat(templateNameCaptor.getValue(), is(CONFIGURE_TEMPLATE));
        Map<String, Object> context = contextCaptor.getValue();
        assertThat(context, hasEntry("link", link));
        assertThat(context, hasEntry("recentInstall", ""));
        assertThat(context, hasEntry("slackPluginResourceProvider", slackPluginResourceProvider));
    }

    @Test
    public void doGet_shouldRedirectToConfig_whenRedirectUriIsProvided() throws Exception {
        String recentInstallId = "someRecentInstallId";
        String redirectUri = "someUri";
        when(request.getParameter("recentInstall")).thenReturn(recentInstallId);
        when(configurationRedirectionManager.getRedirectUri(request)).thenReturn(Optional.of(URI.create(redirectUri)));

        servlet.doGet(request, response);

        verify(response).sendRedirect(redirectUri);
        verify(session).setAttribute(eq("context"), contextCaptor.capture());
        assertThat(contextCaptor.getValue(), hasEntry("recentInstall", recentInstallId));
    }

    @Test
    public void doGet_redirectsToWebSudoIfCurrentlyNotInWebSudoMode() throws Exception {
        when(request.getParameter("action")).thenReturn("add");
        doThrow(new WebSudoSessionException("blah")).when(webSudoManager).willExecuteWebSudoRequest(request);
        InOrder inOrder = Mockito.inOrder(webSudoManager, templateRenderer, request, response);

        servlet.doGet(request, response);

        inOrder.verify(webSudoManager).willExecuteWebSudoRequest(request);
        inOrder.verify(webSudoManager).enforceWebSudoProtection(request, response);
        inOrder.verifyNoMoreInteractions();
    }
}
