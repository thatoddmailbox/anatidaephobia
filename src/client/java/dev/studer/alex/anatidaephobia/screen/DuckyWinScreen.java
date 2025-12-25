package dev.studer.alex.anatidaephobia.screen;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

/**
 * A parody of the vanilla WinScreen that displays a custom ducky-themed script.
 * Triggered by the Ducky Portal (or other means as desired).
 */
public class DuckyWinScreen extends Screen {
    private static final Identifier VIGNETTE_LOCATION = Identifier.withDefaultNamespace("textures/misc/credits_vignette.png");
    private static final Logger LOGGER = LogUtils.getLogger();

    // The ducky script file location - loaded from mod assets
    private static final Identifier DUCKY_SCRIPT_LOCATION = Identifier.parse("anatidaephobia:texts/ducky.txt");

    // Special token for obfuscated "quack" text (similar to vanilla's obfuscate token)
    private static final String QUACK_TOKEN = "§f§k§a§b§3";

    private static final float SPEEDUP_FACTOR = 5.0F;
    private static final float SPEEDUP_FACTOR_FAST = 15.0F;

    private final Runnable onFinished;
    private float scroll;
    private List<FormattedCharSequence> lines;
    private List<Component> narratorComponents;
    private IntSet centeredLines;
    private int totalScrollLength;
    private boolean speedupActive;
    private final IntSet speedupModifiers = new IntOpenHashSet();
    private float scrollSpeed;
    private final float unmodifiedScrollSpeed = 0.75F;
    private int direction = 1;

    public DuckyWinScreen(Runnable onFinished) {
        super(GameNarrator.NO_TITLE);
        this.onFinished = onFinished;
        this.scrollSpeed = this.unmodifiedScrollSpeed;
    }

    private float calculateScrollSpeed() {
        if (this.speedupActive) {
            return this.unmodifiedScrollSpeed * (SPEEDUP_FACTOR + (float) this.speedupModifiers.size() * SPEEDUP_FACTOR_FAST) * (float) this.direction;
        }
        return this.unmodifiedScrollSpeed * (float) this.direction;
    }

    @Override
    public void tick() {
        this.minecraft.getMusicManager().tick();
        this.minecraft.getSoundManager().tick(false);
        float maxScroll = (float) (this.totalScrollLength + this.height + 30);
        if (this.scroll > maxScroll) {
            this.respawn();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isUp()) {
            this.direction = -1;
        } else if (event.key() != 341 && event.key() != 345) {
            if (event.key() == 32) {
                this.speedupActive = true;
            }
        } else {
            this.speedupModifiers.add(event.key());
        }
        this.scrollSpeed = this.calculateScrollSpeed();
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.isUp()) {
            this.direction = 1;
        }
        if (event.key() == 32) {
            this.speedupActive = false;
        } else if (event.key() == 341 || event.key() == 345) {
            this.speedupModifiers.remove(event.key());
        }
        this.scrollSpeed = this.calculateScrollSpeed();
        return super.keyReleased(event);
    }

    @Override
    public void onClose() {
        this.respawn();
    }

    private void respawn() {
        this.onFinished.run();
    }

    @Override
    protected void init() {
        if (this.lines == null) {
            this.lines = Lists.newArrayList();
            this.narratorComponents = Lists.newArrayList();
            this.centeredLines = new IntOpenHashSet();

            this.loadDuckyScript();

            this.totalScrollLength = this.lines.size() * 12;
            LOGGER.info("DuckyWinScreen initialized with {} lines, total scroll length: {}", this.lines.size(), this.totalScrollLength);
        }
    }

    private void loadDuckyScript() {
        try {
            Reader resource = this.minecraft.getResourceManager().openAsReader(DUCKY_SCRIPT_LOCATION);
            try {
                this.addScriptFile(resource);
            } catch (Throwable var7) {
                if (resource != null) {
                    try {
                        resource.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }
                }
                throw var7;
            }
            if (resource != null) {
                resource.close();
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't load ducky script from file {}", DUCKY_SCRIPT_LOCATION, e);
            // Add fallback message if script fails to load
            this.addLine(Component.literal("Quack!").withStyle(ChatFormatting.YELLOW));
            this.addEmptyLine();
            this.addLine(Component.literal("The ducky script could not be loaded.").withStyle(ChatFormatting.RED));
        }
    }

    private void addScriptFile(Reader inputReader) throws IOException {
        BufferedReader reader = new BufferedReader(inputReader);
        RandomSource random = RandomSource.create(8124371L);

        String line;
        while ((line = reader.readLine()) != null) {
            // Replace player name placeholder
            line = line.replaceAll("PLAYERNAME", this.minecraft.getUser().getName());

            // Check for centered lines (lines starting with [CENTER])
            boolean centered = false;
            if (line.startsWith("[CENTER]")) {
                centered = true;
                line = line.substring(8);
            }

            // Check for special duck formatting (lines starting with [DUCK])
            boolean isDuckLine = line.startsWith("[DUCK]");
            if (isDuckLine) {
                line = line.substring(6);
            }

            // Handle obfuscated "quack" text (similar to vanilla's obfuscation)
            // Reset to yellow for duck lines, full reset otherwise
            int pos;
            while ((pos = line.indexOf(QUACK_TOKEN)) != -1) {
                String before = line.substring(0, pos);
                String after = line.substring(pos + QUACK_TOKEN.length());
                // Generate random quacking sounds
                String[] quacks = {"QUACK", "quack", "Quack", "qUACK", "quACK"};
                String quack = quacks[random.nextInt(quacks.length)];
                String reset = isDuckLine ? ChatFormatting.YELLOW.toString() : ChatFormatting.RESET.toString();
                line = before + ChatFormatting.BLUE + ChatFormatting.OBFUSCATED + quack + reset + after;
            }

            if (isDuckLine) {
                Component component = Component.literal(line).withStyle(ChatFormatting.YELLOW);
                this.addLine(component, centered);
            } else {
                this.addScriptLines(line, centered);
            }
            this.addEmptyLine();
        }

        // Add padding at the end
        for (int i = 0; i < 8; ++i) {
            this.addEmptyLine();
        }
    }

    private void addEmptyLine() {
        this.lines.add(FormattedCharSequence.EMPTY);
        this.narratorComponents.add(CommonComponents.EMPTY);
    }

    private void addScriptLines(String line, boolean centered) {
        Component component = Component.literal(line);
        if (centered) {
            this.centeredLines.add(this.lines.size());
        }
        this.lines.addAll(this.minecraft.font.split(component, 256));
        this.narratorComponents.add(component);
    }

    private void addLine(Component line) {
        addLine(line, false);
    }

    private void addLine(Component line, boolean centered) {
        if (centered) {
            this.centeredLines.add(this.lines.size());
        }
        this.lines.addAll(this.minecraft.font.split(line, 256));
        this.narratorComponents.add(line);
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(this.narratorComponents.toArray(new Component[0]));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        this.renderVignette(graphics);

        // Update scroll position
        this.scroll = Math.max(0.0F, this.scroll + delta * this.scrollSpeed);

        int centerX = this.width / 2;
        int startY = this.height + 20;
        float yOffs = -this.scroll;

        // Debug: show scroll info at top of screen (always visible)
        // graphics.drawString(this.font, "Lines: " + this.lines.size() + " Scroll: " + (int)this.scroll, 5, 5, 0xFFFFFFFF);

        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, yOffs);
        graphics.nextStratum();

        // Render a duck title instead of the Minecraft logo
        Component title = Component.literal("ANATIDAEPHOBIA").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        graphics.drawCenteredString(this.font, title, centerX, startY, 0xFFFFFFFF);

        int yPos = startY + 50;

        for (int i = 0; i < this.lines.size(); ++i) {
            if (i == this.lines.size() - 1) {
                float diff = (float) yPos + yOffs - (float) (this.height / 2 - 6);
                if (diff < 0.0F) {
                    graphics.pose().translate(0.0F, -diff);
                }
            }

            if ((float) yPos + yOffs + 12.0F + 8.0F > 0.0F && (float) yPos + yOffs < (float) this.height) {
                FormattedCharSequence formattedLine = this.lines.get(i);
                if (this.centeredLines.contains(i)) {
                    graphics.drawCenteredString(this.font, formattedLine, centerX, yPos, 0xFFFFFFFF);
                } else {
                    graphics.drawString(this.font, formattedLine, centerX - 128, yPos, 0xFFFFFFFF);
                }
            }

            yPos += 12;
        }

        graphics.pose().popMatrix();
    }

    private void renderVignette(GuiGraphics graphics) {
        graphics.blit(RenderPipelines.VIGNETTE, VIGNETTE_LOCATION, 0, 0, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Use a simple dark background with a yellow/gold tint for ducky theme
        graphics.fill(0, 0, this.width, this.height, 0xFF1a1a0a);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isAllowedInPortal() {
        return true;
    }

    /**
     * Convenience method to show this screen.
     * Call this from your Ducky Portal or other trigger.
     */
    public static void show() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new DuckyWinScreen(() -> {
            // When finished, respawn the player and close the screen
            minecraft.player.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            minecraft.setScreen(null);
        }));
    }
}
