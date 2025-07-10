package com.atlassian.plugins.slack.admin;

import jakarta.servlet.http.HttpServletRequest;

public interface XsrfTokenGenerator {
    enum ValidationResult {UNKNOWN, INVALID_TEAM, VALID}

    /**
     * Generates a token and stores it in the session. Used to store state during oauth flow.
     */
    String getNewToken(HttpServletRequest request, String teamId);

    ValidationResult validateToken(HttpServletRequest request, String token, String teamId);
}
