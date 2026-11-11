package com.squishies;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

/**
 * O visual (malha deformavel) e desenhado pelo BlockEntityRenderer no cliente.
 * Aqui ficam a hitbox, os sons de apertar/soltar e a logica de multibloco (butter = 3x1 blocos).
 *
 * FACING = direcao da fila (parte 0 -> ultima parte). PART = indice do bloco na fila (sempre 0 nos tipos de 1 bloco).
 */
public class SquishyBlock extends Block implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty PART = IntProperty.of("part", 0, SquishyType.MAX_LENGTH - 1);

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
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(PART, 0));
    }

    public SquishyType type() { return type; }
    public String variantId() { return variantId; }

    /** So esta parte desenha a malha (a do meio da fila); as outras apenas ocupam espaco. */
    public boolean isMaster(BlockState state) {
        return type.length == 1 || state.get(PART) == type.masterPart();
    }

    /** Posicao da parte 0 (inicio da fila). */
    public BlockPos originOf(BlockPos pos, BlockState state) {
        return type.length == 1 ? pos : pos.offset(state.get(FACING).getOpposite(), state.get(PART));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction f = ctx.getHorizontalPlayerFacing();
        World world = ctx.getWorld();
        BlockPos origin = ctx.getBlockPos();
        for (int i = 1; i < type.length; i++) {      // a fila inteira precisa estar livre
            BlockPos p = origin.offset(f, i);
            if (world.isOutOfHeightLimit(p) || !world.getWorldBorder().contains(p)) return null;
            if (!world.getBlockState(p).canReplace(ctx)) return null;
        }
        return getDefaultState().with(FACING, f).with(PART, 0);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (type.length == 1) return;
        Direction f = state.get(FACING);
        for (int i = 1; i < type.length; i++) {
            world.setBlockState(pos.offset(f, i), state.with(PART, i), Block.NOTIFY_ALL);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction dir, BlockState neighbor,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (type.length > 1) {
            Direction f = state.get(FACING);
            int part = state.get(PART);
            if (dir == f && part < type.length - 1 && !isPart(neighbor, f, part + 1)) return Blocks.AIR.getDefaultState();
            if (dir == f.getOpposite() && part > 0 && !isPart(neighbor, f, part - 1)) return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, dir, neighbor, world, pos, neighborPos);
    }

    private boolean isPart(BlockState s, Direction f, int part) {
        return s.isOf(this) && s.get(FACING) == f && s.get(PART) == part;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (type.length > 1 && !world.isClient) {
            BlockPos origin = originOf(pos, state);
            Direction f = state.get(FACING);
            // o item so cai da parte 0 (loot table): se quebrou outra parte, solta o drop da parte 0 aqui
            if (!player.isCreative() && state.get(PART) != 0) {
                Block.dropStacks(state.with(PART, 0), world, origin, null, player, player.getMainHandStack());
            }
            for (int i = 0; i < type.length; i++) {
                BlockPos p = origin.offset(f, i);
                if (!p.equals(pos) && world.getBlockState(p).isOf(this)) {
                    world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                }
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

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
