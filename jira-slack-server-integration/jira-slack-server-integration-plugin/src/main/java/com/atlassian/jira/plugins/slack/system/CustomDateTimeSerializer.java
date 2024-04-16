package com.atlassian.jira.plugins.slack.system;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.atlassian.jira.plugins.slack.system.PluginInfoSource.buildDateFormat;

/**
 * For human-readable serialization of {@link org.joda.time.DateTime}
 */
public class CustomDateTimeSerializer extends JsonSerializer<Date> {
    @Override
    public void serialize(@Nonnull Date value,
                          @Nonnull JsonGenerator jgen,
                          @Nonnull SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeString(new SimpleDateFormat(buildDateFormat()).format(value));
    }
}
