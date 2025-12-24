package dev.studer.alex.anatidaephobia;

import dev.studer.alex.anatidaephobia.network.AnatidaephobiaClientNetwork;
import dev.studer.alex.anatidaephobia.screen.DuckScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class AnatidaephobiaClient implements ClientModInitializer {
	public static final ModelLayerLocation MODEL_DUCK_LAYER = new ModelLayerLocation(AnatidaephobiaEntities.DUCK_ID, "main");

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		EntityRenderers.register(AnatidaephobiaEntities.DUCK, DuckEntityRenderer::new);
		EntityModelLayerRegistry.registerModelLayer(MODEL_DUCK_LAYER, DuckEntityModel::getTexturedModelData);

		EntityRenderers.register(AnatidaephobiaEntities.THROWN_DUCK_EGG, ThrownItemRenderer::new);

		MenuScreens.register(AnatidaephobiaMenus.DUCK_MENU, DuckScreen::new);

		AnatidaephobiaClientNetwork.init();
	}
}