package com.squishies.client;

import com.squishies.SquishyBlock;
import com.squishies.SquishyBlockEntity;
import com.squishies.SquishyType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

public class SquishyRenderer implements BlockEntityRenderer<SquishyBlockEntity> {
    private static final MeshDeform DEFORM = new MeshDeform();
    private static final float OMEGA = 15f;   // rigidez da mola (rad/s)
    private static final float ZETA = 0.30f;  // amortecimento: <1 = balanca (bounce elastico) ao soltar

    // --- otimizacao (blocos) ---
    private static final double RENDER_DIST = 48.0;          // alem disso nem desenha
    private static final double ACTIVE_DIST2 = 24.0 * 24.0;  // alem disso: sem amassado e sem procurar entidades
    private static final double CULL_COS = 0.20;             // descarta o que esta fora do cone de visao (~78 graus)

    public SquishyRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public int getRenderDistance() { return (int) RENDER_DIST; }

    @Override
    public void render(SquishyBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light, int overlay) {
        World world = be.getWorld();
        if (world == null || !(be.getCachedState().getBlock() instanceof SquishyBlock block)) return;
        SquishyType type = block.type();
        BlockState state = be.getCachedState();
        if (!block.isMaster(state)) return;           // multibloco: so a parte do meio desenha a malha inteira
        SquishyMesh mesh = MeshRegistry.get(type);
        if (mesh == null) return;

        BlockPos pos = be.getPos();
        double now = world.getTime() + tickDelta;

        // ---- descarte barato: atras da camera / fora do cone de visao (rendersOutsideBoundingBox desliga o culling do jogo)
        Camera cam = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cp = cam.getPos();
        double vx = pos.getX() + 0.5 - cp.x, vy = pos.getY() + 0.25 - cp.y, vz = pos.getZ() + 0.5 - cp.z;
        double dist2 = vx * vx + vy * vy + vz * vz;
        double reach = Math.sqrt(type.cellsX * type.cellsX + type.cellsZ * type.cellsZ) * 0.5 + 1.0;   // raio aproximado da malha
        if (dist2 > reach * reach * 4.0) {
            double yaw = Math.toRadians(cam.getYaw()), pitch = Math.toRadians(cam.getPitch());
            double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
            double dot = vx * fx + vy * fy + vz * fz;
            if (dot < Math.sqrt(dist2) * CULL_COS - reach) return;
        }
        boolean active = dist2 < ACTIVE_DIST2;                   // perto o bastante para amassar

        // espaco da malha: X em [0, cellsX] ao longo do FACING, Z em [0, cellsZ] para a direita. O bloco mestre fica no centro.
        int cx = type.cellsX, cz = type.cellsZ, mi = type.masterI(), mj = type.masterJ();
        boolean multi = type.cells() > 1 || type.directional;   // directional: 1x1 que gira com o FACING
        Direction f = multi ? state.get(SquishyBlock.FACING) : Direction.EAST;
        Direction across = f.rotateYClockwise();
        float angle = multi ? 270f - f.asRotation() : 0f;       // gira o eixo X da malha para a direcao do FACING
        float cos = (float) Math.cos(Math.toRadians(angle)), sin = (float) Math.sin(Math.toRadians(angle));
        float meshOx = mi + 0.5f, meshOz = mj + 0.5f;           // ponto da malha sob o centro do bloco mestre

        // caixa (em coordenadas do mundo) da grade inteira de blocos
        BlockPos first = multi ? pos.offset(f.getOpposite(), mi).offset(across.getOpposite(), mj) : pos;
        BlockPos last = multi ? first.offset(f, cx - 1).offset(across, cz - 1) : pos;
        Box box = new Box(Math.min(first.getX(), last.getX()), pos.getY(), Math.min(first.getZ(), last.getZ()),
                Math.max(first.getX(), last.getX()) + 1, pos.getY() + mesh.height + 0.06, Math.max(first.getZ(), last.getZ()) + 1);

        // 1) quem esta pisando e qual a profundidade (pes abaixo do topo = quanto amassa)
        float target = 0f, ex = 0.5f, ez = 0.5f, rad = be.radius;
        if (active) {
            // no maximo 1 consulta por tick quando tem algo acontecendo, 1 a cada 3 ticks quando esta parado
            long tick = world.getTime();
            boolean busy = be.hadContact || Math.abs(be.press) > 0.004f || Math.abs(be.vel) > 0.03f;
            if (be.queryTick < 0 || tick - be.queryTick >= (busy ? 1 : 3) || tick < be.queryTick) {   // (sem subtrair Long.MIN_VALUE: estourava)
                be.queryTick = tick;
                List<LivingEntity> found = world.getEntitiesByClass(LivingEntity.class, box, e -> !e.isSpectator());
                be.nearby = found.isEmpty() ? Collections.emptyList() : found;
            }
        } else if (!be.nearby.isEmpty()) {
            be.nearby = Collections.emptyList();
        }
        for (LivingEntity le : be.nearby) {
            if (le.isRemoved()) continue;
            Vec3d p = le.getLerpedPos(tickDelta);
            float feet = (float) (p.y - pos.getY());
            float depth = MathHelper.clamp(1f - feet / mesh.height, 0f, 0.95f);
            if (depth > 0f && depth >= target) {
                target = Math.min(0.95f, depth + 0.02f);
                // mundo -> espaco da malha (inverso da rotacao aplicada ao desenhar)
                float rx = (float) (p.x - (pos.getX() + 0.5)), rz = (float) (p.z - (pos.getZ() + 0.5));
                ex = cos * rx - sin * rz + meshOx;
                ez = sin * rx + cos * rz + meshOz;
                rad = le.getWidth() * 0.5f + 0.08f;
            }
        }

        // repouso total: nada em cima e a mola ja parou -> pula a fisica e desenha a malha leve
        if (target == 0f && !be.hadContact && Math.abs(be.press) < 0.004f && Math.abs(be.vel) < 0.03f) {
            be.press = 0f; be.vel = 0f; be.lastTime = now;
            DEFORM.active = false;
            drawMesh(mesh, block, matrices, vcp, light, overlay, multi, angle, meshOx, meshOz);
            return;
        }

        // 2) passo de tempo
        float dt = Float.isNaN((float) be.lastTime) ? 0f : (float) ((now - be.lastTime) * 0.05);
        be.lastTime = now;
        if (dt < 0f || dt > 0.25f) dt = 0.016f;

        // 3) o centro do amassado segue o pe; no primeiro contato ele "nasce" embaixo do pe
        if (target > 0.02f) {
            if (!be.hadContact && Math.abs(be.press) < 0.05f) { be.cx = ex; be.cz = ez; be.radius = rad; }
            else {
                float k = Math.min(1f, dt * 18f);
                be.cx += (ex - be.cx) * k; be.cz += (ez - be.cz) * k; be.radius += (rad - be.radius) * k;
            }
            be.hadContact = true;
        } else {
            be.hadContact = false;
        }

        // 4) mola amortecida em direcao a profundidade alvo (da o bounce ao soltar)
        if (dt > 0f) {
            int steps = Math.max(1, (int) Math.ceil(dt / 0.01f));
            float h = dt / steps;
            for (int i = 0; i < steps; i++) {
                float a = OMEGA * OMEGA * (target - be.press) - 2f * ZETA * OMEGA * be.vel;
                be.vel += a * h;
                be.press += be.vel * h;
            }
            be.press = MathHelper.clamp(be.press, -0.35f, 0.95f);
        }

        DEFORM.active = target > 0f || Math.abs(be.press) > 0.004f || Math.abs(be.vel) > 0.03f;
        DEFORM.press = be.press;
        DEFORM.cx = be.cx; DEFORM.cz = be.cz; DEFORM.r = Math.max(0.12f, be.radius);

        drawMesh(mesh, block, matrices, vcp, light, overlay, multi, angle, meshOx, meshOz);
    }

    private static void drawMesh(SquishyMesh mesh, SquishyBlock block, MatrixStack matrices, VertexConsumerProvider vcp,
                                 int light, int overlay, boolean multi, float angle, float meshOx, float meshOz) {
        matrices.push();
        if (multi) {
            matrices.translate(0.5, 0, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
            matrices.translate(-meshOx, 0, -meshOz);
        }
        mesh.render(DEFORM, matrices, vcp, block.bodyTexture, block.labelTexture, light, overlay);
        matrices.pop();
    }

    /** A malha do multibloco passa dos limites do bloco: nao deixa o jogo descartar o desenho fora da tela. */
    @Override
    public boolean rendersOutsideBoundingBox(SquishyBlockEntity be) {
        return true;
    }

    /** Desenho do item (inventario, mao, chao): a malha em repouso, centralizada e ajustada ao tamanho do slot. */
    public static void renderItem(SquishyBlock block, MatrixStack matrices, VertexConsumerProvider vcp, int light, int overlay) {
        SquishyType type = block.type();
        SquishyMesh mesh = MeshRegistry.get(type);
        if (mesh == null) return;
        float s = 0.78f / Math.max(type.w, Math.max(type.d, mesh.height));
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(s, s, s);
        matrices.translate(-type.cellsX / 2f, -mesh.height / 2f, -type.cellsZ / 2f);
        mesh.render(null, matrices, vcp, block.bodyTexture, block.labelTexture, light, overlay);   // null = malha leve
        matrices.pop();
    }
}
