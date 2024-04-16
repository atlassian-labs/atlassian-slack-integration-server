package com.atlassian.plugins.slack.api.json;

import com.atlassian.annotations.VisibleForTesting;
import com.atlassian.json.marshal.Jsonable;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

import static io.atlassian.fugue.Suppliers.memoize;

@Component
public class DefaultJsonableJacksonService implements JsonableJacksonService {
    private Supplier<ObjectMapper> objectMapperSupplier;

    @SuppressWarnings("unused")
    public DefaultJsonableJacksonService() {
        this(ObjectMapper::new);
    }

    @VisibleForTesting
    DefaultJsonableJacksonService(final Supplier<ObjectMapper> mapperSupplier) {
        objectMapperSupplier = memoize(mapperSupplier);
    }

    @Override
    public Jsonable toJsonable(final Object object) {
        return writer -> {
            objectMapperSupplier.get().getJsonFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
            objectMapperSupplier.get().writeValue(writer, object);
        };
    }
}
