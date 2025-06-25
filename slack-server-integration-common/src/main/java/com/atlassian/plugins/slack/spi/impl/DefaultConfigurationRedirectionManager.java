package com.atlassian.plugins.slack.spi.impl;

import com.atlassian.plugins.slack.spi.ConfigurationRedirectionManager;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;

@Component("defaultConfigurationRedirectionManager")
public class DefaultConfigurationRedirectionManager implements ConfigurationRedirectionManager {
    @Override
    public Optional<URI> getRedirectUri(HttpServletRequest request) {
        return Optional.empty();
    }
}
