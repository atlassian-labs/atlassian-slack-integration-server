package com.atlassian.plugins.slack.rest;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DelegatingServletInputStream extends ServletInputStream {
    private final InputStream delegate;
    private boolean finished = false;

    public DelegatingServletInputStream(InputStream delegate) {
        this.delegate = delegate;
    }

    public final InputStream getSourceStream() {
        return this.delegate;
    }

    public int read() throws IOException {
        int data = this.delegate.read();
        if (data == -1) {
            this.finished = true;
        }

        return data;
    }

    public int available() throws IOException {
        return this.delegate.available();
    }

    public void close() throws IOException {
        super.close();
        this.delegate.close();
    }

    @Override
    public boolean isFinished() {
        return this.finished;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException();
    }
}
