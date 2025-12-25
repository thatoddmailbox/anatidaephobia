package dev.studer.alex.anatidaephobia;

import com.mojang.logging.LogUtils;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.UUID;

public class DuckNestManager {
	private static HashMap<UUID, BlockPos> claimedNests = new HashMap<>();

	public static boolean isCentralBlockOfValidNest(LevelReader level, BlockPos pos) {
		// Basic requirements for a nest:
		// * It has a central block (hay or lining)
		// * It is a 3x3 flat and open space (each block has air above it).

		boolean centralBlockValid = level.getBlockState(pos).is(AnatidaephobiaBlocks.NEST_CENTER) && level.getBlockState(pos.above()).isAir();
		if (!centralBlockValid) {
			return false;
		}

		// Check the whole 3x3 area, make sure that it's flat and open
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos checkPos = pos.offset(dx, 0, dz);
				// Each block in the 3x3 must be solid and have air above it
				if (!level.getBlockState(checkPos).isSolid()) {
					return false;
				}
				if (!level.getBlockState(checkPos.above()).isAir()) {
					return false;
				}

				if (dx != 0 && dz != 0) {
					// The border blocks cannot be nest center materials (otherwise you could have overlapping nests!)
					if (level.getBlockState(checkPos).is(AnatidaephobiaBlocks.NEST_CENTER)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	public static int getNestQualityLevel(LevelReader level, BlockPos pos) {
		//
		// Nest quality levels:
		// * NQ1: Hay block with any solid blocks as ring
		// * NQ2: Nest lining with any solid blocks as ring
		// * NQ3: Nest lining with mud bricks as ring
		// * NQ4: Nest lining with calcite as ring
		// * NQ5: Nest lining with quackmium blocks as ring
		//

		BlockState centerState = level.getBlockState(pos);

		// NQ1: Hay block center (lowest quality)
		if (!centerState.is(AnatidaephobiaBlocks.NEST_LINING)) {
			return 1;
		}

		// Center is nest lining - check the ring to determine quality (NQ2-5)
		boolean allQuackmium = true;
		boolean allCalcite = true;
		boolean allMudBricks = true;

		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0) {
					continue; // Skip center block
				}
				BlockState ringState = level.getBlockState(pos.offset(dx, 0, dz));
				if (!ringState.is(AnatidaephobiaBlocks.QUACKMIUM_BLOCK)) {
					allQuackmium = false;
				}
				if (!ringState.is(Blocks.CALCITE)) {
					allCalcite = false;
				}
				if (!ringState.is(Blocks.MUD_BRICKS)) {
					allMudBricks = false;
				}
			}
		}

		if (allQuackmium) {
			return 5;
		}
		if (allCalcite) {
			return 4;
		}
		if (allMudBricks) {
			return 3;
		}
		return 2;
	}

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
