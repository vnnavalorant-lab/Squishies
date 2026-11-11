package com.squishies;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * O visual (malha deformavel) e desenhado pelo BlockEntityRenderer no cliente.
 * Aqui ficam so a hitbox e os sons de apertar/soltar.
 */
public class SquishyBlock extends Block implements BlockEntityProvider {
    private final SquishyType type;
    private final String variantId;
    public final Identifier bodyTexture;
    public final Identifier labelTexture;

    public SquishyBlock(SquishyType type, String variantId, Settings settings) {
        super(settings);
        this.type = type;
        this.variantId = variantId;
        this.bodyTexture = new Identifier(SquishiesMod.MOD_ID, "textures/entity/" + variantId + ".png");
        this.labelTexture = new Identifier(SquishiesMod.MOD_ID, "textures/entity/" + variantId + "_label.png");
    }

    public SquishyType type() { return type; }
    public String variantId() { return variantId; }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SquishyBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return type.outline();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return type.collision();
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient || !(entity instanceof LivingEntity) || entity.isSpectator()) return;
        if (!(world.getBlockEntity(pos) instanceof SquishyBlockEntity be)) return;
        if (be.pressed) return;
        be.pressed = true;
        world.playSound(null, pos, type.press, SoundCategory.BLOCKS, type.pressVolume, 1.0f);
        world.scheduleBlockTick(pos, this, 2);
    }

    private boolean someoneOn(World world, BlockPos pos) {
        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + type.h + 0.1, pos.getZ() + 1);
        return !world.getEntitiesByClass(LivingEntity.class, box, e -> !e.isSpectator()).isEmpty();
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!(world.getBlockEntity(pos) instanceof SquishyBlockEntity be) || !be.pressed) return;
        if (someoneOn(world, pos)) {
            world.scheduleBlockTick(pos, this, 2); // continua apertado
        } else {
            be.pressed = false;
            if (type.release != null) world.playSound(null, pos, type.release, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }
}
