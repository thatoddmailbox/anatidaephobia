package dev.studer.alex.anatidaephobia;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class DuckRenderState extends LivingEntityRenderState {
	// Styled components for each line
	public @Nullable Component duckNameText;
	public @Nullable Component duckLevelText;
	public @Nullable Component duckStatusText;

	// Raw strings for width calculation
	public String rawDuckName = "";
	public String rawLevelLine = "";
	public String rawStatusLine = "";
}
