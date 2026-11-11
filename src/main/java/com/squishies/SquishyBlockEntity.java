package com.squishies;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** Servidor: guarda se esta apertado (para os sons). Cliente: guarda a mola da animacao de amassar. */
public class SquishyBlockEntity extends BlockEntity {
    public boolean pressed;                  // servidor

    // animacao (so usado no cliente)
    public float press, vel;                 // posicao e velocidade da mola (0 = repouso, 1 = amassado ate o chao)
    public float cx = 0.5f, cz = 0.5f;       // centro do amassado, em coordenadas locais do bloco
    public float radius = 0.3f;
    public double lastTime = Double.NaN;
    public boolean hadContact;

    public SquishyBlockEntity(BlockPos pos, BlockState state) {
        super(SquishiesMod.SQUISHY_BE, pos, state);
    }
}
