package com.squishies;

import net.minecraft.block.Block;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.shape.VoxelShape;

/**
 * Dimensoes (em blocos), forma da malha e sons de um tipo de squishy.
 *
 * length = quantos blocos o modelo ocupa em fila (1 a 3). Quando length > 1, o modelo e um multibloco:
 * w = comprimento total no eixo da fila (ex.: 3.0), d = largura (ex.: 1.0) e cada bloco da fila e uma celula cheia.
 */
public class SquishyType {
    public static final int MAX_LENGTH = 3;

    public final String id;
    public final String meshKey;      // qual malha o cliente usa: needoh, gumdrop, dumpling, butter (ou o nome de um .obj em models/obj)
    public final int length;          // blocos em fila (1 = bloco unico)
    public final float w, h, d;       // tamanho em repouso, em blocos
    public final SoundEvent press;
    public final SoundEvent release;  // pode ser null
    public final float pressVolume;
    private final VoxelShape outline;
    private final VoxelShape collision;

    public SquishyType(String id, String meshKey, int length, float w, float h, float d, float collisionFrac,
                       SoundEvent press, SoundEvent release, float pressVolume) {
        if (length < 1 || length > MAX_LENGTH) throw new IllegalArgumentException("length deve ser 1.." + MAX_LENGTH);
        this.id = id; this.meshKey = meshKey; this.length = length; this.w = w; this.h = h; this.d = d;
        this.press = press; this.release = release; this.pressVolume = pressVolume;
        double x0 = 0, x1 = 16, z0 = 0, z1 = 16;
        if (length == 1) {   // bloco unico: caixa centralizada; multibloco: celula inteira
            x0 = (1 - w) / 2.0 * 16; x1 = (1 + w) / 2.0 * 16;
            z0 = (1 - d) / 2.0 * 16; z1 = (1 + d) / 2.0 * 16;
        }
        this.outline = Block.createCuboidShape(x0, 0, z0, x1, h * 16.0, z1);
        // colisao mais baixa: o jogador afunda no squishy e a malha amassa ate os pes dele
        this.collision = Block.createCuboidShape(x0, 0, z0, x1, h * 16.0 * collisionFrac, z1);
    }

    /** Indice da parte que desenha a malha (a do meio da fila). */
    public int masterPart() { return length / 2; }

    public VoxelShape outline() { return outline; }
    public VoxelShape collision() { return collision; }
}
