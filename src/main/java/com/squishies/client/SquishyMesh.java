package com.squishies.client;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Malha macia: desenha as camadas aplicando, vertice a vertice, o amassado em volta de onde o jogador pisa. */
public class SquishyMesh {
    public final List<MeshLayer> layers = new ArrayList<>();
    public float height = 0.2f;
    /** Versao mais leve da mesma forma, usada em repouso e de longe (null = usa esta mesma). */
    public SquishyMesh idle;

    public SquishyMesh idleMesh() { return idle != null ? idle : this; }

    // luz fixa para um brilho falso (especular) que se move conforme a superficie amassa
    private static final float HX = 0.35f, HY = 0.85f, HZ = 0.40f;
    private static final float DECAL_OFFSET = 0.0012f;

    public void finish() {
        float h = 0f;
        for (MeshLayer l : layers) {
            l.finish();
            for (float yy : l.y) h = Math.max(h, yy);
            // brilho em repouso e fixo: calcula uma vez por vertice em vez de a cada frame
            int n = l.x.length;
            l.restCol = new int[n];
            for (int i = 0; i < n; i++) {
                int w = l.weld[i];
                l.restCol[i] = shade(l.bwx[w], l.bwy[w], l.bwz[w]);
            }
        }
        height = Math.max(h, 0.02f);
        if (idle != null) idle.finish();
    }

    private static int shade(float vnx, float vny, float vnz) {
        float s = Math.max(0f, vnx * HX + vny * HY + vnz * HZ);
        s = s * s; s = s * s;                 // ^4: brilho concentrado
        return (int) (255f * (0.86f + 0.14f * Math.min(1f, s * 1.5f)));
    }

    public void render(MeshDeform d, MatrixStack matrices, VertexConsumerProvider vcp,
                       Identifier bodyTex, Identifier labelTex, int light, int overlay) {
        MatrixStack.Entry e = matrices.peek();
        List<MeshLayer> use = (d != null && d.active) ? layers : idleMesh().layers;
        for (MeshLayer l : use) {
            Identifier tex = l.slot.equals("label") ? labelTex : bodyTex;
            VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex));
            renderLayer(l, d, vc, e, light, overlay);
        }
    }

    private void renderLayer(MeshLayer l, MeshDeform d, VertexConsumer vc, MatrixStack.Entry e, int light, int overlay) {
        int n = l.x.length;
        float[] px = l.x, py = l.y, pz = l.z;
        float[] nx = l.bwx, ny = l.bwy, nz = l.bwz;
        boolean deforming = d != null && d.active;

        if (deforming) {
            float r = d.r, k = d.press, H = height;
            float inv = 1f / (1.4f * r * r);
            float[] sx = l.sx, sy = l.sy, sz = l.sz;
            for (int i = 0; i < n; i++) {
                float dx = l.x[i] - d.cx, dz = l.z[i] - d.cz;
                float d2 = dx * dx + dz * dz;
                float f = (float) Math.exp(-d2 * inv);              // afundamento embaixo do pe
                float dist = (float) Math.sqrt(d2);
                float uu = dist / (2f * r);
                float g = uu * (float) Math.exp(0.5f * (1f - uu * uu)); // anel de estufamento em volta
                float yy = l.y[i];
                sy[i] = yy * (1f - k * f + 0.22f * k * g);
                float lift = Math.min(1f, yy / (H * 0.5f));
                float push = 0.30f * r * k * g * lift;                 // empurra a massa para fora
                if (dist > 1e-4f) { sx[i] = l.x[i] + dx / dist * push; sz[i] = l.z[i] + dz / dist * push; }
                else { sx[i] = l.x[i]; sz[i] = l.z[i]; }
            }
            px = sx; py = sy; pz = sz;
            l.accumulate(px, py, pz, l.wnx, l.wny, l.wnz);
            nx = l.wnx; ny = l.wny; nz = l.wnz;
        }

        Matrix4f pm = e.getPositionMatrix();
        Matrix3f nm = e.getNormalMatrix();
        float off = l.decal ? DECAL_OFFSET : 0f;
        int[] tri = l.tri;
        for (int t = 0; t < tri.length; t += 3) {
            for (int c = 0; c < 4; c++) {            // triangulo enviado como quad degenerado (v0, v1, v2, v2)
                int i = tri[t + (c > 2 ? 2 : c)];
                int w = l.weld[i];
                float vnx = nx[w], vny = ny[w], vnz = nz[w];
                int col = deforming ? shade(vnx, vny, vnz) : l.restCol[i];
                vc.vertex(pm, px[i] + off * vnx, py[i] + off * vny, pz[i] + off * vnz)
                        .color(col, col, col, 255)
                        .texture(l.u[i], l.v[i])
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(light)
                        .normal(nm, vnx, vny, vnz)
                        .next();
            }
        }
    }
}
