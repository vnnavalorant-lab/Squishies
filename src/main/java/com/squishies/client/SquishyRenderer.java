package com.squishies.client;

import com.squishies.SquishyBlock;
import com.squishies.SquishyBlockEntity;
import com.squishies.SquishyType;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SquishyRenderer implements BlockEntityRenderer<SquishyBlockEntity> {
    private static final MeshDeform DEFORM = new MeshDeform();
    private static final float OMEGA = 15f;   // rigidez da mola (rad/s)
    private static final float ZETA = 0.30f;  // amortecimento: <1 = balanca (bounce elastico) ao soltar

    public SquishyRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(SquishyBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vcp, int light, int overlay) {
        World world = be.getWorld();
        if (world == null || !(be.getCachedState().getBlock() instanceof SquishyBlock block)) return;
        SquishyType type = block.type();
        SquishyMesh mesh = MeshRegistry.get(type);
        if (mesh == null) return;

        BlockPos pos = be.getPos();
        double now = world.getTime() + tickDelta;

        // 1) quem esta pisando e qual a profundidade (pes abaixo do topo = quanto amassa)
        float target = 0f, ex = 0.5f, ez = 0.5f, rad = be.radius;
        Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + mesh.height + 0.06, pos.getZ() + 1);
        for (LivingEntity le : world.getEntitiesByClass(LivingEntity.class, box, e -> !e.isSpectator())) {
            Vec3d p = le.getLerpedPos(tickDelta);
            float feet = (float) (p.y - pos.getY());
            float depth = MathHelper.clamp(1f - feet / mesh.height, 0f, 0.95f);
            if (depth > 0f && depth >= target) {
                target = Math.min(0.95f, depth + 0.02f);
                ex = (float) (p.x - pos.getX());
                ez = (float) (p.z - pos.getZ());
                rad = le.getWidth() * 0.5f + 0.08f;
            }
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

        mesh.render(DEFORM, matrices, vcp, block.bodyTexture, block.labelTexture, light, overlay);
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
        matrices.translate(-0.5, -mesh.height / 2f, -0.5);
        mesh.render(null, matrices, vcp, block.bodyTexture, block.labelTexture, light, overlay);
        matrices.pop();
    }
}
