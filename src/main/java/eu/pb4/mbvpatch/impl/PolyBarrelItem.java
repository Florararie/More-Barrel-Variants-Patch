package eu.pb4.mbvpatch.impl;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Client representation of a More Barrel Variants barrel item.
 *
 * <p>The sent item is a vanilla barrel, which every client can place and hold; the wood sprite comes
 * from the item model override, because the client resolves a model from the item's model component
 * and a plain vanilla barrel would draw spruce.
 *
 * <p>The name stays translatable: the mod's own language files are part of the pack, so
 * {@code block.more_barrel_variants.oak_barrel} turns into "Oak Barrel" and is translated like any
 * vanilla item.
 */
public record PolyBarrelItem(Identifier model) implements PolymerItem {
    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.BARREL;
    }

    /**
     * The mod's own item model from the pack.
     *
     * <p>The mod's item definition is a plain model naming its own block model, which an unmodded
     * client understands, so the inventory and advancement icons are the real barrel art.
     */
    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context, HolderLookup.Provider lookup) {
        return this.model;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context,
                                         HolderLookup.Provider lookup) {
        ItemStack out = PolymerItemUtils.createItemStack(itemStack, tooltipType, context, lookup);
        // A block item's description id is block.more_barrel_variants.<id>, which is exactly what the
        // mod's language files translate.
        out.set(DataComponents.ITEM_NAME, Component.translatable(itemStack.getItem().getDescriptionId()));
        return out;
    }

    @Override
    public boolean isPolymerBlockInteraction(BlockState state, ServerPlayer player, InteractionHand hand,
                                             ItemStack stack, ServerLevel world, BlockHitResult blockHitResult,
                                             InteractionResult actionResult) {
        return true;
    }

    @Override
    public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state, ServerPlayer player,
                                                                     InteractionHand hand, ItemStack stack,
                                                                     ServerLevel world, BlockHitResult blockHitResult) {
        return true;
    }
}
