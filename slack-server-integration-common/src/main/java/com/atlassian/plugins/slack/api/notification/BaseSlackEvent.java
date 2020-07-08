package com.atlassian.plugins.slack.api.notification;

/**
 * Base of all events handled by the integration. Plugins trying to connect custom notifications on certain events
 * should publish events inherited from this class.
 */
public interface BaseSlackEvent {
}
