package com.atlassian.jira.plugins.slack.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Creates and populates a {@link com.atlassian.jira.plugins.slack.system.PluginInfo}
 */
public final class PluginInfoSource {
    private static final Logger log = LoggerFactory.getLogger(PluginInfoSource.class);

    public static final String BUILD_PROPERTIES_FILE = "/build.properties";

    public static final String PROPERTY_NAME_VERSION = "version";
    public static final String PROPERTY_NAME_BUILD_TIMESTAMP = "build.timestamp";
    public static final String PROPERTY_NAME_BUILD_TIMESTAMP_FORMAT = "build.timestamp.format";

    private static final String version;
    private static final Date buildDate;
    private static final String buildDateFormat;

    static {
        final InputStream propsFile = PluginInfoSource.class.getResourceAsStream(BUILD_PROPERTIES_FILE);
        final Properties props = new Properties();

        try {
            props.load(propsFile);
        } catch (IOException e) {
            log.warn("Failed to load build properties.", e);
        }

        version = props.getProperty(PROPERTY_NAME_VERSION);
        final String buildDateStr = props.getProperty(PROPERTY_NAME_BUILD_TIMESTAMP);
        buildDateFormat = props.getProperty(PROPERTY_NAME_BUILD_TIMESTAMP_FORMAT);

        if (isBlank(buildDateFormat)) {
            log.warn("Build date format string is invalid. Check the pom file!");
            throw new IllegalStateException("Invalid build date format string. Check the pom file!");
        }

        try {
            buildDate = new SimpleDateFormat(buildDateFormat).parse(buildDateStr);
        } catch (ParseException e) {
            log.warn("Failed to parse build date: " + buildDateStr, e);
            throw new IllegalStateException("Invalid build date format.", e);
        }
    }

    private PluginInfoSource() {
    }

    public static PluginInfo getPluginInfo() {
        if (isBlank(version) || buildDate == null) {
            throw new IllegalStateException("Failed to obtain plugin info.");
        }

        return new PluginInfo(version, buildDate);
    }

    static String buildDateFormat() {
        return buildDateFormat;
    }
}
