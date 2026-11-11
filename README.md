# Squishies ASMR (Fabric 1.20.1)

31 squishies que **amassam onde o jogador pisa** (a malha deforma em volta do pe, estufa nas bordas e volta com bounce):
Needoh (6), Gumdrop Needoh (7), Dumpling (13) e Salted Butter (5: yellow, pink, lilac, blue, mint).

- Butter: usa o `butter.obj` (Roblox) + a etiqueta `Butter2_diff.png`, recolorida por variante.
- Needoh/Gumdrop/Dumpling: formas macias geradas por codigo (superelipsoides), cor lisa.
- Modelos originais: @rvn4x (Roblox), uso livre conforme aviso nos scripts.

## Compilar
Suba para o GitHub: `.github/workflows/build.yml` compila e deixa o .jar em Actions > build > Artifacts.
Local: `gradle build` (Gradle 8.8 + Java 17).

## Ajustar
- Tamanhos/colisao/sons: `SquishiesMod.java` (tipos) e `SquishyType.java`.
- Maciez: `SquishyRenderer.java` (OMEGA = rigidez, ZETA = quanto balanca) e `SquishyMesh.java` (formula do amassado).
- Trocar o modelo do butter: substitua `assets/squishies/models/obj/butter.obj` (1o grupo = corpo, demais = etiqueta). F3+T recarrega.
- Sons: coloque .ogg em `assets/squishies/sounds/` e edite `sounds.json` (hoje reaproveita sons do Minecraft).
