package dev.studer.alex.anatidaephobia;

import com.mojang.logging.LogUtils;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.UUID;

public class DuckNestManager {
	private static HashMap<UUID, BlockPos> claimedNests = new HashMap<>();

	public static boolean isNestFree(BlockPos p, UUID self) {
		return !claimedNests.containsValue(p) || (claimedNests.get(self) == p);
	}

	public static void claimNest(final Duck d, BlockPos p) {
		LogUtils.getLogger().info("[nestmgr] claimNest {} {}", d.getUUID(), p);
		claimedNests.put(d.getUUID(), p);
		LogUtils.getLogger().info("[nestmgr] claimNest {}", claimedNests);
	}

	public static void unclaimNest(final Duck d) {
		LogUtils.getLogger().info("[nestmgr] unclaimNest {}", d.getUUID());
		claimedNests.remove(d.getUUID());
		LogUtils.getLogger().info("[nestmgr] unclaimNest {}", claimedNests);
	}
}
