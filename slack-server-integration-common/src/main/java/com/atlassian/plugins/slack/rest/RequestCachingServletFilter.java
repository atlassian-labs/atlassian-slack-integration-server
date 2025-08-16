package com.atlassian.plugins.slack.rest;

import com.atlassian.annotations.security.UnrestrictedAccess;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import static com.github.seratch.jslack.app_backend.SlackSignature.HeaderNames.X_SLACK_SIGNATURE;

@UnrestrictedAccess
public class RequestCachingServletFilter implements Filter {
    private final RequestHolder requestHolder;

    public RequestCachingServletFilter(final RequestHolder requestHolder) {
        this.requestHolder = requestHolder;
    }

    @Override
    public void doFilter(final ServletRequest request,
                         final ServletResponse response,
                         final FilterChain chain) throws IOException, ServletException {
        ServletRequest processedRequest = request;

        // cache request body if Slack signature is present
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String signature = httpRequest.getHeader(X_SLACK_SIGNATURE);
        if (StringUtils.isNotBlank(signature)) {
            processedRequest = new CachingServletRequestWrapper(httpRequest);
            requestHolder.put(signature, (CachingServletRequestWrapper) processedRequest);
        }
        chain.doFilter(processedRequest, response);
    }

}
