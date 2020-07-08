package com.atlassian.jira.plugins.slack.manager;

import com.atlassian.sal.api.pluginsettings.PluginSettings;

/**
 * This class manages the global configuration of the plugin.
 */
public interface PluginConfigurationManager {
    /**
     * Returns if the global configuration is enabled or not
     *
     * @return true if it is enabled, false if not
     */
    boolean isGlobalAutoConvertEnabled();

    /**
     * Sets the value if the global configuration is enabled or not
     *
     * @param value the value to set
     */
    void setGlobalAutoConvertEnabled(boolean value);

    boolean isIssuePanelHidden();

    void setIssuePanelHidden(boolean value);

    /**
     * Verifies if we can send issue previews to externally shared channels.
     * This is a global setting that will check also if the project
     * is enable for searching or not
     *
     * @return true if it is enabled, false if not
     */
    boolean isIssuePreviewForGuestChannelsEnabled();

    /**
     * Overwrites the value to make issue preview enabled or not
     *
     * @param value true/false
     */
    void setIssuePreviewForGuestChannelsEnabled(boolean value);

    /**
     * @return the settings of this plugin
     */
    PluginSettings getSettings();
}
