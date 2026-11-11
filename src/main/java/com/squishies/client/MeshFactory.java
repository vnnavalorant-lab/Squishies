package com.squishies.client;

import com.squishies.SquishyType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Monta as malhas: o OBJ do butter e formas macias (superelipsoides) para o resto. */
public final class MeshFactory {
    private MeshFactory() {}

    public static SquishyMesh build(SquishyType t) throws IOException {
        switch (t.meshKey) {
            case "needoh":   return superellipsoid(t, 0.40f, 0.40f, true, 22, 44);
            case "gumdrop":  return superellipsoid(t, 0.75f, 0.85f, false, 16, 44);
            case "dumpling": return superellipsoid(t, 0.60f, 0.90f, false, 16, 48);
            case "butter":   return fromObj(t, new Identifier("squishies", "models/obj/butter.obj"));
            default: throw new IOException("malha desconhecida: " + t.meshKey);
        }
    }

    // ---------------------------------------------------------------- superelipsoide
    private static float sgnpow(float v, float e) {
        return Math.signum(v) * (float) Math.pow(Math.abs(v), e);
    }

    private static SquishyMesh superellipsoid(SquishyType t, float e1, float e2, boolean full, int nLat, int nLon) {
        float rx = t.w / 2f, rz = t.d / 2f;
        float ry = full ? t.h / 2f : t.h;
        float cy = full ? ry : 0f;
        float phiMin = full ? -(float) (Math.PI / 2) : 0f;
        float phiMax = (float) (Math.PI / 2);
        int cols = nLon + 1, rows = nLat + 1;
        MeshLayer l = new MeshLayer("body", false, rows * cols);
        for (int i = 0; i < rows; i++) {
            float phi = phiMin + (phiMax - phiMin) * i / nLat;
            float cp = sgnpow((float) Math.cos(phi), e1), sp = sgnpow((float) Math.sin(phi), e1);
            for (int j = 0; j < cols; j++) {
                float th = (float) (2 * Math.PI * j / nLon);
                float ct = sgnpow((float) Math.cos(th), e2), st = sgnpow((float) Math.sin(th), e2);
                int idx = i * cols + j;
                l.x[idx] = 0.5f + rx * cp * ct;
                l.y[idx] = cy + ry * sp;
                l.z[idx] = 0.5f + rz * cp * st;
                l.u[idx] = 0.5f; l.v[idx] = 0.5f;      // textura de cor lisa
            }
        }
        l.tri = new int[nLat * nLon * 6];
        int p = 0;
        for (int i = 0; i < nLat; i++) {
            for (int j = 0; j < nLon; j++) {
                int a = i * cols + j, b = a + 1, c = a + cols, d = c + 1;
                l.tri[p++] = a; l.tri[p++] = c; l.tri[p++] = b;
                l.tri[p++] = b; l.tri[p++] = c; l.tri[p++] = d;
            }
        }
        l.orientOutward(0.5f, cy, 0.5f);
        SquishyMesh m = new SquishyMesh();
        m.layers.add(l);
        m.finish();
        return m;
    }

    // ---------------------------------------------------------------- OBJ (exportado do Roblox Studio)
    private static final class Group {
        final String name;
        final List<float[]> verts = new ArrayList<>();   // x y z u v nx ny nz
        final Map<String, Integer> map = new HashMap<>();
        final List<Integer> tris = new ArrayList<>();
        Group(String name) { this.name = name; }
    }

    private static SquishyMesh fromObj(SquishyType t, Identifier id) throws IOException {
        Resource res = MinecraftClient.getInstance().getResourceManager().getResource(id)
                .orElseThrow(() -> new IOException("OBJ nao encontrado: " + id));
        List<float[]> V = new ArrayList<>(), VT = new ArrayList<>(), VN = new ArrayList<>();
        List<Group> groups = new ArrayList<>();
        Group cur = null;
        try (InputStream in = res.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\s+");
                switch (p[0]) {
                    case "v":  V.add(new float[]{f(p[1]), f(p[2]), f(p[3])}); break;
                    case "vt": VT.add(new float[]{f(p[1]), f(p[2])}); break;
                    case "vn": VN.add(new float[]{f(p[1]), f(p[2]), f(p[3])}); break;
                    case "g":  cur = new Group(p.length > 1 ? p[1] : "g" + groups.size()); groups.add(cur); break;
                    case "f": {
                        if (cur == null) { cur = new Group("g0"); groups.add(cur); }
                        int[] idx = new int[p.length - 1];
                        for (int i = 1; i < p.length; i++) idx[i - 1] = vertexOf(cur, p[i], V, VT, VN);
                        for (int i = 1; i + 1 < idx.length; i++) {   // triangulacao em leque
                            cur.tris.add(idx[0]); cur.tris.add(idx[i]); cur.tris.add(idx[i + 1]);
                        }
                        break;
                    }
                    default: break;
                }
            }
        }
        // caixa envolvente global -> escala para a largura (eixo X) do tipo, centraliza em XZ, base em y=0
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (float[] v : V) {
            minX = Math.min(minX, v[0]); maxX = Math.max(maxX, v[0]);
            minY = Math.min(minY, v[1]);
            minZ = Math.min(minZ, v[2]); maxZ = Math.max(maxZ, v[2]);
        }
        float scale = t.w / (maxX - minX);
        float cx = (minX + maxX) / 2f, cz = (minZ + maxZ) / 2f;

        SquishyMesh mesh = new SquishyMesh();
        for (int gi = 0; gi < groups.size(); gi++) {
            Group g = groups.get(gi);
            if (g.tris.isEmpty()) continue;
            boolean body = mesh.layers.isEmpty();     // primeiro grupo = corpo; os demais = etiqueta
            int n = g.verts.size();
            MeshLayer l = new MeshLayer(body ? "body" : "label", !body, n);
            l.fnx = new float[n]; l.fny = new float[n]; l.fnz = new float[n];
            for (int i = 0; i < n; i++) {
                float[] v = g.verts.get(i);
                l.x[i] = (v[0] - cx) * scale + 0.5f;
                l.y[i] = (v[1] - minY) * scale;
                l.z[i] = (v[2] - cz) * scale + 0.5f;
                if (body) { l.u[i] = 0.5f; l.v[i] = 0.5f; }   // corpo usa cor lisa
                else { l.u[i] = v[3]; l.v[i] = 1f - v[4]; }
                l.fnx[i] = v[5]; l.fny[i] = v[6]; l.fnz[i] = v[7];
            }
            l.tri = new int[g.tris.size()];
            for (int i = 0; i < l.tri.length; i++) l.tri[i] = g.tris.get(i);
            l.orientByFileNormals();
            mesh.layers.add(l.subdivide());   // 1 nivel: cova mais suave em malha esparsa
        }
        mesh.finish();
        return mesh;
    }

    private static int vertexOf(Group g, String token, List<float[]> V, List<float[]> VT, List<float[]> VN) {
        Integer have = g.map.get(token);
        if (have != null) return have;
        String[] s = token.split("/", -1);
        float[] pos = V.get(Integer.parseInt(s[0]) - 1);
        float u = 0, vv = 0, nx = 0, ny = 1, nz = 0;
        if (s.length > 1 && !s[1].isEmpty()) { float[] t = VT.get(Integer.parseInt(s[1]) - 1); u = t[0]; vv = t[1]; }
        if (s.length > 2 && !s[2].isEmpty()) { float[] n = VN.get(Integer.parseInt(s[2]) - 1); nx = n[0]; ny = n[1]; nz = n[2]; }
        g.verts.add(new float[]{pos[0], pos[1], pos[2], u, vv, nx, ny, nz});
        int idx = g.verts.size() - 1;
        g.map.put(token, idx);
        return idx;
    }

    private static float f(String s) { return Float.parseFloat(s); }
}
