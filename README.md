# Squishies ASMR (Fabric 1.20.1)

32 blocos/itens que achatam quando um mob/jogador pisa e voltam com bounce:
Needoh (6), Gumdrop Needoh (7), Butter Slab (2), Butter (2), Crunchy Butter (2), Dumpling (13).
Tamanhos, cores e tempos vieram do squishies.rbxl. Modelos originais: @rvn4x (Roblox), uso livre segundo o aviso nos scripts.

## Compilar
No GitHub: envie o projeto para um repositorio; o workflow `.github/workflows/build.yml` compila sozinho e o .jar fica em Actions > build > Artifacts (nao precisa do wrapper).

Localmente:
1. Copie `gradlew`, `gradlew.bat` e `gradle/wrapper/gradle-wrapper.jar` de qualquer outro mod Fabric seu para esta pasta.
2. `./gradlew build` -> o .jar fica em `build/libs/`.

## Trocar modelos e sons por os reais
- Modelos: `assets/squishies/models/block/<id>_s0..s6.json` (s0 = parado, s3 = apertado, s4-s6 = bounce). Para usar um .obj, aponte o s0 para ele e ajuste os outros.
- Sons: coloque .ogg em `assets/squishies/sounds/` e edite `assets/squishies/sounds.json` (hoje cada evento reutiliza sons do Minecraft).
