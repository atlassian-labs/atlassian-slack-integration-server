package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.security.ContentPermissionSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultSlackContentPermissionChecker implements SlackContentPermissionChecker {
    private final ContentPermissionManager contentPermissionManager;

    @Autowired
    public DefaultSlackContentPermissionChecker(final ContentPermissionManager contentPermissionManager) {
        this.contentPermissionManager = contentPermissionManager;
    }

    @Override
    public boolean doesContentHaveViewRestrictions(final ContentEntityObject content) {
        final List<ContentPermissionSet> permissionSets = contentPermissionManager
                .getContentPermissionSets(content, ContentPermission.VIEW_PERMISSION);

        for (ContentPermissionSet permissionSet : permissionSets) {
            if (!permissionSet.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
