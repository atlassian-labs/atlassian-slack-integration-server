package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.core.ContentEntityObject;

public interface SlackContentPermissionChecker {
    boolean doesContentHaveViewRestrictions(ContentEntityObject content);
}
