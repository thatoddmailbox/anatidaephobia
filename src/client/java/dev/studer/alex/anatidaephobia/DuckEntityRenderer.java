package dev.studer.alex.anatidaephobia;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;

public class DuckEntityRenderer extends MobRenderer<Duck, DuckRenderState, DuckEntityModel> {

	// Scale constants - adjust these to taste
	private static final float NAME_SCALE = 0.025f;
	private static final float STATUS_SCALE = 0.0175f;  // Smaller for status line (0.8x default)
	private static final float SMALL_LINE_SPACING = 0.185f;   // Gap between small lines in world units
	private static final float LINE_SPACING = 0.25f;   // Gap between lines in world units

	public DuckEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new DuckEntityModel(context.bakeLayer(AnatidaephobiaClient.MODEL_DUCK_LAYER)), 0.5f);
	}

	@Override
	public Identifier getTextureLocation(DuckRenderState state) {
		return Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, "textures/entity/duck/duck.png");
	}

	@Override
	public DuckRenderState createRenderState() {
		return new DuckRenderState();
	}

	@Override
	public void extractRenderState(Duck duck, DuckRenderState state, float partialTicks) {
		super.extractRenderState(duck, state, partialTicks);

		// Build the name line
		state.rawDuckName = duck.getDuckName();
		state.duckNameText = Component.literal(state.rawDuckName)
				.withStyle(Style.EMPTY);

		// Build level line
		String levelStr = "Level " + duck.getDuckLevel();
		state.rawLevelLine = levelStr;
		state.duckLevelText = Component.literal(levelStr)
				.withStyle(Style.EMPTY);

		// Build status line
		if (duck.getDuckStateEnum() != Duck.DuckState.DEFAULT) {
			String statusStr = getStateDisplayName(duck);
			state.rawStatusLine = statusStr;
			state.duckStatusText = Component.literal(statusStr)
					.withStyle(Style.EMPTY);
		} else {
			state.rawStatusLine = "";
			state.duckStatusText = null;
		}
	}

	private String getStateDisplayName(Duck duck) {
		if (duck.isDeadOrDying()) {
			return "Dead";
		}

		return switch (duck.getDuckStateEnum()) {
			case SCARED -> "Scared";
			case NESTING -> "Nesting";
			case HUNGRY -> "Hungry";
			case STRESSED -> "Stressed";
			case DESTRESSING -> "Relaxing";
			case LONELY -> "Lonely";
			case SOCIALIZING -> "Chatting";
			case DEFAULT -> "Cool";
		};
	}

	@Override
	protected boolean shouldShowName(Duck duck, double distanceToCameraSq) {
		// Always show the duck's nametag (replaces setCustomNameVisible(true))
		return true;
	}

	@Override
	protected void submitNameTag(DuckRenderState state, PoseStack poseStack,
								 SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		Vec3 attachment = state.nameTagAttachment;
		if (attachment == null) return;

		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		int bgColor = (int)(mc.options.getBackgroundOpacity(0.25f) * 255.0f) << 24;
		int lightCoords = LightTexture.lightCoordsWithEmission(state.lightCoords, 2);

		poseStack.pushPose();

		// Position at the nametag attachment point
		poseStack.translate(attachment.x, attachment.y + 0.425f, attachment.z);

		// Billboard - face the camera
		poseStack.mulPose((Quaternionfc) camera.orientation);

		// --- Render status line (bottom) ---
		if (state.duckStatusText != null) {
			poseStack.pushPose();
			poseStack.scale(STATUS_SCALE, -STATUS_SCALE, STATUS_SCALE);

			float statusWidth = font.width(state.rawStatusLine);
			float statusX = -statusWidth / 2.0f;

			submitNodeCollector.submitText(
					poseStack,
					statusX, 0,
					state.duckStatusText.getVisualOrderText(),
					false,  // no drop shadow
					Font.DisplayMode.NORMAL,
					lightCoords,
					-1,     // white (component style overrides)
					bgColor,
					0       // no outline
			);
			poseStack.popPose();
		}

		// --- Render level line (middle) ---
		if (state.duckLevelText != null) {
			poseStack.pushPose();

			// Move up for the level line
			poseStack.translate(0, SMALL_LINE_SPACING, 0);

			poseStack.scale(STATUS_SCALE, -STATUS_SCALE, STATUS_SCALE);

			float statusWidth = font.width(state.rawLevelLine);
			float statusX = -statusWidth / 2.0f;

			submitNodeCollector.submitText(
					poseStack,
					statusX, 0,
					state.duckLevelText.getVisualOrderText(),
					false,  // no drop shadow
					Font.DisplayMode.NORMAL,
					lightCoords,
					-1,     // white (component style overrides)
					bgColor,
					0       // no outline
			);
			poseStack.popPose();
		}

		// --- Render name line (top, offset up) ---
		if (state.duckNameText != null) {
			poseStack.pushPose();

			// Move more up for the name line
			poseStack.translate(0, SMALL_LINE_SPACING+LINE_SPACING, 0);

			poseStack.scale(NAME_SCALE, -NAME_SCALE, NAME_SCALE);

			float nameWidth = font.width(state.rawDuckName);
			float nameX = -nameWidth / 2.0f;

			submitNodeCollector.submitText(
					poseStack,
					nameX, 0,
					state.duckNameText.getVisualOrderText(),
					false,
					Font.DisplayMode.NORMAL,
					lightCoords,
					-1,
					bgColor,
					0
			);
			poseStack.popPose();
		}

		poseStack.popPose();
	}
}
