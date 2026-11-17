package com.squishies.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Formas procedurais dos squishies "de comida/brinquedo": donut, sorvete de casquinha, mochi com rosto,
 * fatia de pizza e pop-it. Cada forma e uma lista de MeshLayer (uma por parte colorida) que entra no mesmo
 * sistema de malha maleavel dos outros squishies.
 *
 * Espaco da malha (antes do ajuste de tamanho): grade 1x1, centro em (0.5, 0.5), base em y = 0,
 * frente do modelo apontando para -X (igual ao OBJ girado dos outros tipos directional).
 *
 * Textura de cada variante: 64x16 px.
 *   - x 0..15  : paleta 4x4 de celulas de 4 px (cor k = celula k, ver pal())
 *   - x 16..63 : faixa de 48 px (gradiente, listras ou padrao de waffle, ver stripU())
 * Cada camada usa UVs fixos (cor lisa) ou a faixa, entao a forma e igual em todas as variantes e so a textura muda.
 *
 * Esta classe NAO depende do Minecraft (so de MeshLayer), para poder ser testada fora do jogo.
 */
public final class ShapeMeshes {
    private ShapeMeshes() {}

    /** q = 1: malha densa (usada enquanto amassa); q = 0: malha leve (repouso/longe/itens). */
    public interface Gen { List<MeshLayer> make(int q); }

    interface P { void at(float a, float b, float[] o); }      // parametros 0..1 -> x y z
    interface Q { void at(float a, float b, float[] o); }      // parametros 0..1 -> u v
    interface Ref { void at(float x, float y, float z, float[] o); }   // ponto "de dentro" para orientar as faces

    private static final float TW = 64f, TH = 16f;
    private static final float TAU = (float) (Math.PI * 2);

    // ------------------------------------------------------------------ textura
    /** UV do centro da celula k (0..15) da paleta 4x4. */
    static float[] pal(int k) {
        return new float[]{((k % 4) * 4 + 2f) / TW, ((k / 4) * 4 + 2f) / TH};
    }

    /** U na faixa de 48 px (t = 0..1). Evita as bordas da faixa para a textura nao "dar a volta". */
    static float stripU(float t) {
        t = Math.min(1f, Math.max(0f, t));
        return (16.02f + 47.96f * t) / TW;
    }

    static float vClamp(float v) { return Math.min(0.97f, Math.max(0.03f, v)); }

    // ------------------------------------------------------------------ utilidades
    static float sgnpow(float v, float e) { return Math.signum(v) * (float) Math.pow(Math.abs(v), e); }
    static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Acumula varios "retalhos" (grades parametricas) numa unica camada. */
    static final class Acc {
        final List<float[]> v = new ArrayList<>();   // x y z u v
        final List<Integer> tri = new ArrayList<>();

        /** Grade (rows+1) x (cols+1). a = linha (0..1), b = coluna (0..1). */
        void patch(int rows, int cols, P fn, Q uv, Ref ref) {
            int base = v.size();
            float[] p = new float[3], t = new float[2];
            for (int i = 0; i <= rows; i++) {
                for (int j = 0; j <= cols; j++) {
                    float a = (float) i / rows, b = (float) j / cols;
                    fn.at(a, b, p);
                    uv.at(a, b, t);
                    v.add(new float[]{p[0], p[1], p[2], t[0], t[1]});
                }
            }
            int stride = cols + 1;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int a = base + i * stride + j, b = a + 1, c = a + stride, d = c + 1;
                    addTri(a, c, b, ref);
                    addTri(b, c, d, ref);
                }
            }
        }

        /** Quadrilatero solto (4 cantos em ordem), cor lisa. */
        void quad(float[] p0, float[] p1, float[] p2, float[] p3, float[] uv, Ref ref) {
            int base = v.size();
            for (float[] p : new float[][]{p0, p1, p2, p3}) v.add(new float[]{p[0], p[1], p[2], uv[0], uv[1]});
            addTri(base, base + 1, base + 2, ref);
            addTri(base, base + 2, base + 3, ref);
        }

        private void addTri(int a, int b, int c, Ref ref) {
            float[] A = v.get(a), B = v.get(b), C = v.get(c);
            if (ref != null) {
                float ux = B[0] - A[0], uy = B[1] - A[1], uz = B[2] - A[2];
                float wx = C[0] - A[0], wy = C[1] - A[1], wz = C[2] - A[2];
                float nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
                float mx = (A[0] + B[0] + C[0]) / 3f, my = (A[1] + B[1] + C[1]) / 3f, mz = (A[2] + B[2] + C[2]) / 3f;
                float[] r = new float[3];
                ref.at(mx, my, mz, r);
                if (nx * (mx - r[0]) + ny * (my - r[1]) + nz * (mz - r[2]) < 0) { int t = b; b = c; c = t; }
            }
            tri.add(a); tri.add(b); tri.add(c);
        }

        MeshLayer toLayer(String slot, boolean decal) {
            int n = v.size();
            MeshLayer l = new MeshLayer(slot, decal, n);
            for (int i = 0; i < n; i++) {
                float[] p = v.get(i);
                l.x[i] = p[0]; l.y[i] = p[1]; l.z[i] = p[2]; l.u[i] = p[3]; l.v[i] = p[4];
            }
            l.tri = new int[tri.size()];
            for (int i = 0; i < l.tri.length; i++) l.tri[i] = tri.get(i);
            return l;
        }
    }

    private static Q color(int k) {
        final float[] c = pal(k);
        return (a, b, o) -> { o[0] = c[0]; o[1] = c[1]; };
    }

    private static Ref point(final float x, final float y, final float z) {
        return (px, py, pz, o) -> { o[0] = x; o[1] = y; o[2] = z; };
    }

    // ================================================================== DONUT
    // Rosquinha: massa (toro) + cobertura com borda ondulada + granulado colorido.
    private static final float D_R = 0.29f, D_RT = 0.17f, D_RY = 0.20f;   // raio do anel, raio horizontal e vertical do tubo
    private static final float D_ICE = 1.035f;                            // cobertura um pouco maior que a massa

    private static void torusPt(float u, float v, float k, float[] o) {
        float rad = D_R + k * D_RT * (float) Math.cos(v);
        o[0] = 0.5f + rad * (float) Math.cos(u);
        o[1] = D_RY + k * D_RY * (float) Math.sin(v);
        o[2] = 0.5f + rad * (float) Math.sin(u);
    }

    private static final Ref DONUT_REF = (x, y, z, o) -> {
        float dx = x - 0.5f, dz = z - 0.5f, len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-5f) { o[0] = 0.5f; o[1] = D_RY; o[2] = 0.5f; return; }
        o[0] = 0.5f + D_R * dx / len; o[1] = D_RY; o[2] = 0.5f + D_R * dz / len;
    };

    private static float iceV0(float u) { return -0.18f + 0.10f * (float) Math.sin(6 * u); }
    private static float iceV1(float u) { return (float) Math.PI + 0.18f + 0.10f * (float) Math.sin(6 * u + 2.0f); }

    public static List<MeshLayer> donut(int q) {
        int nu = q == 1 ? 56 : 32, nv = q == 1 ? 26 : 14;
        List<MeshLayer> out = new ArrayList<>();

        Acc dough = new Acc();
        dough.patch(nv, nu, (a, b, o) -> torusPt(TAU * b, TAU * a, 1f, o), color(0), DONUT_REF);
        out.add(dough.toLayer("body", false));

        Acc ice = new Acc();
        ice.patch(nv, nu, (a, b, o) -> {
            float u = TAU * b;
            torusPt(u, lerp(iceV0(u), iceV1(u), a), D_ICE, o);
        }, color(1), DONUT_REF);
        out.add(ice.toLayer("body", false));

        // granulado: tracinhos achatados em cima da cobertura
        Acc spr = new Acc();
        Random rnd = new Random(7);
        int count = q == 1 ? 30 : 22;
        for (int k = 0; k < count; k++) {
            float u = TAU * rnd.nextFloat();
            float a = 0.16f + 0.68f * rnd.nextFloat();
            float v = lerp(iceV0(u), iceV1(u), a);
            float[] p = new float[3];
            torusPt(u, v, D_ICE, p);
            float cu = (float) Math.cos(u), su = (float) Math.sin(u), cv = (float) Math.cos(v), sv = (float) Math.sin(v);
            float[] n = norm(cv * cu / D_RT, sv / D_RY, cv * su / D_RT);
            float[] eu = {-su, 0f, cu};
            float[] ev = norm(-sv * cu * D_RT, D_RY * cv, -sv * su * D_RT);
            float psi = TAU * rnd.nextFloat();
            float[] dir = norm(eu[0] * (float) Math.cos(psi) + ev[0] * (float) Math.sin(psi),
                    eu[1] * (float) Math.cos(psi) + ev[1] * (float) Math.sin(psi),
                    eu[2] * (float) Math.cos(psi) + ev[2] * (float) Math.sin(psi));
            float[] side = norm(n[1] * dir[2] - n[2] * dir[1], n[2] * dir[0] - n[0] * dir[2], n[0] * dir[1] - n[1] * dir[0]);
            float hl = 0.036f, hw = 0.012f, lift = 0.0045f;
            float[] c = {p[0] + n[0] * lift, p[1] + n[1] * lift, p[2] + n[2] * lift};
            spr.quad(corner(c, dir, side, -hl, -hw), corner(c, dir, side, hl, -hw),
                    corner(c, dir, side, hl, hw), corner(c, dir, side, -hl, hw), pal(2 + rnd.nextInt(5)), DONUT_REF);
        }
        out.add(spr.toLayer("body", false));
        return out;
    }

    private static float[] corner(float[] c, float[] d, float[] s, float a, float b) {
        return new float[]{c[0] + d[0] * a + s[0] * b, c[1] + d[1] * a + s[1] * b, c[2] + d[2] * a + s[2] * b};
    }

    private static float[] norm(float x, float y, float z) {
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        if (l < 1e-9f) return new float[]{0f, 1f, 0f};
        return new float[]{x / l, y / l, z / l};
    }

    // ================================================================== SORVETE DE CASQUINHA
    private static final float C_H = 0.46f, C_R0 = 0.07f, C_RT = 0.295f;   // altura, raio da ponta e do topo da casquinha
    private static final float C_CREAM_Y = C_H - 0.03f;

    public static List<MeshLayer> icecream(int q) {
        int nth = q == 1 ? 56 : 32;
        List<MeshLayer> out = new ArrayList<>();

        // casquinha (ponta para baixo): tampa da ponta + lateral. UV na faixa (waffle) por angulo/altura
        final int sideSteps = q == 1 ? 10 : 5;
        final int n = sideSteps + 2;                       // pontos do perfil
        final float[] py = new float[n], pr = new float[n];
        py[0] = 0f; pr[0] = 0f;
        py[1] = 0f; pr[1] = C_R0;
        for (int i = 0; i <= sideSteps - 0; i++) {
            if (i == 0) continue;
            float t = (float) i / sideSteps;
            if (1 + i < n) { py[1 + i] = C_H * t; pr[1 + i] = lerp(C_R0, C_RT, t); }
        }
        Acc cone = new Acc();
        cone.patch(n - 1, nth, (a, b, o) -> {
            int i = Math.round(a * (n - 1));
            float th = TAU * b;
            o[0] = 0.5f + pr[i] * (float) Math.cos(th); o[1] = py[i]; o[2] = 0.5f + pr[i] * (float) Math.sin(th);
        }, (a, b, o) -> {
            int i = Math.round(a * (n - 1));
            o[0] = stripU(b); o[1] = vClamp(py[i] / C_H);
        }, (x, y, z, o) -> {
            if (y < 0.002f) { o[0] = x; o[1] = y + 1f; o[2] = z; }
            else { o[0] = 0.5f; o[1] = y; o[2] = 0.5f; }
        });
        out.add(cone.toLayer("body", false));

        // bola de sorvete em espiral (3 "voltas"), perfil de revolucao com ondulacao
        final int cap = 3, rows = q == 1 ? 34 : 18;
        final int m = cap + rows;
        final float[] cy = new float[m], cr = new float[m];
        cy[0] = C_H - 0.04f; cr[0] = 0f;
        cy[1] = C_H - 0.04f; cr[1] = 0.20f;
        cy[2] = C_H - 0.04f; cr[2] = 0.30f;
        for (int i = 0; i < rows; i++) {
            float t = (float) i / (rows - 1);
            cy[cap + i] = C_CREAM_Y + 0.50f * t;
            float env = 0.335f * (float) Math.sqrt(Math.max(0.0, 1.0 - Math.pow(t, 1.8)));
            float tier = 1f - 0.045f * (1f - (float) Math.cos(TAU * 2.5f * t));
            cr[cap + i] = env * tier;
        }
        final float spiral = 0.045f;
        Acc cream = new Acc();
        cream.patch(m - 1, nth, (a, b, o) -> {
            int i = Math.round(a * (m - 1));
            float th = TAU * b;
            float t = Math.max(0f, (cy[i] - C_CREAM_Y) / 0.50f);
            float r = cr[i] * (1f + spiral * (float) Math.sin(3 * th - 16f * t));
            o[0] = 0.5f + r * (float) Math.cos(th); o[1] = cy[i]; o[2] = 0.5f + r * (float) Math.sin(th);
        }, color(0), (x, y, z, o) -> {
            if (y < C_H - 0.034f) { o[0] = x; o[1] = y + 1f; o[2] = z; }
            else { o[0] = 0.5f; o[1] = y; o[2] = 0.5f; }
        });
        out.add(cream.toLayer("body", false));
        return out;
    }

    // ================================================================== MOCHI COM ROSTO (em bandeja de madeira)
    private static final float M_RX = 0.42f, M_RY = 0.34f, M_Y0 = 0.07f;
    private static final float M_PHI0 = 0.18f, M_PHI1 = 1.10f, M_TH_HALF = 0.62f;   // area do rosto

    private static void domePt(float phi, float th, float k, float[] o) {
        float cp = sgnpow((float) Math.cos(phi), 0.75f), sp = sgnpow((float) Math.sin(phi), 0.75f);
        o[0] = 0.5f + M_RX * k * cp * (float) Math.cos(th);
        o[1] = M_Y0 + M_RY * k * sp;
        o[2] = 0.5f + M_RX * k * cp * (float) Math.sin(th);
    }

    public static List<MeshLayer> mochi(int q) {
        int nlat = q == 1 ? 26 : 14, nlon = q == 1 ? 56 : 32;
        List<MeshLayer> out = new ArrayList<>();

        // bandeja de madeira: perfil de revolucao (raio, altura)
        final float[][] prof = {{0f, 0f}, {0.38f, 0f}, {0.43f, 0.012f}, {0.46f, 0.04f}, {0.455f, 0.075f},
                {0.43f, 0.095f}, {0.38f, 0.095f}, {0.30f, 0.085f}, {0f, 0.085f}};
        Acc tray = new Acc();
        tray.patch(prof.length - 1, q == 1 ? 56 : 32, (a, b, o) -> {
            int i = Math.round(a * (prof.length - 1));
            float th = TAU * b;
            o[0] = 0.5f + prof[i][0] * (float) Math.cos(th); o[1] = prof[i][1]; o[2] = 0.5f + prof[i][0] * (float) Math.sin(th);
        }, color(1), point(0.5f, 0.045f, 0.5f));
        out.add(tray.toLayer("body", false));

        // cupula: UV da faixa pelo angulo -> degrade conico (arco-iris) no topo, igual ao squishy de referencia
        Acc dome = new Acc();
        dome.patch(nlat, nlon, (a, b, o) -> domePt(a * (float) (Math.PI / 2), TAU * b, 1f, o),
                (a, b, o) -> { o[0] = stripU(b); o[1] = 0.5f; }, point(0.5f, M_Y0, 0.5f));
        out.add(dome.toLayer("body", false));

        // rosto (textura mochi_face.png): remendo na frente (-X) da cupula, um pouco acima da superficie
        Acc face = new Acc();
        float th0 = (float) Math.PI + M_TH_HALF, th1 = (float) Math.PI - M_TH_HALF;   // esquerda -> direita de quem olha
        face.patch(q == 1 ? 18 : 10, q == 1 ? 26 : 14, (a, b, o) -> domePt(lerp(M_PHI1, M_PHI0, a), lerp(th0, th1, b), 1.008f, o),
                (a, b, o) -> { o[0] = b; o[1] = a; }, point(0.5f, M_Y0, 0.5f));
        out.add(face.toLayer("label", true));
        return out;
    }

    // ================================================================== FATIA DE PIZZA
    private static final float Z_X0 = 0.02f, Z_XC = 0.875f, Z_RC = 0.105f, Z_D = 0.74f, Z_B = 0.045f;

    public static List<MeshLayer> pizza(int q) {
        List<MeshLayer> out = new ArrayList<>();
        final float cz = 0.5f, len = Z_XC - Z_X0;

        // queijo: secao em superelipse que cresce da ponta ate a borda
        Acc cheese = new Acc();
        cheese.patch(q == 1 ? 26 : 12, q == 1 ? 28 : 14, (a, b, o) -> {
            float s = a, phi = TAU * b;
            float k = Math.min(1f, (float) Math.sqrt(s / 0.05f));
            float half = 0.5f * Z_D * 0.93f * (0.04f + 0.96f * s) * k;
            float bb = Z_B * k;
            o[0] = Z_X0 + len * s;
            o[1] = Z_B + bb * sgnpow((float) Math.sin(phi), 0.55f);
            o[2] = cz + half * sgnpow((float) Math.cos(phi), 0.45f);
        }, color(0), (x, y, z, o) -> { o[0] = x; o[1] = Z_B; o[2] = cz; });
        out.add(cheese.toLayer("body", false));

        // borda (cilindro arredondado ao longo de Z)
        final float yc = Z_RC;
        Acc crust = new Acc();
        crust.patch(q == 1 ? 18 : 10, q == 1 ? 28 : 14, (a, b, o) -> {
            float phi = (a - 0.5f) * (float) Math.PI, th = TAU * b;
            float cp = sgnpow((float) Math.cos(phi), 0.6f);
            o[0] = Z_XC + Z_RC * cp * (float) Math.cos(th);
            o[1] = yc + Z_RC * cp * (float) Math.sin(th);
            o[2] = cz + 0.5f * Z_D * sgnpow((float) Math.sin(phi), 0.3f);
        }, color(1), point(Z_XC, yc, cz));
        out.add(crust.toLayer("body", false));

        // pepperoni: discos achatados sobre o queijo
        final float[][] pep = {{0.40f, 0f, 0.075f}, {0.70f, 0.14f, 0.070f}, {0.70f, -0.14f, 0.070f}, {0.19f, 0.0f, 0.040f}};
        Acc pp = new Acc();
        for (float[] d : pep) {
            final float px = Z_X0 + len * d[0], pz = cz + d[1], r = d[2], py = 2 * Z_B + 0.003f, ry = 0.018f;
            pp.patch(q == 1 ? 8 : 6, q == 1 ? 18 : 10, (a, b, o) -> {
                float phi = (a - 0.5f) * (float) Math.PI, th = TAU * b;
                float cp = sgnpow((float) Math.cos(phi), 0.45f), sp = sgnpow((float) Math.sin(phi), 0.7f);
                o[0] = px + r * cp * (float) Math.cos(th); o[1] = py + ry * sp; o[2] = pz + r * cp * (float) Math.sin(th);
            }, color(2), point(px, py, pz));
        }
        out.add(pp.toLayer("body", false));
        return out;
    }

    // ================================================================== POP-IT (bolhas coloridas)
    private static final int PI_N = 6;
    private static final float PI_X0 = 0.02f, PI_P = 0.16f, PI_BASE = 0.07f, PI_HB = 0.055f, PI_R = 0.068f;

    private static float popY(float x, float z) {
        int ix = Math.min(PI_N - 1, Math.max(0, (int) ((x - PI_X0) / PI_P)));
        int iz = Math.min(PI_N - 1, Math.max(0, (int) ((z - PI_X0) / PI_P)));
        float dx = x - (PI_X0 + (ix + 0.5f) * PI_P), dz = z - (PI_X0 + (iz + 0.5f) * PI_P);
        float d = (float) Math.sqrt(dx * dx + dz * dz) / PI_R;
        if (d >= 1f) return PI_BASE;
        return PI_BASE + PI_HB * (float) Math.pow(1f - d * d, 0.6);
    }

    public static List<MeshLayer> popit(int q) {
        int res = q == 1 ? 10 : 6;
        float size = PI_N * PI_P, x1 = PI_X0 + size;
        List<MeshLayer> out = new ArrayList<>();
        for (int r = 0; r < PI_N; r++) {
            final float z0 = PI_X0 + r * PI_P, z1 = z0 + PI_P;
            final float tcol = (r + 0.5f) / PI_N;
            Q uv = (a, b, o) -> { o[0] = stripU(tcol); o[1] = 0.5f; };
            Acc acc = new Acc();
            // topo com bolhas
            acc.patch(res, PI_N * res, (a, b, o) -> {
                float x = PI_X0 + size * b, z = z0 + PI_P * a;
                o[0] = x; o[1] = popY(x, z); o[2] = z;
            }, uv, (x, y, z, o) -> { o[0] = x; o[1] = y - 1f; o[2] = z; });
            // fundo
            acc.patch(1, 4, (a, b, o) -> { o[0] = PI_X0 + size * b; o[1] = 0f; o[2] = lerp(z0, z1, a); },
                    uv, (x, y, z, o) -> { o[0] = x; o[1] = y + 1f; o[2] = z; });
            // paredes das pontas (esquerda e direita da fileira)
            acc.patch(1, 2, (a, b, o) -> { o[0] = PI_X0; o[1] = PI_BASE * (1f - a); o[2] = lerp(z0, z1, b); },
                    uv, (x, y, z, o) -> { o[0] = x + 1f; o[1] = y; o[2] = z; });
            acc.patch(1, 2, (a, b, o) -> { o[0] = x1; o[1] = PI_BASE * (1f - a); o[2] = lerp(z0, z1, b); },
                    uv, (x, y, z, o) -> { o[0] = x - 1f; o[1] = y; o[2] = z; });
            if (r == 0) {
                acc.patch(1, 4, (a, b, o) -> { o[0] = PI_X0 + size * b; o[1] = PI_BASE * (1f - a); o[2] = z0; },
                        uv, (x, y, z, o) -> { o[0] = x; o[1] = y; o[2] = z + 1f; });
            }
            if (r == PI_N - 1) {
                acc.patch(1, 4, (a, b, o) -> { o[0] = PI_X0 + size * b; o[1] = PI_BASE * (1f - a); o[2] = z1; },
                        uv, (x, y, z, o) -> { o[0] = x; o[1] = y; o[2] = z - 1f; });
            }
            out.add(acc.toLayer("body", false));
        }
        return out;
    }
}
