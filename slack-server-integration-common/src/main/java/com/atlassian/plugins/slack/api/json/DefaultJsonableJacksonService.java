package com.atlassian.plugins.slack.api.json;

import com.atlassian.annotations.VisibleForTesting;
import com.atlassian.json.marshal.Jsonable;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

import static io.atlassian.fugue.Suppliers.memoize;

@Component
public class DefaultJsonableJacksonService implements JsonableJacksonService {
    private Supplier<ObjectMapper> objectMapperSupplier;

    @SuppressWarnings("unused")
    public DefaultJsonableJacksonService() {
        this(() -> JsonMapper.builderWithJackson2Defaults().build());
    }

    @VisibleForTesting
    DefaultJsonableJacksonService(final Supplier<ObjectMapper> mapperSupplier) {
        objectMapperSupplier = memoize(mapperSupplier);
    }

    @Override
    public Jsonable toJsonable(final Object object) {
        return writer -> {
            // Jackson 3 mappers are immutable: disable AUTO_CLOSE_TARGET per write instead of on the shared factory
            objectMapperSupplier.get().writer().without(StreamWriteFeature.AUTO_CLOSE_TARGET).writeValue(writer, object);
        };
    }
}
