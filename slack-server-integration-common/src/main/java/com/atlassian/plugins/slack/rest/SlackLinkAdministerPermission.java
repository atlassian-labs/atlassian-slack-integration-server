package com.atlassian.plugins.slack.rest;

import javax.ws.rs.NameBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface SlackLinkAdministerPermission {
}
