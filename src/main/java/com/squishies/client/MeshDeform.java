package com.squishies.client;

/** Estado do amassado aplicado na malha neste frame. */
public class MeshDeform {
    public boolean active;
    public float press;   // k: 0 = repouso, 1 = amassado ate o chao (pode ficar negativo = estufa para cima)
    public float cx, cz;  // centro do amassado (espaco do bloco)
    public float r;       // raio da pisada em blocos
}
