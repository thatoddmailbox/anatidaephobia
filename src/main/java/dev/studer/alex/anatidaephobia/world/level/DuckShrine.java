package dev.studer.alex.anatidaephobia.world.level;

import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.AnatidaephobiaBlocks;
import dev.studer.alex.anatidaephobia.world.level.saveddata.DuckShrineSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class DuckShrine {
	// Cooldown duration in ticks (5 minutes = 6000 ticks)
	private static final long COOLDOWN_TICKS = 5 * 60 * Anatidaephobia.TICKS_PER_SECOND;

	/**
	 * Check if a position is within a valid duck shrine structure.
	 * Structure: 5x5 mud brick border with 3x3 water center.
	 *
	 * The bobber could be anywhere in the 3x3 water area, so we check
	 * all possible offsets to find a valid shrine configuration.
	 */
	public static boolean isInValidShrine(Level level, BlockPos bobberPos) {
		// The bobber is in water - check if it's part of a 3x3 water pool
		// surrounded by a 5x5 mud brick border.
		// Try each possible position within the 3x3 water area as the center.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos potentialCenter = bobberPos.offset(dx, 0, dz);
				if (isValidShrineAtCenter(level, potentialCenter)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check if there's a valid shrine with the given block as the center of the 3x3 water pool.
	 */
	private static boolean isValidShrineAtCenter(Level level, BlockPos center) {
		// Check the 3x3 water area
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos waterPos = center.offset(dx, 0, dz);
				if (!level.getFluidState(waterPos).is(FluidTags.WATER)) {
					return false;
				}
			}
		}

		// Check the 5x5 border (only the outer ring, not the inner 3x3)
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				// Skip the inner 3x3 area (already checked as water)
				if (dx >= -1 && dx <= 1 && dz >= -1 && dz <= 1) {
					continue;
				}

				BlockPos borderPos = center.offset(dx, 0, dz);
				BlockState state = level.getBlockState(borderPos);
				if (!state.is(AnatidaephobiaBlocks.DUCK_SHRINE_BORDER)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Check if a player is on cooldown for summoning ducks.
	 */
	public static boolean isOnCooldown(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		DuckShrineSavedData data = DuckShrineSavedData.get(level.getServer());
		long currentTime = level.getGameTime();
		return data.isOnCooldown(player.getUUID(), currentTime, COOLDOWN_TICKS);
	}

	/**
	 * Get remaining cooldown time in seconds.
	 */
	public static int getRemainingCooldownSeconds(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		DuckShrineSavedData data = DuckShrineSavedData.get(level.getServer());
		long currentTime = level.getGameTime();
		return data.getRemainingCooldownSeconds(player.getUUID(), currentTime, COOLDOWN_TICKS);
	}

	/**
	 * Mark that a player has summoned a duck (starts cooldown).
	 */
	public static void markSummoned(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		DuckShrineSavedData data = DuckShrineSavedData.get(level.getServer());
		data.markSummoned(player.getUUID(), level.getGameTime());
	}

	/**
	 * Send a cooldown admonishment message to the player.
	 */
	public static void sendCooldownMessage(ServerPlayer player) {
		int remainingSeconds = getRemainingCooldownSeconds(player);
		int minutes = remainingSeconds / 60;
		int seconds = remainingSeconds % 60;

		String timeString;
		if (minutes > 0) {
			timeString = minutes + "m " + seconds + "s";
		} else {
			timeString = seconds + "s";
		}

		player.sendSystemMessage(Component.literal(
				"The shrine's power has not yet recovered. Please wait " + timeString + "..."
		));
	}

	/**
	 * Send a success message when a duck is summoned.
	 */
	public static void sendSuccessMessage(ServerPlayer player) {
		player.sendSystemMessage(Component.literal(
				"Something emerges from the depths..."
		));
	}
}
