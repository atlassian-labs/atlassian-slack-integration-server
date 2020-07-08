package com.atlassian.jira.plugins.slack.util;

import com.atlassian.jira.plugins.slack.model.EventMatcherType;
import com.google.common.collect.Sets;

import java.util.EnumSet;
import java.util.Set;

/**
 * We use this class to hold generic information about resources, plugin keys, and any thing that could cross several
 * classes.
 */
public interface PluginConstants {
    String PLUGIN_KEY = "com.atlassian.jira.plugins.jira-slack-server-integration-plugin";

    //Resources
    String SLACK_JIRA_GLOBAL_IMAGE_RESOURCES = "slack-jira-image-resources";

    // This are the event types we care about
    // If you want to listen to more events just add them here
    Set<EventMatcherType> EVENT_TYPES = Sets.immutableEnumSet(
            EventMatcherType.ISSUE_CREATED,
            EventMatcherType.ISSUE_UPDATED,
            EventMatcherType.ISSUE_COMMENTED,
            EventMatcherType.ISSUE_ASSIGNMENT_CHANGED,
            EventMatcherType.ISSUE_TRANSITIONED);

    EnumSet<EventMatcherType> EVENT_MATCHERS_FOR_DEDICATED_CHANNEL = EnumSet.of(
            EventMatcherType.ISSUE_UPDATED,
            EventMatcherType.ISSUE_COMMENTED,
            EventMatcherType.ISSUE_TRANSITIONED,
            EventMatcherType.ISSUE_ASSIGNMENT_CHANGED);

}
