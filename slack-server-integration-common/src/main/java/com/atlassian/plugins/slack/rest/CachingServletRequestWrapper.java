package com.atlassian.plugins.slack.rest;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;

public class CachingServletRequestWrapper extends HttpServletRequestWrapper {
    private final byte[] body;
    private final LinkedHashMap<String, String[]> formParams;

    public CachingServletRequestWrapper(final HttpServletRequest request) throws IOException {
        super(request);
        body = IOUtils.toByteArray(request.getInputStream());
        formParams = new LinkedHashMap<>(request.getParameterMap());
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new DelegatingServletInputStream(new ByteArrayInputStream(body));
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    public byte[] getBody() {
        return body;
    }

    public LinkedHashMap<String, String[]> getFormParams() {
        return formParams;
    }
}
