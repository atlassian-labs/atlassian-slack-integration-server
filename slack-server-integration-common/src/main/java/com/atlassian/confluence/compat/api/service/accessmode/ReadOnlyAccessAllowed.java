package com.atlassian.confluence.compat.api.service.accessmode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that the annotated method (or all methods in the class, or all the methods in all the classes in the package)
 * should be bypassed by read-only access check.
 * WARNING: This annotation must be used for admin actions only or user usage tracking services, e.g. recently viewed, analytics
 *
 * This class is copied from 'com.atlassian.confluence.compat:confluence-compat-lib' dependency to mark REST resources
 * in common module as Confluence read-only mode compatible.<br>
 * Class from the original package wasn't used because
 * <ol>
 *     <li>
 *         Including 'confluence-compat-lib' into Confluence plugin module would require refactoring of the
 *         REST resources in common module and in all product plugin modules.
 *      </li>
 *      <li>
 *          Including 'confluence-compat-lib' into common module would make it impossible to launch Jira and BB plugins
 *          because of absence of Confluence API classes, compat library depends on.
 *      </li>
 * <ol/>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.PACKAGE})
public @interface ReadOnlyAccessAllowed {
}
