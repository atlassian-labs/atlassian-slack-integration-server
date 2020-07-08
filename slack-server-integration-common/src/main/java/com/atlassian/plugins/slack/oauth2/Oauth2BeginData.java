package com.atlassian.plugins.slack.oauth2;

import com.atlassian.sal.api.user.UserKey;

import javax.servlet.http.HttpServletRequest;

public final class Oauth2BeginData {
    private final HttpServletRequest servletRequest;
    private final String redirect;
    private final String redirectQuery;
    private final String fragment;
    private final UserKey userKey;
    private final String teamId;
    private final String secret;

    public Oauth2BeginData(final HttpServletRequest servletRequest,
                           final String redirect,
                           final String redirectQuery,
                           final String fragment,
                           final UserKey userKey,
                           final String teamId,
                           final String secret) {
        this.servletRequest = servletRequest;
        this.redirect = redirect;
        this.redirectQuery = redirectQuery;
        this.fragment = fragment;
        this.userKey = userKey;
        this.teamId = teamId;
        this.secret = secret;
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public String getRedirect() {
        return redirect;
    }

    public String getRedirectQuery() {
        return redirectQuery;
    }

    public String getFragment() {
        return fragment;
    }

    public UserKey getUserKey() {
        return userKey;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getSecret() {
        return secret;
    }
}
