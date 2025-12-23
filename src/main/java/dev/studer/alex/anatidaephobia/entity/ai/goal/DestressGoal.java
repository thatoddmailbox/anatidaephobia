package dev.studer.alex.anatidaephobia.entity.ai.goal;

import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class DestressGoal extends Goal {
	private static final int STRESS_THRESHOLD = 3;
	private static final int SEARCH_RANGE = 16;
	private static final double SPEED_MODIFIER = 1.0;
	private static final int DESTRESS_INTERVAL = 3 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int MIN_SWIM_DURATION = 5 * Anatidaephobia.TICKS_PER_SECOND;

	private final Duck duck;
	private boolean isRunning;
	private BlockPos targetWaterPos;
	private int ticksInWater;
	private int ticksRunning;

	public DestressGoal(Duck duck) {
		this.duck = duck;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	public boolean isRunning() {
		return this.isRunning;
	}

	@Override
	public boolean canUse() {
		if (duck.getDuckStress() < STRESS_THRESHOLD) {
			return false;
		}

		if (duck.isInWater()) {
			return true;
		}

		// Try to find nearby water
		return findNearestWater();
	}

	private boolean findNearestWater() {
		BlockPos duckPos = duck.blockPosition();
		BlockPos bestPos = null;
		double bestDistSq = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.withinManhattan(duckPos, SEARCH_RANGE, SEARCH_RANGE / 2, SEARCH_RANGE)) {
			if (pos.getX() == duckPos.getX() && pos.getZ() == duckPos.getZ()) {
				continue;
			}

			BlockState state = duck.level().getBlockState(pos);
			BlockState aboveState = duck.level().getBlockState(pos.above());

			// Look for water with air above (so duck can swim on surface)
			if (state.is(Blocks.WATER) && aboveState.isAir()) {
				double distSq = pos.distSqr(duckPos);
				if (distSq < bestDistSq) {
					bestDistSq = distSq;
					bestPos = pos.immutable();
				}
			}
		}

		if (bestPos != null) {
			this.targetWaterPos = bestPos;
			return true;
		}

		return false;
	}

	@Override
	public boolean canContinueToUse() {
		// Continue while stressed and either:
		// - Still navigating to water
		// - Swimming in water (for minimum duration or until stress is gone)
		if (duck.getDuckStress() <= 0) {
			return false;
		}

		if (duck.isInWater()) {
			// Stay in water for minimum duration or until fully destressed
			return ticksInWater < MIN_SWIM_DURATION || duck.getDuckStress() > 0;
		}

		// Still trying to reach water
		return !duck.getNavigation().isDone() || findNearestWater();
	}

	@Override
	public void start() {
		isRunning = true;
		ticksInWater = 0;
		ticksRunning = 0;

		if (!duck.isInWater() && targetWaterPos != null) {
			duck.getNavigation().moveTo(
				targetWaterPos.getX() + 0.5,
				targetWaterPos.getY(),
				targetWaterPos.getZ() + 0.5,
				SPEED_MODIFIER
			);
		}
	}

	@Override
	public void stop() {
		isRunning = false;
		ticksInWater = 0;
		ticksRunning = 0;
		targetWaterPos = null;
		duck.getNavigation().stop();
	}

	@Override
	public void tick() {
		ticksRunning++;

		if (duck.isInWater()) {
			ticksInWater++;

			// Reduce stress periodically while swimming
			if (ticksInWater % DESTRESS_INTERVAL == 0) {
				duck.reduceDuckStress(1);
			}

			// Occasionally swim around randomly in the water
			if (ticksInWater % (2 * Anatidaephobia.TICKS_PER_SECOND) == 0) {
				swimRandomly();
			}
		} else {
			// Not in water yet, keep trying to reach it
			if (duck.getNavigation().isDone() && targetWaterPos != null) {
				// Recalculate path if we got stuck
				duck.getNavigation().moveTo(
					targetWaterPos.getX() + 0.5,
					targetWaterPos.getY(),
					targetWaterPos.getZ() + 0.5,
					SPEED_MODIFIER
				);
			}
		}
	}

	private void swimRandomly() {
		// Find a nearby water position to swim to
		BlockPos currentPos = duck.blockPosition();
		for (int i = 0; i < 5; i++) {
			int dx = duck.getRandom().nextInt(5) - 2;
			int dz = duck.getRandom().nextInt(5) - 2;
			BlockPos newPos = currentPos.offset(dx, 0, dz);

			if (duck.level().getFluidState(newPos).is(FluidTags.WATER)) {
				duck.getNavigation().moveTo(
					newPos.getX() + 0.5,
					newPos.getY(),
					newPos.getZ() + 0.5,
					SPEED_MODIFIER * 0.5
				);
				break;
			}
		}
	}
}
