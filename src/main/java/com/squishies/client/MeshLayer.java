package com.squishies.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/** Uma camada de malha (corpo ou etiqueta) com uma textura propria. Coordenadas em espaco do bloco (0..1). */
public class MeshLayer {
    public final String slot;      // "body" ou "label"
    public final boolean decal;    // desloca levemente na normal para nao brigar (z-fighting) com o corpo
    public final float[] x, y, z, u, v;
    public int[] tri;              // 3 indices por triangulo
    float[] fnx, fny, fnz;         // normais vindas do arquivo (so OBJ), usadas para orientar os triangulos

    int[] weld;                    // id da posicao unica de cada vertice (para normais suaves)
    int weldCount;
    float[] bwx, bwy, bwz;         // normais suaves em repouso (por weld)
    float[] sx, sy, sz;            // posicoes deformadas (scratch)
    float[] wnx, wny, wnz;         // normais deformadas (scratch, por weld)

    public MeshLayer(String slot, boolean decal, int n) {
        this.slot = slot; this.decal = decal;
        x = new float[n]; y = new float[n]; z = new float[n]; u = new float[n]; v = new float[n];
    }


    /** Maior aresta (em blocos) entre todos os triangulos da camada. */
    public float maxEdge() {
        float m = 0f;
        for (int t = 0; t < tri.length; t += 3) {
            for (int e = 0; e < 3; e++) {
                int a = tri[t + e], b = tri[t + (e + 1) % 3];
                float dx = x[a] - x[b], dy = y[a] - y[b], dz = z[a] - z[b];
                m = Math.max(m, dx * dx + dy * dy + dz * dz);
            }
        }
        return (float) Math.sqrt(m);
    }

    /** Subdivide cada triangulo em 4 (pontos medios) para a deformacao ficar suave em malhas esparsas. */
    public MeshLayer subdivide() {
        int n = x.length, T = tri.length / 3;
        HashMap<Long, Integer> edge = new HashMap<>();
        List<int[]> mids = new ArrayList<>();
        int[] nt = new int[T * 12];
        int p = 0;
        for (int t = 0; t < tri.length; t += 3) {
            int a = tri[t], b = tri[t + 1], c = tri[t + 2];
            int ab = mid(a, b, n, edge, mids), bc = mid(b, c, n, edge, mids), ca = mid(c, a, n, edge, mids);
            nt[p++] = a;  nt[p++] = ab; nt[p++] = ca;
            nt[p++] = ab; nt[p++] = b;  nt[p++] = bc;
            nt[p++] = ca; nt[p++] = bc; nt[p++] = c;
            nt[p++] = ab; nt[p++] = bc; nt[p++] = ca;
        }
        MeshLayer r = new MeshLayer(slot, decal, n + mids.size());
        System.arraycopy(x, 0, r.x, 0, n); System.arraycopy(y, 0, r.y, 0, n); System.arraycopy(z, 0, r.z, 0, n);
        System.arraycopy(u, 0, r.u, 0, n); System.arraycopy(v, 0, r.v, 0, n);
        for (int i = 0; i < mids.size(); i++) {
            int a = mids.get(i)[0], b = mids.get(i)[1], k = n + i;
            r.x[k] = (x[a] + x[b]) / 2f; r.y[k] = (y[a] + y[b]) / 2f; r.z[k] = (z[a] + z[b]) / 2f;
            r.u[k] = (u[a] + u[b]) / 2f; r.v[k] = (v[a] + v[b]) / 2f;
        }
        r.tri = nt;
        return r;
    }

    private static int mid(int a, int b, int base, HashMap<Long, Integer> edge, List<int[]> mids) {
        long key = ((long) Math.min(a, b) << 32) | (long) Math.max(a, b);
        Integer found = edge.get(key);
        if (found != null) return found;
        int idx = base + mids.size();
        mids.add(new int[]{a, b});
        edge.put(key, idx);
        return idx;
    }

    /** Vira os triangulos cuja normal aponta para dentro, usando a direcao "para fora" a partir de um centro. */
    public void orientOutward(float cx, float cy, float cz) {
        for (int t = 0; t < tri.length; t += 3) {
            int a = tri[t], b = tri[t + 1], c = tri[t + 2];
            float[] n = faceNormal(x, y, z, a, b, c);
            float mx = (x[a] + x[b] + x[c]) / 3f - cx, my = (y[a] + y[b] + y[c]) / 3f - cy, mz = (z[a] + z[b] + z[c]) / 3f - cz;
            if (n[0] * mx + n[1] * my + n[2] * mz < 0) { tri[t + 1] = c; tri[t + 2] = b; }
        }
    }

    /** Vira os triangulos cuja normal discorda das normais do arquivo OBJ. */
    public void orientByFileNormals() {
        if (fnx == null) return;
        for (int t = 0; t < tri.length; t += 3) {
            int a = tri[t], b = tri[t + 1], c = tri[t + 2];
            float[] n = faceNormal(x, y, z, a, b, c);
            float sx2 = fnx[a] + fnx[b] + fnx[c], sy2 = fny[a] + fny[b] + fny[c], sz2 = fnz[a] + fnz[b] + fnz[c];
            if (n[0] * sx2 + n[1] * sy2 + n[2] * sz2 < 0) { tri[t + 1] = c; tri[t + 2] = b; }
        }
    }

    static float[] faceNormal(float[] px, float[] py, float[] pz, int a, int b, int c) {
        float ux = px[b] - px[a], uy = py[b] - py[a], uz = pz[b] - pz[a];
        float vx = px[c] - px[a], vy = py[c] - py[a], vz = pz[c] - pz[a];
        return new float[]{uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx};
    }

    /** Calcula os ids de solda, as normais em repouso e aloca os buffers de trabalho. */
    public void finish() {
        int n = x.length;
        HashMap<String, Integer> map = new HashMap<>();
        weld = new int[n];
        for (int i = 0; i < n; i++) {
            String k = Math.round(x[i] * 10000) + "," + Math.round(y[i] * 10000) + "," + Math.round(z[i] * 10000);
            Integer w = map.get(k);
            if (w == null) { w = map.size(); map.put(k, w); }
            weld[i] = w;
        }
        weldCount = map.size();
        bwx = new float[weldCount]; bwy = new float[weldCount]; bwz = new float[weldCount];
        wnx = new float[weldCount]; wny = new float[weldCount]; wnz = new float[weldCount];
        sx = new float[n]; sy = new float[n]; sz = new float[n];
        accumulate(x, y, z, bwx, bwy, bwz);
        fnx = fny = fnz = null;
    }

    /** Soma as normais das faces em cada posicao unica e normaliza. */
    void accumulate(float[] px, float[] py, float[] pz, float[] nx, float[] ny, float[] nz) {
        Arrays.fill(nx, 0, weldCount, 0f); Arrays.fill(ny, 0, weldCount, 0f); Arrays.fill(nz, 0, weldCount, 0f);
        for (int t = 0; t < tri.length; t += 3) {
            int a = tri[t], b = tri[t + 1], c = tri[t + 2];
            float ux = px[b] - px[a], uy = py[b] - py[a], uz = pz[b] - pz[a];
            float vx = px[c] - px[a], vy = py[c] - py[a], vz = pz[c] - pz[a];
            float cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
            int wa = weld[a], wb = weld[b], wc = weld[c];
            nx[wa] += cx; ny[wa] += cy; nz[wa] += cz;
            nx[wb] += cx; ny[wb] += cy; nz[wb] += cz;
            nx[wc] += cx; ny[wc] += cy; nz[wc] += cz;
        }
        for (int i = 0; i < weldCount; i++) {
            float len = (float) Math.sqrt(nx[i] * nx[i] + ny[i] * ny[i] + nz[i] * nz[i]);
            if (len > 1e-12f) { nx[i] /= len; ny[i] /= len; nz[i] /= len; } else { nx[i] = 0; ny[i] = 1; nz[i] = 0; }
        }
    }
}
