package com.atlassian.plugins.slack.analytics;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugins.slack.api.events.SlackLinkClickedAnalyticEvent;
import com.atlassian.plugins.slack.util.LinkHelper;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class SlackAnalyticsFilter implements Filter {
    private final EventPublisher eventPublisher;
    private final AnalyticsContextProvider analyticsContextProvider;
    private final TransactionTemplate transactionTemplate;
    private boolean useTransaction;

    @Override
    public void init(FilterConfig filterConfig) {
        useTransaction = Boolean.valueOf(filterConfig.getInitParameter("useTransaction"));
    }

    @Override
    public void doFilter(final ServletRequest request,
                         final ServletResponse response,
                         final FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            LinkHelper.decodeOriginType(request.getParameter(LinkHelper.ATL_LINK_ORIGIN)).ifPresent(type -> {
                if (useTransaction) {
                    transactionTemplate.execute(() -> {
                        publishEvent(type);
                        return null;
                    });
                } else {
                    publishEvent(type);
                }
            });
        }
    }

    private void publishEvent(String type) {
        eventPublisher.publish(new SlackLinkClickedAnalyticEvent(analyticsContextProvider.current(), type));
    }

    @Override
    public void destroy() {
    }
}
