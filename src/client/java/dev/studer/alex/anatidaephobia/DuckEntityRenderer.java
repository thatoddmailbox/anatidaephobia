package dev.studer.alex.anatidaephobia;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class DuckEntityRenderer extends MobRenderer<DuckEntity, LivingEntityRenderState, DuckEntityModel> {
	public DuckEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new DuckEntityModel(context.bakeLayer(AnatidaephobiaClient.MODEL_DUCK_LAYER)), 0.5f);
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState entity) {
		return Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, "textures/entity/duck/duck.png");
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}
}
