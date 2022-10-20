package com.atlassian.plugins.slack.api.client.jslack;

import com.github.seratch.jslack.SlackConfig;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.model.block.ContextBlockElement;
import com.github.seratch.jslack.api.model.block.LayoutBlock;
import com.github.seratch.jslack.api.model.block.composition.TextObject;
import com.github.seratch.jslack.api.model.block.element.BlockElement;
import com.github.seratch.jslack.common.http.SlackHttpClient;
import com.github.seratch.jslack.common.json.GsonBlockElementFactory;
import com.github.seratch.jslack.common.json.GsonContextBlockElementFactory;
import com.github.seratch.jslack.common.json.GsonTextObjectFactory;
import com.github.seratch.jslack.common.json.UnknownPropertyDetectionAdapterFactory;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import java.io.IOException;

/**
 * Main purpose of this class it to pass PatchedGsonLayoutBlockFactory instead of GsonLayoutBlockFactory provided by
 * jslack 1.5.6 as the standard factory throws exceptions when an unknown BlockKit block is received from the Slack API.
 */
public class PatchedSlackHttpClient extends SlackHttpClient {
    public PatchedSlackHttpClient(OkHttpClient okHttpClient) {
        super(okHttpClient);
    }

    // copied from com.github.seratch.jslack.common.http.SlackHttpClient#parseJsonResponse(...) Gson replacement
    @Override
    public <T> T parseJsonResponse(Response response, Class<T> clazz) throws IOException, SlackApiException {
        if (response.code() == 200) {
            String body = response.body().string();
            runHttpResponseListeners(response, body);
            return createSnakeCase(getConfig()).fromJson(body, clazz); // fix
        } else {
            String body = response.body().string();
            throw new SlackApiException(response, body);
        }
    }

    // copied from com.github.seratch.jslack.common.json.GsonFactory#createSnakeCase(SlackConfig) with GsonLayoutBlockFactory modification
    private Gson createSnakeCase(SlackConfig config) {
        GsonBuilder gsonBuilder = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(LayoutBlock.class, new PatchedGsonLayoutBlockFactory()) // fix
                .registerTypeAdapter(TextObject.class, new GsonTextObjectFactory())
                .registerTypeAdapter(ContextBlockElement.class, new GsonContextBlockElementFactory())
                .registerTypeAdapter(BlockElement.class, new GsonBlockElementFactory());
        if (config.isLibraryMaintainerMode()) {
            gsonBuilder = gsonBuilder.registerTypeAdapterFactory(new UnknownPropertyDetectionAdapterFactory());
        }
        if (config.isPrettyResponseLoggingEnabled()) {
            gsonBuilder = gsonBuilder.setPrettyPrinting();
        }
        return gsonBuilder.create();
    }
}
