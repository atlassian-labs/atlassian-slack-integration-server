package com.atlassian.jira.plugins.slack.util;

import org.apache.velocity.app.event.IncludeEventHandler;

/**
 * Limits a template to the content written in the template itself, so that resource-loading directives such as
 * {@code #include} and {@code #parse} resolve to nothing. Returning null leaves the resource uninput, which Velocity
 * treats as a successful operation, so the remainder of the template still renders.
 */
public class InlineOnlyIncludeEventHandler implements IncludeEventHandler {
    @Override
    public String includeEvent(final String includeResourcePath,
                               final String currentResourcePath,
                               final String directiveName) {
        return null;
    }
}
