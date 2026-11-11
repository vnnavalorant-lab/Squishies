package com.squishies;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Stage 0 = parado, 1-3 = apertando, 3 = totalmente apertado (fica ate ninguem estar em cima),
 * 4-6 = volta com bounce elastico. Equivale ao Touched/TouchEnded + TweenService do script original.
 */
public class SquishyBlock extends Block {
    public static final IntProperty STAGE = IntProperty.of("stage", 0, 6);
    private final SquishyType type;

    public SquishyBlock(SquishyType type, Settings settings) {
        super(settings);
        this.type = type;
        setDefaultState(getStateManager().getDefaultState().with(STAGE, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(STAGE); }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return type.shape(state.get(STAGE));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return type.shape(state.get(STAGE));
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient || !(entity instanceof LivingEntity) || entity.isSpectator()) return;
        int s = state.get(STAGE);
        if (s >= 1 && s <= 3) return; // ja esta apertado/apertando
        world.setBlockState(pos, state.with(STAGE, 1));
        world.playSound(null, pos, type.press, SoundCategory.BLOCKS, type.pressVolume, 1.0f);
        world.scheduleBlockTick(pos, this, 2);
    }

    private boolean someoneOn(World world, BlockPos pos) {
        return !world.getEntitiesByClass(LivingEntity.class, new Box(pos), e -> !e.isSpectator()).isEmpty();
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int s = state.get(STAGE);
        if (s == 1 || s == 2) {
            world.setBlockState(pos, state.with(STAGE, s + 1));
            world.scheduleBlockTick(pos, this, 2);
        } else if (s == 3) {
            if (someoneOn(world, pos)) {
                world.scheduleBlockTick(pos, this, 4); // continua apertado
            } else {
                world.setBlockState(pos, state.with(STAGE, 4));
                if (type.release != null) world.playSound(null, pos, type.release, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.scheduleBlockTick(pos, this, 3);
            }
        } else if (s == 4 || s == 5) {
            world.setBlockState(pos, state.with(STAGE, s + 1));
            world.scheduleBlockTick(pos, this, 3);
        } else if (s == 6) {
            world.setBlockState(pos, state.with(STAGE, 0));
        }
    }
}
