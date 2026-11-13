package com.atlassian.plugins.slack.soy;

import com.atlassian.soy.renderer.SoyServerFunction;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class SimpleJsonSerializeFunction implements SoyServerFunction<String> {
    private static final Logger log = LoggerFactory.getLogger(SimpleJsonSerializeFunction.class);
    private static final Set<Integer> VALID_ARG_SIZES = ImmutableSet.of(1);
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builderWithJackson2Defaults().build();

    @Override
    public String apply(Object... args) {
        final Object object = args[0];
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JacksonException e) {
            log.error("Error serializing " + object, e);
            return "";
        }
    }

    @Override
    public String getName() {
        return "simpleJsonSerialize";
    }

    @Override
    public Set<Integer> validArgSizes() {
        return VALID_ARG_SIZES;
    }
}
