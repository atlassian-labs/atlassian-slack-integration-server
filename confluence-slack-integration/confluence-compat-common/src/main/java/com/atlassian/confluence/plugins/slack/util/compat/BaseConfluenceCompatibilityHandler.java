package com.atlassian.confluence.plugins.slack.util.compat;

import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.sal.api.component.ComponentLocator;

public class BaseConfluenceCompatibilityHandler {
    private SpaceManager spaceManager;

    protected SpaceManager getSpaceManager() {
        if (spaceManager == null) {
            spaceManager = ComponentLocator.getComponent(SpaceManager.class);
        }
        return spaceManager;
    }
}
