package com.atlassian.confluence.plugins.slack.spacetochannel.service;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.ContentPermissionManager;
import com.atlassian.confluence.security.ContentPermission;
import com.atlassian.confluence.security.ContentPermissionSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultSlackContentPermissionCheckerTest {
    @Mock
    private ContentPermissionManager contentPermissionManager;
    @Mock
    private ContentEntityObject contentEntityObject;
    @Mock
    private ContentPermissionSet contentPermissions;

    @InjectMocks
    private DefaultSlackContentPermissionChecker target;

    @Test
    public void doesContentHaveViewRestrictions_shouldReturnTrueWhenPermissionsArePresent() {
        when(contentPermissionManager.getContentPermissionSets(contentEntityObject, ContentPermission.VIEW_PERMISSION))
                .thenReturn(Collections.singletonList(contentPermissions));
        when(contentPermissions.isEmpty()).thenReturn(false);

        boolean result = target.doesContentHaveViewRestrictions(contentEntityObject);

        assertThat(result, is(true));
    }

    @Test
    public void doesContentHaveViewRestrictions_shouldReturnFalseWhenPermissionsAreNotPresent() {
        when(contentPermissionManager.getContentPermissionSets(contentEntityObject, ContentPermission.VIEW_PERMISSION))
                .thenReturn(Collections.singletonList(contentPermissions));
        when(contentPermissions.isEmpty()).thenReturn(true);

        boolean result = target.doesContentHaveViewRestrictions(contentEntityObject);

        assertThat(result, is(false));
    }

    @Test
    public void doesContentHaveViewRestrictions_shouldReturnFalseWhenPermissionIsEmpty() {
        when(contentPermissionManager.getContentPermissionSets(contentEntityObject, ContentPermission.VIEW_PERMISSION))
                .thenReturn(Collections.emptyList());

        boolean result = target.doesContentHaveViewRestrictions(contentEntityObject);

        assertThat(result, is(false));
    }
}
