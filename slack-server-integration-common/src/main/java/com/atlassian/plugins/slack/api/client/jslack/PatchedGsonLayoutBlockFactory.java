package com.atlassian.plugins.slack.api.client.jslack;

import com.github.seratch.jslack.api.model.block.ActionsBlock;
import com.github.seratch.jslack.api.model.block.ContextBlock;
import com.github.seratch.jslack.api.model.block.DividerBlock;
import com.github.seratch.jslack.api.model.block.ImageBlock;
import com.github.seratch.jslack.api.model.block.LayoutBlock;
import com.github.seratch.jslack.api.model.block.SectionBlock;
import com.github.seratch.jslack.common.json.GsonLayoutBlockFactory;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;

/**
 * Factory that handles unknown BlockKit blocks gracefully without throwing exceptions (as it does GsonLayoutBlockFactory).
 */
public class PatchedGsonLayoutBlockFactory extends GsonLayoutBlockFactory {
    // copied from com.github.seratch.jslack.common.json.GsonLayoutBlockFactory.deserialize() without changes
    @Override
    public LayoutBlock deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        final JsonPrimitive prim = (JsonPrimitive) jsonObject.get("type");
        final String blockType = prim.getAsString();
        final Class<? extends LayoutBlock> clazz = getLayoutClassInstance(blockType);
        return context.deserialize(jsonObject, clazz);
    }

    // com.github.seratch.jslack.common.json.GsonLayoutBlockFactory.getLayoutClassInstance() with change of unknown blocks handling
    private Class<? extends LayoutBlock> getLayoutClassInstance(String blockType) {
        switch (blockType) {
            case SectionBlock.TYPE:
                return SectionBlock.class;
            case DividerBlock.TYPE:
                return DividerBlock.class;
            case ImageBlock.TYPE:
                return ImageBlock.class;
            case ContextBlock.TYPE:
                return ContextBlock.class;
            case ActionsBlock.TYPE:
                return ActionsBlock.class;
            default:
                return UnsupportedLayoutBlock.class; // fix
        }
    }
}
