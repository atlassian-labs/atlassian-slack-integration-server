package com.atlassian.confluence.plugins.slack.util.compat;

import com.atlassian.confluence.util.GeneralUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConfluenceCompatibilityDispatcher {
    private ConfluenceCompatibilityHandler handler;

    public ConfluenceCompatibilityHandler getHandler() {
        if (handler == null) {
            String versionNumber = GeneralUtil.getVersionNumber();
            log.debug("Detected product version: {}", versionNumber);

            String[] versionParts = versionNumber.split("\\.");
            if (versionParts.length < 1) {
                throw new IllegalStateException("Version number is empty: " + versionNumber);
            }

            String majorVersionString = versionParts[0];
            try {
                int majorVersion = Integer.parseUnsignedInt(majorVersionString);
                handler = majorVersion > 7
                        ? Confluence8CompatibilityHandler.INSTANCE
                        : Confluence7CompatibilityHandler.INSTANCE;
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Major version is not an integer: " + majorVersionString, e);
            }
        }

        return handler;
    }
}
