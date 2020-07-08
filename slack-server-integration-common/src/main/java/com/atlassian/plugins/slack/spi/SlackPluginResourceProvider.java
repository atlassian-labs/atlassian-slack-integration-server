package com.atlassian.plugins.slack.spi;

import java.util.List;

/**
 * Provides resources to be consumed by base plugin.
 */
public interface SlackPluginResourceProvider {
    String getInstallingImage();

    String getUninstallingImage();

    String getInstalledImage();

    String getPluginKey();

    /**
     * Provide a list of keys for personal personal notification configurations that the common module should render.
     * Each key should have a corresponding label in {@code slack-common.properties}. The page will render them
     * automatically as checkboxes and handle persisting the values to the user settings.
     */
    List<Enum<?>> getPersonalConfigurationKeys();
}
