# Squishies ASMR (Fabric 1.20.1)

44 squishies que **amassam onde o jogador pisa** (a malha deforma em volta do pe, estufa nas bordas e volta com bounce):
Needoh (6), Gumdrop Needoh (7), Dumpling (13) e Salted Butter (5: yellow, pink, lilac, blue, mint) e Face Dumpling (13 cores).

- Butter: **multibloco 3x1** (3 de comprimento, 1 de largura; coloque virado para a direcao em que voce olha). Usa o `butter.obj` (Roblox) + a etiqueta `Butter2_diff.png`, recolorida por variante.
- Needoh/Gumdrop/Dumpling: formas macias geradas por codigo (superelipsoides), cor lisa. Tamanhos: Needoh 0.70, Gumdrop 0.70x0.67, Dumpling 1.0x0.62.
- Todo modelo passa pelo mesmo sistema de malha maleavel (`MeshFactory` -> `SquishyMesh`). Para adicionar um novo: coloque `models/obj/<nome>.obj` e use `<nome>` como `meshKey` em `SquishiesMod` (1o grupo = corpo, demais = etiqueta); ele ja amassa igual ao butter.
- Modelos originais: @rvn4x (Roblox), uso livre conforme aviso nos scripts.

## Compilar
Suba para o GitHub: `.github/workflows/build.yml` compila e deixa o .jar em Actions > build > Artifacts.
Local: `gradle build` (Gradle 8.8 + Java 17).

## Ajustar
- Tamanhos/colisao/sons: `SquishiesMod.java` (tipos) e `SquishyType.java` (`length` = blocos em fila, max 3; `w` = comprimento total, `d` = largura).
- Maciez: `SquishyRenderer.java` (OMEGA = rigidez, ZETA = quanto balanca) e `SquishyMesh.java` (formula do amassado).
- Trocar o modelo do butter: substitua `assets/squishies/models/obj/butter.obj` (1o grupo = corpo, demais = etiqueta). F3+T recarrega.
- Sons: coloque .ogg em `assets/squishies/sounds/` e edite `sounds.json` (hoje reaproveita sons do Minecraft).

## Desempenho (FPS)
- Cada modelo tem 2 malhas: **densa** (so usada enquanto alguem amassa, deformacao suave) e **leve** (repouso, longe e itens, ~4x menos vertices).
- Fora do cone de visao ou atras da camera: nem desenha. Alem de 48 blocos: nao desenha. Alem de 24 blocos: so a malha leve, sem amassado.
- A busca por entidades em cima do squishy roda no maximo 1x por tick (1x a cada 3 ticks quando esta parado), nao a cada frame.
- Em repouso total (nada em cima e a mola parou) a fisica e o calculo de normais sao pulados; o brilho em repouso e pre-calculado.
- Ajuste: `RENDER_DIST`, `ACTIVE_DIST2` e `CULL_COS` em `SquishyRenderer.java`; `MAX_EDGE` e `MAX_TRIS` em `MeshFactory.java`.

## Face Dumpling (novo)
- Modelo do Roblox (`models/obj/face_dumpling.obj`, grupo 1 = corpo, grupo 2 = rosto) com o rosto em `textures/entity/face_dumpling_face.png` (o mesmo rosto para todas as cores).
- 1,5 bloco de largura (altura ~1,02), **multibloco 2x2**: a malha de 1,5 fica centralizada na grade e cada bloco ganha so o pedaco da hitbox que lhe cabe. O rosto aponta para quem colocou o bloco.
- 13 cores (white, brown, black, gray, navy, pink, purple, blue, green, lime, red, yellow, orange): so mudam a textura lisa do corpo.
- Para adicionar mais cores: crie `textures/entity/face_dumpling_<cor>.png` (16x16 lisa), os arquivos de blockstate/model/item/loot (copie de uma cor existente) e uma linha em `SquishiesMod.BLOCKS`.
- Grades multibloco: `SquishyType(cellsX, cellsZ, ...)`, ate 4 blocos no total.
