package com.squishies.client;

import com.squishies.SquishiesMod;
import com.squishies.SquishyBlock;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public class SquishiesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(SquishiesMod.SQUISHY_BE, SquishyRenderer::new);

        for (SquishyBlock block : SquishiesMod.SQUISHY_BLOCKS) {
            BuiltinItemRendererRegistry.INSTANCE.register(block.asItem(),
                    (stack, mode, matrices, vcp, light, overlay) -> SquishyRenderer.renderItem(block, matrices, vcp, light, overlay));
        }

        // F3+T recarrega o OBJ/texturas
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override public Identifier getFabricId() { return new Identifier(SquishiesMod.MOD_ID, "meshes"); }
            @Override public void reload(ResourceManager manager) { MeshRegistry.clear(); }
        });
    }
}
