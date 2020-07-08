package com.atlassian.plugins.slack.api.json;

import com.atlassian.json.marshal.Jsonable;

public interface JsonableJacksonService {
    Jsonable toJsonable(Object object);
}
