package com.squishies;

import net.minecraft.block.Block;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.shape.VoxelShape;

/** Dimensoes (em blocos), forma da malha e sons de um tipo de squishy. */
public class SquishyType {
    public final String id;
    public final String meshKey;      // qual malha o cliente usa: needoh, gumdrop, dumpling, butter
    public final float w, h, d;       // tamanho em repouso, em blocos
    public final SoundEvent press;
    public final SoundEvent release;  // pode ser null
    public final float pressVolume;
    private final VoxelShape outline;
    private final VoxelShape collision;

    public SquishyType(String id, String meshKey, float w, float h, float d, float collisionFrac,
                       SoundEvent press, SoundEvent release, float pressVolume) {
        this.id = id; this.meshKey = meshKey; this.w = w; this.h = h; this.d = d;
        this.press = press; this.release = release; this.pressVolume = pressVolume;
        double x0 = (1 - w) / 2.0 * 16, x1 = (1 + w) / 2.0 * 16;
        double z0 = (1 - d) / 2.0 * 16, z1 = (1 + d) / 2.0 * 16;
        this.outline = Block.createCuboidShape(x0, 0, z0, x1, h * 16.0, z1);
        // colisao mais baixa: o jogador afunda no squishy e a malha amassa ate os pes dele
        this.collision = Block.createCuboidShape(x0, 0, z0, x1, h * 16.0 * collisionFrac, z1);
    }

    public VoxelShape outline() { return outline; }
    public VoxelShape collision() { return collision; }
}
