package eu.pb4.mbvpatch;

import eu.pb4.mbvpatch.impl.BarrelModels;
import eu.pb4.mbvpatch.impl.BarrelVariants;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MoreBarrelVariantsPatch implements ModInitializer {
    public static final String MOD_ID = "morebarrelvariants-polymer-patch";
    /** The patched mod, whose barrel textures and translations the patch draws from. */
    public static final String TARGET_MOD_ID = BarrelVariants.TARGET_MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // More Barrel Variants ships every barrel texture and its own language files, so its assets
        // are most of the client-side look. The barrel geometry is a plain vanilla cube, which a
        // client already knows, so the only art this patch has to add is the model that shows the
        // open barrel while it is in use.
        if (!PolymerResourcePackUtils.addModAssets(TARGET_MOD_ID)) {
            LOGGER.error("Could not read assets from {}, barrels will have no textures or names.", TARGET_MOD_ID);
        }

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(BarrelModels::write);

        // The mod's blocks and items are registered by the time this runs, because this mod depends
        // on more_barrel_variants. Overlaying them here means no mixin is needed.
        BarrelVariants.register();

        LOGGER.info("More Barrel Variants Polymer patch loaded: all {} barrel variants are now server-side only.",
                BarrelVariants.WOODS.size());
    }
}
