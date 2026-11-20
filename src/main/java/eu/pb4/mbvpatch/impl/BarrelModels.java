package eu.pb4.mbvpatch.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import eu.pb4.mbvpatch.MoreBarrelVariantsPatch;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;

/**
 * Writes the item models the display entities use for open barrels into the Polymer pack.
 *
 * <p>Barrels are ordinary block models, so the closed barrel can use the mod's own item definition
 * unchanged. The open model is a separate block model ({@code <wood>_barrel_open}, with a lid
 * texture on top), and the mod ships no item definition for it, so one is generated per wood.
 */
public final class BarrelModels {
    private static final Gson GSON = new Gson();

    private BarrelModels() {
    }

    public static void write(ResourcePackBuilder builder) {
        var written = 0;

        for (var wood : BarrelVariants.WOODS) {
            var path = "assets/%s/items/%s_open.json".formatted(BarrelVariants.ASSETS,
                    BarrelVariants.id(wood));
            builder.addStringData(path, GSON.toJson(model(wood + "_barrel_open")));
            written++;
        }

        MoreBarrelVariantsPatch.LOGGER.info("Generated {} open barrel models for the Polymer pack", written);
    }

    /** An item definition that draws one of the mod's block models. */
    private static JsonObject model(String blockModel) {
        var inner = new JsonObject();
        inner.addProperty("type", "minecraft:model");
        inner.addProperty("model", BarrelVariants.target("block/" + blockModel).toString());

        var json = new JsonObject();
        json.add("model", inner);
        return json;
    }
}
