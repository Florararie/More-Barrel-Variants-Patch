package eu.pb4.mbvpatch.impl;

import eu.pb4.mbvpatch.MoreBarrelVariantsPatch;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The registry ids More Barrel Variants uses, and the overlays that hide them from vanilla clients.
 *
 * <p>Everything is looked up by id rather than by class, so the mod is not a compile dependency and
 * a version bump that keeps its ids cannot break the patch.
 */
public final class BarrelVariants {
    public static final String TARGET_MOD_ID = "more_barrel_variants";
    /** Namespace for the models this patch generates into the Polymer pack. */
    public static final String ASSETS = "mbvpatch";

    /**
     * The woods the mod adds.
     *
     * <p>Spruce is missing on purpose: it is the vanilla barrel, which the mod renames to "Spruce
     * Barrel" rather than registering again.
     */
    public static final List<String> WOODS = List.of(
            "oak", "birch", "jungle", "acacia", "dark_oak", "pale_oak", "mangrove",
            "cherry", "bamboo", "crimson", "warped");

    private BarrelVariants() {
    }

    public static Identifier target(String path) {
        return Identifier.fromNamespaceAndPath(TARGET_MOD_ID, path);
    }

    public static Identifier asset(String path) {
        return Identifier.fromNamespaceAndPath(ASSETS, path);
    }

    /** The block and item id the mod registers for a barrel, which are the same string. */
    public static String id(String wood) {
        return wood + "_barrel";
    }

    /** The mod's own item model, which draws the closed barrel. */
    public static Identifier itemModel(String wood) {
        return target(id(wood));
    }

    /** The generated item model that draws the barrel with its lid open. */
    public static Identifier openModel(String wood) {
        return asset(id(wood) + "_open");
    }

    public static void register() {
        var blocks = 0;
        var items = 0;

        for (var wood : WOODS) {
            var id = target(id(wood));

            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block == null) {
                MoreBarrelVariantsPatch.LOGGER.error("More Barrel Variants has no block {}, skipping it", id);
                continue;
            }

            var overlay = new PolyBarrelBlock(block, wood);
            // One overlay for the client block state and one for the display entity drawing it.
            PolymerBlock.registerOverlay(block, overlay);
            BlockWithElementHolder.registerOverlay(block, overlay);
            blocks++;

            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == null) {
                MoreBarrelVariantsPatch.LOGGER.error("More Barrel Variants has no item {}, skipping it", id);
                continue;
            }

            // The sent item is a vanilla barrel showing the mod's own model from the pack.
            PolymerItem.registerOverlay(item, new PolyBarrelItem(itemModel(wood)));
            items++;
        }

        // Unlike the chest variants, nothing has to be done about block entities: every barrel uses
        // the vanilla barrel block entity type, which a client without the mod already knows.
        MoreBarrelVariantsPatch.LOGGER.info("Virtualised {} barrel blocks and {} barrel items", blocks, items);
    }
}
