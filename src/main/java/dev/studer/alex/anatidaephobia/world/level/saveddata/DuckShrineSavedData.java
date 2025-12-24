package dev.studer.alex.anatidaephobia.world.level.saveddata;

import com.mojang.serialization.Codec;
import dev.studer.alex.anatidaephobia.Anatidaephobia;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuckShrineSavedData extends SavedData {
	private final Map<UUID, Long> playerCooldowns;

	private static final Codec<Map<UUID, Long>> COOLDOWNS_CODEC = Codec.unboundedMap(
			UUIDUtil.STRING_CODEC,
			Codec.LONG
	);

	private static final Codec<DuckShrineSavedData> CODEC = COOLDOWNS_CODEC.xmap(
			DuckShrineSavedData::new,
			data -> data.playerCooldowns
	);

	private static final SavedDataType<DuckShrineSavedData> TYPE = new SavedDataType<>(
			"anatidaephobia_shrine_cooldowns",
			DuckShrineSavedData::new,
			CODEC,
			null
	);

	public DuckShrineSavedData() {
		this.playerCooldowns = new HashMap<>();
	}

	public DuckShrineSavedData(Map<UUID, Long> cooldowns) {
		this.playerCooldowns = new HashMap<>(cooldowns);
	}

	public static DuckShrineSavedData get(MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		return overworld.getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean isOnCooldown(UUID playerId, long currentGameTime, long cooldownTicks) {
		if (!playerCooldowns.containsKey(playerId)) {
			return false;
		}
		long lastSummon = playerCooldowns.get(playerId);
		return (currentGameTime - lastSummon) < cooldownTicks;
	}

	public int getRemainingCooldownSeconds(UUID playerId, long currentGameTime, long cooldownTicks) {
		if (!playerCooldowns.containsKey(playerId)) {
			return 0;
		}
		long lastSummon = playerCooldowns.get(playerId);
		long remainingTicks = cooldownTicks - (currentGameTime - lastSummon);
		if (remainingTicks <= 0) {
			return 0;
		}
		return (int) (remainingTicks / Anatidaephobia.TICKS_PER_SECOND);
	}

	public void markSummoned(UUID playerId, long currentGameTime) {
		playerCooldowns.put(playerId, currentGameTime);
		setDirty();
	}
}
