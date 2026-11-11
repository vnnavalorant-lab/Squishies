package com.squishies;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
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
        {"butter_slab_pink", "butter_slab"},
        {"butter_slab_yellow", "butter_slab"},
        {"butter_yellow", "butter"},
        {"butter_cream", "butter"},
        {"crunchy_butter_light", "crunchy_butter"},
        {"crunchy_butter_blue", "crunchy_butter"},
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
        {"dumpling_orange", "dumpling"}
    };

    @Override
    public void onInitialize() {
        ModSounds.init();
        TYPES.put("needoh", new SquishyType("needoh", 8f, 8f, 8f, 0.25f, ModSounds.NEEDOH_PRESS, ModSounds.NEEDOH_RELEASE, 1.0f));
        TYPES.put("gumdrop", new SquishyType("gumdrop", 8f, 7.7f, 8f, 0.25f, ModSounds.NEEDOH_PRESS, ModSounds.NEEDOH_RELEASE, 1.0f));
        TYPES.put("butter_slab", new SquishyType("butter_slab", 16f, 3.8f, 5.2f, 0.25f, ModSounds.BUTTER_PRESS, ModSounds.BUTTER_RELEASE, 1.0f));
        TYPES.put("butter", new SquishyType("butter", 5f, 3.5f, 14.5f, 0.25f, ModSounds.BUTTER_PRESS, ModSounds.BUTTER_RELEASE, 1.0f));
        TYPES.put("crunchy_butter", new SquishyType("crunchy_butter", 5f, 3.5f, 14.5f, 0.25f, ModSounds.CRUNCHY_PRESS, ModSounds.CRUNCHY_RELEASE, 2.0f));
        TYPES.put("dumpling", new SquishyType("dumpling", 14f, 9.5f, 14f, 0.65f, ModSounds.DUMPLING_SQUISH, null, 1.0f));
        List<Item> items = new ArrayList<>();
        for (String[] b : BLOCKS) {
            Identifier id = new Identifier(MOD_ID, b[0]);
            Block block = Registry.register(Registries.BLOCK, id, new SquishyBlock(TYPES.get(b[1]),
                    AbstractBlock.Settings.create().strength(0.4f).sounds(BlockSoundGroup.SLIME).nonOpaque()));
            Item item = Registry.register(Registries.ITEM, id, new BlockItem(block, new FabricItemSettings()));
            items.add(item);
        }
        Registry.register(Registries.ITEM_GROUP, new Identifier(MOD_ID, "main"), FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.squishies.main"))
                .icon(() -> new ItemStack(items.get(0)))
                .entries((ctx, entries) -> items.forEach(entries::add))
                .build());
    }
}
