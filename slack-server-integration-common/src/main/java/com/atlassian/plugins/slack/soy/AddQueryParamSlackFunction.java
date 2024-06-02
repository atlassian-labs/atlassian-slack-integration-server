package com.atlassian.plugins.slack.soy;

import com.atlassian.soy.renderer.SoyServerFunction;

import javax.ws.rs.core.UriBuilder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

/**
 * addQueryParam(uriOrString, paramName, paramValue)
 */
public class AddQueryParamSlackFunction implements SoyServerFunction<String> {
    @Override
    public String getName() {
        return "addQueryParam";
    }

    @Override
    public Set<Integer> validArgSizes() {
        return Collections.singleton(3);
    }

    @Override
    public String apply(final Object... args) {
        String decodedUri = URLDecoder.decode(args[0].toString(), StandardCharsets.UTF_8);
        return UriBuilder.fromUri(decodedUri)
                .replaceQueryParam(args[1].toString(), args[2].toString())
                .build()
                .toString();
    }
}
