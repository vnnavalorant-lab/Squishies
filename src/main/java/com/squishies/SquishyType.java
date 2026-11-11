package com.squishies;

import net.minecraft.block.Block;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.shape.VoxelShape;

/** Tamanho (em pixels) e sons de um tipo de squishy. As escalas por stage espelham os tweens do script Roblox. */
public class SquishyType {
    static final float[] XZ = {1.0f, 1.04f, 1.08f, 1.12f, 0.97f, 1.02f, 1.0f};
    public final String id;
    final float w, h, d, minY;
    public final SoundEvent press;
    public final SoundEvent release; // pode ser null (dumpling so toca ao pisar)
    public final float pressVolume;
    private final VoxelShape[] shapes = new VoxelShape[7];

    public SquishyType(String id, float w, float h, float d, float minY, SoundEvent press, SoundEvent release, float pressVolume) {
        this.id = id; this.w = w; this.h = h; this.d = d; this.minY = minY;
        this.press = press; this.release = release; this.pressVolume = pressVolume;
        for (int s = 0; s < 7; s++) {
            float sx = Math.min(16f, w * XZ[s]);
            float sz = Math.min(16f, d * XZ[s]);
            float sy = h * yScale(s);
            double x0 = (16 - sx) / 2.0, z0 = (16 - sz) / 2.0;
            shapes[s] = Block.createCuboidShape(x0, 0, z0, x0 + sx, sy, z0 + sz);
        }
    }

    float yScale(int stage) {
        switch (stage) {
            case 0: return 1f;
            case 1: return 1f - (1f - minY) * 0.45f;
            case 2: return 1f - (1f - minY) * 0.80f;
            case 3: return minY;
            case 4: return 1.15f;
            case 5: return 0.94f;
            default: return 1.03f;
        }
    }

    public VoxelShape shape(int stage) { return shapes[stage]; }
}
