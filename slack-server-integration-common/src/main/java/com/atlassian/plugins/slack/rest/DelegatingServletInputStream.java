package com.atlassian.plugins.slack.rest;

import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DelegatingServletInputStream extends ServletInputStream {
    private final InputStream delegate;

    public DelegatingServletInputStream(final InputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }
}
