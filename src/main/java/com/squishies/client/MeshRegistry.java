package com.squishies.client;

import com.squishies.SquishiesMod;
import com.squishies.SquishyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Cache das malhas por tipo (so a forma; cada variante troca apenas a textura). */
public final class MeshRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(SquishiesMod.MOD_ID);
    private static final Map<String, SquishyMesh> CACHE = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();

    private MeshRegistry() {}

    public static SquishyMesh get(SquishyType t) {
        SquishyMesh m = CACHE.get(t.meshKey);
        if (m != null) return m;
        if (FAILED.contains(t.meshKey)) return null;
        try {
            m = MeshFactory.build(t);
            CACHE.put(t.meshKey, m);
            return m;
        } catch (Exception ex) {
            LOG.error("Falha ao montar a malha '{}'", t.meshKey, ex);
            FAILED.add(t.meshKey);
            return null;
        }
    }

    public static void clear() { CACHE.clear(); FAILED.clear(); }
}
