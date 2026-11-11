package com.squishies;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SquishiesMod implements ModInitializer {
    public static final String MOD_ID = "squishies";
    public static final List<SquishyBlock> SQUISHY_BLOCKS = new ArrayList<>();
    public static BlockEntityType<SquishyBlockEntity> SQUISHY_BE;
    private static final Map<String, SquishyType> TYPES = new HashMap<>();
    private static final String[][] BLOCKS = {
        {"needoh_red", "needoh"},
        {"needoh_blue", "needoh"},
        {"needoh_green", "needoh"},
        {"needoh_pink", "needoh"},
        {"needoh_purple", "needoh"},
        {"needoh_yellow", "needoh"},
        {"gumdrop_indigo", "gumdrop"},
        {"gumdrop_violet", "gumdrop"},
        {"gumdrop_blue", "gumdrop"},
        {"gumdrop_green", "gumdrop"},
        {"gumdrop_yellow", "gumdrop"},
        {"gumdrop_orange", "gumdrop"},
        {"gumdrop_red", "gumdrop"},
        {"dumpling_white", "dumpling"},
        {"dumpling_brown", "dumpling"},
        {"dumpling_black", "dumpling"},
        {"dumpling_gray", "dumpling"},
        {"dumpling_navy", "dumpling"},
        {"dumpling_pink", "dumpling"},
        {"dumpling_purple", "dumpling"},
        {"dumpling_blue", "dumpling"},
        {"dumpling_green", "dumpling"},
        {"dumpling_lime", "dumpling"},
        {"dumpling_red", "dumpling"},
        {"dumpling_yellow", "dumpling"},
        {"dumpling_orange", "dumpling"},
        {"butter_yellow", "butter"},
        {"butter_pink", "butter"},
        {"butter_lilac", "butter"},
        {"butter_blue", "butter_crunchy"},
        {"butter_mint", "butter_crunchy"}
    };

    @Override
    public void onInitialize() {
        ModSounds.init();
        // (id, malha, blocos em fila, comprimento, altura, largura, fracao da colisao, sons...)
        TYPES.put("needoh", new SquishyType("needoh", "needoh", 1, 0.70f, 0.70f, 0.70f, 0.4f, ModSounds.NEEDOH_PRESS, ModSounds.NEEDOH_RELEASE, 1.0f));
        TYPES.put("gumdrop", new SquishyType("gumdrop", "gumdrop", 1, 0.70f, 0.67f, 0.70f, 0.4f, ModSounds.NEEDOH_PRESS, ModSounds.NEEDOH_RELEASE, 1.0f));
        TYPES.put("dumpling", new SquishyType("dumpling", "dumpling", 1, 1.0f, 0.62f, 1.0f, 0.45f, ModSounds.DUMPLING_SQUISH, null, 1.0f));
        // butter: multibloco de 3x1 blocos (3 de comprimento, 1 de largura)
        TYPES.put("butter", new SquishyType("butter", "butter", 3, 3.0f, 0.5f, 1.0f, 0.4f, ModSounds.BUTTER_PRESS, ModSounds.BUTTER_RELEASE, 1.0f));
        TYPES.put("butter_crunchy", new SquishyType("butter_crunchy", "butter", 3, 3.0f, 0.5f, 1.0f, 0.4f, ModSounds.CRUNCHY_PRESS, ModSounds.CRUNCHY_RELEASE, 2.0f));
        List<Item> items = new ArrayList<>();
        for (String[] b : BLOCKS) {
            Identifier id = new Identifier(MOD_ID, b[0]);
            SquishyBlock block = Registry.register(Registries.BLOCK, id, new SquishyBlock(TYPES.get(b[1]), b[0],
                    AbstractBlock.Settings.create().strength(0.4f).sounds(BlockSoundGroup.SLIME).nonOpaque()));
            SQUISHY_BLOCKS.add(block);
            items.add(Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings())));
        }
        SQUISHY_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, "squishy"),
                FabricBlockEntityTypeBuilder.create(SquishyBlockEntity::new, SQUISHY_BLOCKS.toArray(new SquishyBlock[0])).build());
        Registry.register(Registries.ITEM_GROUP, new Identifier(MOD_ID, "main"), FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.squishies.main"))
                .icon(() -> new ItemStack(items.get(0)))
                .entries((ctx, entries) -> items.forEach(entries::add))
                .build());
    }
}
