package com.atlassian.plugins.slack.api.json;

import com.atlassian.json.marshal.Jsonable;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultJsonableJacksonServiceTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private JsonFactory jsonFactory;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DefaultJsonableJacksonService service;

    @Before
    public void setUp() throws Exception {
        service = new DefaultJsonableJacksonService(() -> objectMapper);
    }

    @Test
    public void toJsonable_shouldSetExpectedOption() throws Exception {
        when(objectMapper.getJsonFactory()).thenReturn(jsonFactory);
        StringWriter writer = new StringWriter();
        Map toSerialize = new HashMap<>();

        Jsonable jsonable = service.toJsonable(toSerialize);
        jsonable.write(writer);

        verify(jsonFactory).disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        verify(objectMapper).writeValue(writer, toSerialize);
    }
}
