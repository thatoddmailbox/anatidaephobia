package dev.studer.alex.anatidaephobia.screen;

import dev.studer.alex.anatidaephobia.entity.Duck;
import dev.studer.alex.anatidaephobia.menu.DuckMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, EXPERIENCE_BAR_BACKGROUND, xo + 16, yo + 16, 102, 5);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float
			partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}
}
