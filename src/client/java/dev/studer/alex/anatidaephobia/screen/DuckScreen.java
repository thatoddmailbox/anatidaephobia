package dev.studer.alex.anatidaephobia.screen;

import dev.studer.alex.anatidaephobia.entity.Duck;
import dev.studer.alex.anatidaephobia.menu.DuckMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class DuckScreen extends AbstractContainerScreen<DuckMenu> {
	private static final Identifier TEXTURE =
			Identifier.fromNamespaceAndPath("anatidaephobia",
					"textures/gui/duck.png");

	private static final Identifier EXPERIENCE_BAR_BACKGROUND = Identifier.withDefaultNamespace("container/villager/experience_bar_background");
	private static final Identifier EXPERIENCE_BAR_CURRENT = Identifier.withDefaultNamespace("container/villager/experience_bar_current");
	private static final Identifier EXPERIENCE_BAR_RESULT = Identifier.withDefaultNamespace("container/villager/experience_bar_result");

	public DuckScreen(DuckMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 222;
	}

	@Override
	public Component getTitle() {
		Duck duck = menu.getDuck();
		return duck != null ? Component.literal(duck.getDuckName()) : super.getTitle();
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int
			mouseX, int mouseY) {
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
	}

	@Override
	protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
		// NOTE: this gets translated by (xo, yo) in AbstractContainerScreen

		// can't use graphics.drawCenteredString because it forces drop shadow
		Component title = getTitle();
		graphics.drawString(this.font, title, (this.imageWidth - font.width(title)) / 2, this.titleLabelY, 0xff404040, false);
	}

	@Override
	public void renderContents(final GuiGraphics graphics, final int mouseX, final int mouseY, final float a) {
		super.renderContents(graphics, mouseX, mouseY, a);

		int xo = (this.width - this.imageWidth) / 2;
		int yo = (this.height - this.imageHeight) / 2;

		Duck duck = getMenu().getDuck();

		int duckLevel = duck.getDuckLevel();
		int levelCurrentXP = duck.getLevelCurrentXP();
		int levelMaxXP = duck.getLevelMaxXP();
		int duckHunger = duck.getDuckHunger();
		int duckStress = duck.getDuckStress();
		int duckLoneliness = duck.getDuckLoneliness();

		String duckLevelString = "Level " + duckLevel + (duckLevel >= Duck.getMaxLevel() ? " (max level!)" : "");
		graphics.drawString(this.font, duckLevelString, (this.width - font.width(duckLevelString)) / 2, yo + 18, 0xff404040, false);

		int xpBarWidth = 102;
		int xpBarX = xo + ((this.imageWidth - xpBarWidth) / 2);
		int xpBarY = yo + 30;

		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND, xpBarX, xpBarY, xpBarWidth, 5);
		int xpBarGreenW;
		if (levelMaxXP > 0) {
			float multiplier = ((float) xpBarWidth) / ((float) levelMaxXP);
			xpBarGreenW = Math.min(Mth.floor(multiplier * ((float) levelCurrentXP)), xpBarWidth);
		} else {
			// Max level - show full bar
			xpBarGreenW = xpBarWidth;
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_CURRENT, xpBarWidth, 5, 0, 0, xpBarX, xpBarY, xpBarGreenW, 5);

		// TODO: make this look nicer
		graphics.drawString(this.font, "Hunger: " + duckHunger, xo + 8, yo + 40, 0xff404040, false);
		graphics.drawString(this.font, "Stress: " + duckStress, xo + 8, yo + 52, 0xff404040, false);
		graphics.drawString(this.font, "Loneliness: " + duckLoneliness, xo + 8, yo + 64, 0xff404040, false);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float
			partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}
}
