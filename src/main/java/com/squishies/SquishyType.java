package com.squishies;

import net.minecraft.block.Block;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.EnumMap;
import java.util.Map;

/**
 * Dimensoes (em blocos), forma da malha e sons de um tipo de squishy.
 *
 * O modelo ocupa uma grade de cellsX x cellsZ blocos (ate MAX_PARTS no total). Espaco da malha:
 * X em [0, cellsX] (eixo FACING), Z em [0, cellsZ] (para a direita do FACING). O modelo (w x d de area,
 * h de altura) fica centralizado nessa grade; cada bloco da grade recebe so o pedaco da hitbox que lhe cabe.
 */
public class SquishyType {
    public static final int MAX_PARTS = 4;

    public final String id;
    public final String meshKey;      // qual malha o cliente usa: needoh, gumdrop, dumpling, butter ou o nome de um .obj em models/obj
    public final int cellsX, cellsZ;  // blocos da grade (1x1 = bloco unico)
    public final float w, h, d;       // tamanho em repouso, em blocos (w no eixo X da malha, d no eixo Z)
    public final SoundEvent press;
    public final SoundEvent release;  // pode ser null
    public final float pressVolume;

    public int meshRot = 0;           // quartos de volta (em Y) aplicados ao OBJ para o rosto/frente apontar para -X da malha
    public boolean directional = false; // 1x1 que gira conforme o FACING (tem rosto/frente)
    public String sharedLabel = null; // textura de etiqueta/rosto unica para todas as variantes (null = <variante>_label.png)

    private final Map<Direction, VoxelShape[]> outline = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape[]> collision = new EnumMap<>(Direction.class);

    public SquishyType(String id, String meshKey, int cellsX, int cellsZ, float w, float h, float d, float collisionFrac,
                       SoundEvent press, SoundEvent release, float pressVolume) {
        if (cellsX < 1 || cellsZ < 1 || cellsX * cellsZ > MAX_PARTS) throw new IllegalArgumentException("grade invalida");
        this.id = id; this.meshKey = meshKey; this.cellsX = cellsX; this.cellsZ = cellsZ;
        this.w = w; this.h = h; this.d = d;
        this.press = press; this.release = release; this.pressVolume = pressVolume;
        for (Direction f : Direction.Type.HORIZONTAL) {
            VoxelShape[] o = new VoxelShape[cells()], c = new VoxelShape[cells()];
            for (int p = 0; p < cells(); p++) {
                o[p] = cellShape(f, p, h * 16.0);
                c[p] = cellShape(f, p, h * 16.0 * collisionFrac);
            }
            outline.put(f, o);
            collision.put(f, c);
        }
    }

    public SquishyType withMeshRot(int quarterTurns) { this.meshRot = ((quarterTurns % 4) + 4) % 4; return this; }
    public SquishyType withDirectional() { this.directional = true; return this; }
    public SquishyType withSharedLabel(String name) { this.sharedLabel = name; return this; }

    public int cells() { return cellsX * cellsZ; }
    public int masterI() { return cellsX / 2; }
    public int masterJ() { return cellsZ / 2; }
    /** Indice da parte (bloco) que desenha a malha: a mais central da grade. */
    public int masterPart() { return masterI() + masterJ() * cellsX; }

    /** Caixa do pedaco da hitbox que cai no bloco (i, j), ja girada para o FACING. */
    private VoxelShape cellShape(Direction f, int part, double heightPx) {
        int i = part % cellsX, j = part / cellsX;
        double ax0 = Math.max((cellsX - w) / 2.0, i), ax1 = Math.min((cellsX + w) / 2.0, i + 1);
        double az0 = Math.max((cellsZ - d) / 2.0, j), az1 = Math.min((cellsZ + d) / 2.0, j + 1);
        if (ax0 >= ax1 || az0 >= az1) return VoxelShapes.empty();
        // relativo ao centro do bloco, no espaco da malha
        double x0 = ax0 - i - 0.5, x1 = ax1 - i - 0.5, z0 = az0 - j - 0.5, z1 = az1 - j - 0.5;
        double rad = Math.toRadians(270.0 - f.asRotation());
        double c = Math.round(Math.cos(rad)), s = Math.round(Math.sin(rad));   // multiplos de 90 graus: valores exatos
        double wx0 = c * x0 + s * z0, wz0 = -s * x0 + c * z0;
        double wx1 = c * x1 + s * z1, wz1 = -s * x1 + c * z1;
        return Block.createCuboidShape(
                (Math.min(wx0, wx1) + 0.5) * 16.0, 0, (Math.min(wz0, wz1) + 0.5) * 16.0,
                (Math.max(wx0, wx1) + 0.5) * 16.0, heightPx, (Math.max(wz0, wz1) + 0.5) * 16.0);
    }

    // PART vai de 0 a MAX_PARTS-1 em todos os tipos, entao o Minecraft cria estados (e pede a forma deles)
    // para partes que nao existem neste tipo (ex.: tipos 1x1 so tem a parte 0). Nesses casos: forma vazia.
    public VoxelShape outline(Direction f, int part) {
        VoxelShape[] a = outline.get(f);
        return part >= 0 && part < a.length ? a[part] : VoxelShapes.empty();
    }
    public VoxelShape collision(Direction f, int part) {
        VoxelShape[] a = collision.get(f);
        return part >= 0 && part < a.length ? a[part] : VoxelShapes.empty();
    }
}
