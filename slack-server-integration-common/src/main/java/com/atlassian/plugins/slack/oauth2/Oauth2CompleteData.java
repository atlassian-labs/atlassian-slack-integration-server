package com.atlassian.plugins.slack.oauth2;

import com.atlassian.sal.api.user.UserKey;

import javax.servlet.http.HttpServletRequest;

public final class Oauth2CompleteData {
    private final String code;
    private final UserKey userKey;
    private final String teamId;
    private final HttpServletRequest request;
    private final String state;

    public Oauth2CompleteData(final String code,
                              final HttpServletRequest request,
                              final UserKey userKey,
                              final String teamId,
                              final String state) {
        this.code = code;
        this.request = request;
        this.userKey = userKey;
        this.teamId = teamId;
        this.state = state;
    }

    public String getCode() {
        return code;
    }

    public UserKey getUserKey() {
        return userKey;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getState() {
        return state;
    }
}
