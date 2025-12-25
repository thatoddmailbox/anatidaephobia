package dev.studer.alex.anatidaephobia.entity;

import dev.studer.alex.anatidaephobia.AnatidaephobiaItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Static properties and utility functions for Duck entities.
 */
public final class DuckProps {
	private DuckProps() {} // Prevent instantiation

	// Duck names for random generation
	private static final String[] DUCK_NAMES = {
		"Quackers", "Waddles", "Gerald", "Howard", "Mallard", "Puddles",
		"Bill", "Drake", "Webby", "Alistair", "Barnaby", "Mortimer",
		"Finley", "Winston", "Bob"
	};

	public static String generateDuckName(int hashCode) {
		int index = Math.abs(hashCode) % DUCK_NAMES.length;
		return DUCK_NAMES[index];
	}

	// XP thresholds for each level (total XP required to reach that level)
	private static final int[] LEVEL_XP_THRESHOLDS = {0, 10, 25, 55, 115};

	public static int getMaxLevel() {
		return LEVEL_XP_THRESHOLDS.length;
	}

	public static int getLevelForXP(int xp) {
		for (int level = LEVEL_XP_THRESHOLDS.length; level >= 1; level--) {
			if (xp >= LEVEL_XP_THRESHOLDS[level - 1]) {
				return level;
			}
		}
		return 1;
	}

	// Returns XP progress within the current level (0 to getMaxXPForLevel()-1)
	public static int getCurrentXPInLevel(int xp) {
		int level = getLevelForXP(xp);
		return xp - LEVEL_XP_THRESHOLDS[level - 1];
	}

	// Returns XP needed to advance from given level to next (0 if max level)
	public static int getMaxXPForLevel(int level) {
		if (level >= LEVEL_XP_THRESHOLDS.length) {
			return 0; // Max level
		}
		return LEVEL_XP_THRESHOLDS[level] - LEVEL_XP_THRESHOLDS[level - 1];
	}

	public static int getValueOfFood(ItemStack itemStack) {
		if (itemStack.is(Items.WHEAT_SEEDS) || itemStack.is(Items.MELON_SEEDS) ||
				itemStack.is(Items.PUMPKIN_SEEDS) || itemStack.is(Items.BEETROOT_SEEDS) ||
				itemStack.is(Items.TORCHFLOWER_SEEDS) || itemStack.is(Items.PITCHER_POD)) {
			return 1; // seeds
		} else if (itemStack.is(Items.SEAGRASS)) {
			return 1;
		} else if (itemStack.is(Items.KELP) || itemStack.is(Items.DRIED_KELP)) {
			return 2;
		} else if (itemStack.is(Items.BEETROOT)) {
			return 2; // but they don't like it so much (aren't tempted by it)
		} else if (itemStack.is(Items.SWEET_BERRIES)) {
			return 3;
		} else if (itemStack.is(Items.MELON_SLICE)) {
			return 4;
		} else if (itemStack.is(Items.GLISTERING_MELON_SLICE)) {
			return 7;
		}

		return 1; // default for any other duck food
	}

	public static ItemStack getRandomNestDrop(int level, RandomSource random) {
		float roll = random.nextFloat();

		// TODO: these are placeholders, need to make some more items
		return switch (level) {
			case 1 -> new ItemStack(AnatidaephobiaItems.DUCK_EGG);
			case 2 -> roll < 0.6f ? new ItemStack(AnatidaephobiaItems.DUCK_EGG)
					: new ItemStack(Items.FEATHER);
			case 3 -> roll < 0.6f ? new ItemStack(AnatidaephobiaItems.DUCK_EGG)
					: new ItemStack(AnatidaephobiaItems.RAW_QUACKMIUM);
			case 4 -> roll < 0.7f ? new ItemStack(AnatidaephobiaItems.RAW_QUACKMIUM)
					: new ItemStack(AnatidaephobiaItems.DUCK_EGG);
			case 5 -> roll < 0.8f ? new ItemStack(AnatidaephobiaItems.RAW_QUACKMIUM)
					: new ItemStack(AnatidaephobiaItems.RAW_QUACKMIUM, 2);
			default -> new ItemStack(AnatidaephobiaItems.DUCK_EGG);
		};
	}
}
