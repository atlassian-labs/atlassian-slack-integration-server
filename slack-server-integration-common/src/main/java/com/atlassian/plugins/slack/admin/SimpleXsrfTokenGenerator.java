package com.atlassian.plugins.slack.admin;

import com.atlassian.security.random.DefaultSecureTokenGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class SimpleXsrfTokenGenerator implements XsrfTokenGenerator {
    @Override
    public String getNewToken(final HttpServletRequest request, final String teamId) {
        final HttpSession session = request.getSession();
        final String token = DefaultSecureTokenGenerator.getInstance().generateToken();
        session.setAttribute(token, teamId);
        return token;
    }

    @Override
    public ValidationResult validateToken(final HttpServletRequest request, final String token, final String teamId) {
        if (StringUtils.isBlank(token)) {
            return ValidationResult.UNKNOWN;
        }
        final String tokenValue = (String) request.getSession(true).getAttribute(token);
        if (StringUtils.isBlank(tokenValue)) {
            return ValidationResult.UNKNOWN;
        }
        if (!teamId.equals(tokenValue)) {
            return ValidationResult.INVALID_TEAM;
        }
        return ValidationResult.VALID;
    }
}
