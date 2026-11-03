package com.atlassian.jira.plugins.slack.system;

import jakarta.annotation.Nonnull;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.atlassian.jira.plugins.slack.system.PluginInfoSource.buildDateFormat;

/**
 * For human-readable serialization of {@link org.joda.time.DateTime}
 */
public class CustomDateTimeSerializer extends ValueSerializer<Date> {
    @Override
    public void serialize(@Nonnull Date value,
                          @Nonnull JsonGenerator jgen,
                          @Nonnull SerializationContext provider) throws JacksonException {
        jgen.writeString(new SimpleDateFormat(buildDateFormat()).format(value));
    }
}
