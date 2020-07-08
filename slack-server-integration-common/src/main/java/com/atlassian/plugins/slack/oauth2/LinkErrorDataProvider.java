package com.atlassian.plugins.slack.oauth2;

import com.atlassian.json.marshal.Jsonable;
import com.atlassian.plugins.slack.api.json.JsonableJacksonService;
import com.atlassian.sal.api.web.context.HttpContext;
import com.atlassian.webresource.api.data.WebResourceDataProvider;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public class LinkErrorDataProvider implements WebResourceDataProvider {
    public static final String SLACK_OAUTH_ERROR_SESSION = "slack.oauth2.error";
    private final HttpContext httpContext;
    private final JsonableJacksonService jsonableJacksonService;

    @Autowired
    public LinkErrorDataProvider(final HttpContext httpContext,
                                 final JsonableJacksonService jsonableJacksonService) {
        this.httpContext = httpContext;
        this.jsonableJacksonService = jsonableJacksonService;
    }

    @Override
    public Jsonable get() {
        ImmutableMap.Builder<String, Object> defaultPanelContext = ImmutableMap.builder();

        Optional.ofNullable(httpContext.getRequest())
                .map(HttpServletRequest::getSession)
                .flatMap(session -> {
                    try {
                        return Optional.ofNullable(session.getAttribute(SLACK_OAUTH_ERROR_SESSION));
                    } finally {
                        session.removeAttribute(SLACK_OAUTH_ERROR_SESSION);
                    }
                })
                .ifPresent(errorString -> defaultPanelContext.put("errorString", errorString));

        return jsonableJacksonService.toJsonable(defaultPanelContext.build());
    }
}
