package dev.studer.alex.anatidaephobia.screen;

import dev.studer.alex.anatidaephobia.entity.Duck;
import dev.studer.alex.anatidaephobia.entity.DuckProps;
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

	private static final Identifier BAR_SHORT_BACKGROUND = Identifier.fromNamespaceAndPath("anatidaephobia", "duck/bar_short_background");
	private static final Identifier BAR_SHORT_GOOD = Identifier.fromNamespaceAndPath("anatidaephobia", "duck/bar_short_good");

	private static final Identifier BAR_BACKGROUND = Identifier.fromNamespaceAndPath("anatidaephobia", "duck/bar_background");
	private static final Identifier BAR_GOOD = Identifier.fromNamespaceAndPath("anatidaephobia", "duck/bar_good");
	private static final Identifier BAR_OK = Identifier.fromNamespaceAndPath("anatidaephobia", "duck/bar_ok");
	private static final Identifier BAR_BAD = Identifier.fromNamespaceAndPath("anatidaephobia", "duck/bar_bad");

	private int BAR_WIDTH = 160;
	private int BAR_SHORT_WIDTH = 102;

	private int LEFT_MARGIN = 7;

	protected enum BarStyle {
		GOOD,
		OK,
		BAD
	};

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

	protected void drawBar(final GuiGraphics graphics, int x, int y, int value, int max, boolean isShort, BarStyle bar) {
		int effectiveWidth = isShort ? BAR_SHORT_WIDTH : BAR_WIDTH;

		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, isShort ? BAR_SHORT_BACKGROUND : BAR_BACKGROUND, x, y, effectiveWidth, 5);

		int fillW;
		if (max > 0) {
			float multiplier = ((float) effectiveWidth) / ((float) max);
			fillW = Math.min(Mth.floor(multiplier * ((float) value)), effectiveWidth);
		} else {
			// Max level - show full bar
			fillW = effectiveWidth;
		}

		graphics.blitSprite(
				RenderPipelines.GUI_TEXTURED,
				(isShort ? BAR_SHORT_GOOD : (bar == BarStyle.BAD ? BAR_BAD : bar == BarStyle.OK ? BAR_OK : BAR_GOOD)),
				effectiveWidth,
				5,
				0,
				0,
				x,
				y,
				fillW,
				5
		);
	}

	protected void drawStat(final GuiGraphics graphics, int x, int y, String label, int value) {
		// TODO: I suppose all of these strings should be translatable
		String hint = switch (value) {
			case 0 -> "None";
			case 1, 2, 3 -> "Low";
			case 4, 5, 6 -> "Medium";
			case 7, 8, 9 -> "High";
			case 10 -> "Very High";
			default -> "Unknown";
		};

		BarStyle barStyle = switch (value) {
			case 0, 1, 2, 3 -> BarStyle.GOOD;
			case 4, 5, 6 -> BarStyle.OK;
			case 7, 8, 9, 10 -> BarStyle.BAD;
			default -> BarStyle.BAD;
		};

		// Draw stat name
		graphics.drawString(this.font, label, x, y, 0xff404040, false);

		// Draw stat hint
		graphics.drawString(this.font, hint, x + BAR_WIDTH - font.width(hint), y, 0xff404040, false);

		// Draw stat bar
		drawBar(graphics, x, y + 12, value, 10, false, barStyle);
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

		String duckLevelString = "Level " + duckLevel + (duckLevel >= DuckProps.getMaxLevel() ? " (max level!)" : "");
		graphics.drawString(this.font, duckLevelString, (this.width - font.width(duckLevelString)) / 2, yo + 18, 0xff404040, false);

		int xpBarX = xo + ((this.imageWidth - BAR_SHORT_WIDTH) / 2);
		int xpBarY = yo + 30;
		drawBar(graphics, xpBarX, xpBarY, levelCurrentXP, levelMaxXP, true, BarStyle.GOOD);

		drawStat(graphics, xo + LEFT_MARGIN, yo + 50, "Hunger", duckHunger);
		drawStat(graphics, xo + LEFT_MARGIN, yo + 50 + 32, "Stress", duckStress);
		drawStat(graphics, xo + LEFT_MARGIN, yo + 50 + 32 + 32, "Loneliness", duckLoneliness);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float
			partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderTooltip(graphics, mouseX, mouseY);
	}
}
