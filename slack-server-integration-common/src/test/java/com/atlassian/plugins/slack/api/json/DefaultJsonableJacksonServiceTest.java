package com.atlassian.plugins.slack.api.json;

import com.atlassian.json.marshal.Jsonable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultJsonableJacksonServiceTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ObjectWriter objectWriter;
    @Mock
    private ObjectWriter configuredObjectWriter;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultJsonableJacksonService service;

    @Before
    public void setUp() throws Exception {
        service = new DefaultJsonableJacksonService(() -> objectMapper);
    }

    @Test
    public void toJsonable_shouldSetExpectedOption() throws Exception {
        when(objectMapper.writer()).thenReturn(objectWriter);
        when(objectWriter.without(StreamWriteFeature.AUTO_CLOSE_TARGET)).thenReturn(configuredObjectWriter);
        StringWriter writer = new StringWriter();
        Map toSerialize = new HashMap<>();

        Jsonable jsonable = service.toJsonable(toSerialize);
        jsonable.write(writer);

        verify(objectWriter).without(StreamWriteFeature.AUTO_CLOSE_TARGET);
        verify(configuredObjectWriter).writeValue(writer, toSerialize);
    }
}
