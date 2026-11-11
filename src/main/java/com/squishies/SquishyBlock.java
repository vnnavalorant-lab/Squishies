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
 * Multibloco: grade cellsX x cellsZ. FACING = direcao do eixo X da grade; o eixo Z vai para a direita do FACING.
 * PART = i + j * cellsX (i ao longo do FACING, j para a direita); a parte 0 e o canto de origem (sempre 0 nos tipos de 1 bloco).
 */
public class SquishyBlock extends Block implements BlockEntityProvider {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final IntProperty PART = IntProperty.of("part", 0, SquishyType.MAX_PARTS - 1);

    private final SquishyType type;
    private final String variantId;
    public final Identifier bodyTexture;
    public final Identifier labelTexture;

    public SquishyBlock(SquishyType type, String variantId, Settings settings) {
        super(settings);
        this.type = type;
        this.variantId = variantId;
        this.bodyTexture = new Identifier(SquishiesMod.MOD_ID, "textures/entity/" + variantId + ".png");
        this.labelTexture = new Identifier(SquishiesMod.MOD_ID, "textures/entity/"
                + (type.sharedLabel != null ? type.sharedLabel : variantId + "_label") + ".png");
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(PART, 0));
    }

    public SquishyType type() { return type; }
    public String variantId() { return variantId; }

    /** So esta parte desenha a malha inteira (a mais central da grade); as outras apenas ocupam espaco. */
    public boolean isMaster(BlockState state) {
        return state.get(PART) == type.masterPart();
    }

    private static Direction across(Direction f) { return f.rotateYClockwise(); }

    /** Posicao do bloco da grade (i, j) a partir da origem (parte 0). */
    private BlockPos cellPos(BlockPos origin, Direction f, int part) {
        int i = part % type.cellsX, j = part / type.cellsX;
        return origin.offset(f, i).offset(across(f), j);
    }

    /** Posicao da parte 0 (canto de origem da grade). */
    public BlockPos originOf(BlockPos pos, BlockState state) {
        Direction f = state.get(FACING);
        int part = state.get(PART), i = part % type.cellsX, j = part / type.cellsX;
        return pos.offset(f.getOpposite(), i).offset(across(f).getOpposite(), j);
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
        for (int p = 1; p < type.cells(); p++) {      // a grade inteira precisa estar livre
            BlockPos q = cellPos(origin, f, p);
            if (world.isOutOfHeightLimit(q) || !world.getWorldBorder().contains(q)) return null;
            if (!world.getBlockState(q).canReplace(ctx)) return null;
        }
        return getDefaultState().with(FACING, f).with(PART, 0);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (type.cells() == 1) return;
        Direction f = state.get(FACING);
        for (int p = 1; p < type.cells(); p++) {
            world.setBlockState(cellPos(pos, f, p), state.with(PART, p), Block.NOTIFY_ALL);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction dir, BlockState neighbor,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (type.cells() > 1) {
            Direction f = state.get(FACING), a = across(f);
            int part = state.get(PART), i = part % type.cellsX, j = part / type.cellsX;
            int want = -1;                                   // parte que deve existir do outro lado
            if (dir == f && i + 1 < type.cellsX) want = part + 1;
            else if (dir == f.getOpposite() && i > 0) want = part - 1;
            else if (dir == a && j + 1 < type.cellsZ) want = part + type.cellsX;
            else if (dir == a.getOpposite() && j > 0) want = part - type.cellsX;
            if (want >= 0 && !isPart(neighbor, f, want)) return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, dir, neighbor, world, pos, neighborPos);
    }

    private boolean isPart(BlockState s, Direction f, int part) {
        return s.isOf(this) && s.get(FACING) == f && s.get(PART) == part;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (type.cells() > 1 && !world.isClient) {
            BlockPos origin = originOf(pos, state);
            Direction f = state.get(FACING);
            // o item so cai da parte 0 (loot table): se quebrou outra parte, solta o drop da parte 0 aqui
            if (!player.isCreative() && state.get(PART) != 0) {
                Block.dropStacks(state.with(PART, 0), world, origin, null, player, player.getMainHandStack());
            }
            for (int p = 0; p < type.cells(); p++) {
                BlockPos q = cellPos(origin, f, p);
                if (!q.equals(pos) && world.getBlockState(q).isOf(this)) {
                    world.setBlockState(q, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
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
        return type.outline(state.get(FACING), state.get(PART));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return type.collision(state.get(FACING), state.get(PART));
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
