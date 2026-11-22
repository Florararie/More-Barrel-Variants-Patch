package eu.pb4.mbvpatch.impl;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * Shows one More Barrel Variants barrel on unmodded clients as a barrier plus a display entity
 * holding the barrel model.
 *
 * <p>The client block is a barrier: invisible, solid (so it still collides and still takes the click
 * that opens the menu) and non-occluding, so neighbouring blocks keep their faces. A carrier block
 * whose model draws the barrel would cull its neighbours' faces and fight them over the shared face
 * plane, and there are not enough unused vanilla block states to give every wood and facing its own.
 *
 * <p>The barrel itself is a display entity showing the mod's own block model, which is a plain cube
 * with a wood texture on its sides and a barrel lid on top. Because the lid is a texture on one face,
 * the model has to be turned to match the barrel's facing, including barrels placed on their side or
 * upside down.
 *
 * <p>Trade-off: the client draws break progress for its own block, and a barrier draws no cracks, so
 * mining a barrel shows no cracking overlay until it pops. The server still tracks and applies the
 * break normally.
 */
public record PolyBarrelBlock(Block block, String wood) implements PolymerBlock, BlockWithElementHolder {
    @Override
    public BlockState getPolymerBlockState(BlockState state, @Nullable PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialState) {
        return true;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialState) {
        return new BarrelHolder(world, pos, this, initialState);
    }

    /** The item whose model the display entity draws: the mod's closed model, or the open one. */
    public ItemStack stack(boolean open) {
        ItemStack stack = new ItemStack(Items.BARREL);
        stack.set(DataComponents.ITEM_MODEL, open ? BarrelVariants.openModel(this.wood) : BarrelVariants.itemModel(this.wood));
        return stack;
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(BarrelBlock.FACING) ? state.getValue(BarrelBlock.FACING) : Direction.UP;
    }

    private static boolean isOpen(BlockState state) {
        return state.hasProperty(BarrelBlock.OPEN) && state.getValue(BarrelBlock.OPEN);
    }

    /**
     * Yaw that turns the barrel's lid toward a horizontal facing.
     *
     * <p>Item models are drawn turned around relative to block models, which is why the model starts
     * a half turn off and the yaw values are what a chest uses for the same four facings.
     */
    private static float yaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
    }

    /**
     * Pitch that lays the lid toward a horizontal facing.
     *
     * <p>The sign is negative because a positive pitch tips the model's top away from the player,
     * which leaves the lid on the far side of the barrel.
     *
     * <p>A barrel facing down cannot use pitch: {@code Entity#setXRot} clamps every value to a
     * quarter turn either way, so a half turn collapses onto a quarter and the lid ends up pointing
     * sideways. That facing is turned over by {@link #FLIPPED} instead.
     */
    private static float pitch(Direction facing) {
        return switch (facing) {
            case UP, DOWN -> 0f;
            default -> -90f;
        };
    }

    /** A half turn about the side axis, which is how a barrel hanging under a block is drawn. */
    private static final Quaternionf FLIPPED = new Quaternionf().rotateX((float) Math.PI);
    private static final Quaternionf UPRIGHT = new Quaternionf();

    private static boolean flipped(Direction facing) {
        return facing == Direction.DOWN;
    }

    private static final class BarrelHolder extends ElementHolder {
        private final ServerLevel world;
        private final BlockPos pos;
        private final PolyBarrelBlock barrel;
        private final ItemDisplayElement element;
        private boolean open;
        private float yaw;
        private float pitch;
        private boolean flipped;
        private int skyLight = -1;
        private int blockLight = -1;

        /**
         * Takes the state from the caller instead of reading it: Polymer creates element holders from
         * the chunk constructor while a chunk is being deserialised, and asking the level for a block
         * there re-enters chunk loading and parks the server thread forever on the chunk it is still
         * building. Any stale state is corrected by the first {@link #onTick()}.
         */
        BarrelHolder(ServerLevel world, BlockPos pos, PolyBarrelBlock barrel, BlockState state) {
            this.world = world;
            this.pos = pos;
            this.barrel = barrel;
            this.open = isOpen(state);

            Direction facing = facing(state);
            this.yaw = yaw(facing);
            this.pitch = pitch(facing);
            this.flipped = flipped(facing);

            this.element = new ItemDisplayElement(barrel.stack(this.open));
            this.element.setItemDisplayContext(ItemDisplayContext.NONE);
            this.element.setDisplaySize(2f, 2f);
            this.element.setViewRange(1.5f);
            this.element.setTeleportDuration(0);
            this.element.setInvisible(true);
            this.element.setYaw(this.yaw);
            this.element.setPitch(this.pitch);
            if (this.flipped) {
                this.element.setLeftRotation(FLIPPED);
            }
            this.addElement(this.element);
        }

        /**
         * Lights the display from the space around the barrel instead of from the barrel's own block.
         *
         * <p>A client draws a display entity with the light of the block it stands in, and this one
         * always stands inside the barrel, which is a solid block. A solid block has no light inside
         * it, and a vanilla client is sent those light values with the chunk and never revises them
         * for a block that does not change, so a barrel that was placed while its own cell still
         * counted as air comes back from a rejoin in the dark and only lights up once some other
         * block update makes the client rebuild the light around it.
         *
         * <p>Taking the brightest of the six neighbouring cells is how bright the space a barrel
         * stands in is, which is the light a player expects a barrel to be drawn with. The brightness
         * is part of the display's synced data, so it is already correct when the entity is spawned,
         * and it only costs a packet when the light around the barrel actually changes.
         */
        private void applyBrightness() {
            int sky = 0;
            int block = 0;
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = this.pos.relative(direction);
                sky = Math.max(sky, this.world.getBrightness(LightLayer.SKY, neighbour));
                block = Math.max(block, this.world.getBrightness(LightLayer.BLOCK, neighbour));
            }
            if (sky != this.skyLight || block != this.blockLight) {
                this.skyLight = sky;
                this.blockLight = block;
                this.element.setBrightness(new Brightness(block, sky));
            }
        }

        /**
         * Follows the barrel every tick. The server flips the block's open property while a player has
         * the barrel open, so that property is all the display needs to swap between the closed and
         * open model, and re-reading it also keeps the facing honest after a rotation or a clone.
         */
        @Override
        protected void onTick() {
            if (!this.world.isLoaded(this.pos)) {
                return;
            }

            BlockState state = this.world.getBlockState(this.pos);
            if (state.getBlock() != this.barrel.block()) {
                return;
            }

            this.applyBrightness();

            boolean open = isOpen(state);
            if (open != this.open) {
                this.open = open;
                this.element.setItem(this.barrel.stack(open));
            }

            Direction facing = facing(state);
            float yaw = yaw(facing);
            float pitch = pitch(facing);
            boolean flipped = flipped(facing);
            if (yaw != this.yaw || pitch != this.pitch || flipped != this.flipped) {
                this.yaw = yaw;
                this.pitch = pitch;
                this.flipped = flipped;
                this.element.setYaw(yaw);
                this.element.setPitch(pitch);
                this.element.setLeftRotation(flipped ? FLIPPED : UPRIGHT);
            }
        }
    }
}
