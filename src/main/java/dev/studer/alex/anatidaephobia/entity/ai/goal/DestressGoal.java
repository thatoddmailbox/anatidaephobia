package dev.studer.alex.anatidaephobia.entity.ai.goal;

import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class DestressGoal extends Goal {
	private static final int STRESS_THRESHOLD = 3;
	private static final int MAX_STRESS = 10;
	private static final int SEARCH_RANGE = 16;
	private static final int MIN_WATER_AREA = 16;
	private static final double SPEED_MODIFIER = 1.0;
	private static final int DESTRESS_INTERVAL = 7 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int MIN_SWIM_DURATION = 5 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int NAVIGATION_TIMEOUT = 10 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int WATER_VALIDATION_INTERVAL = 2 * Anatidaephobia.TICKS_PER_SECOND;

	private final Duck duck;
	private boolean isRunning;
	private BlockPos targetWaterPos;
	private int ticksInWater;
	private int ticksRunning;
	private int ticksNavigating;

	// Cache for water body validation to avoid expensive flood-fill every tick
	private BlockPos lastValidatedPos;
	private long lastValidationTick;
	private boolean lastValidationResult;

	public DestressGoal(Duck duck) {
		this.duck = duck;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	public boolean isRunning() {
		return this.isRunning;
	}

	@Override
	public boolean canUse() {
		int stress = duck.getDuckStress();
		if (stress < STRESS_THRESHOLD) {
			return false;
		}

		// If already in water, just check if it's a valid body
		if (duck.isInWater()) {
			return isWaterBodyValid(duck.blockPosition());
		}

		// Probabilistic triggering - gives other goals a chance to run
		// At max stress, always try to find water. At lower levels, random chance.
		if (stress < MAX_STRESS) {
			// Probability increases with stress: 6.25% at stress 3, up to 43.75% at stress 10
			float probability = (stress - STRESS_THRESHOLD + 1) / (float)(MAX_STRESS - STRESS_THRESHOLD + 1);
			probability /= 2;
			if (duck.getRandom().nextFloat() >= probability) {
				return false;
			}
		}

		// Try to find nearby accessible water
		return findNearestWater();
	}

	/**
	 * Checks if a position is in a valid (large enough) water body.
	 * Results are cached to avoid expensive flood-fill every tick.
	 */
	private boolean isWaterBodyValid(BlockPos pos) {
		long currentTick = duck.level().getGameTime();

		// Use cache if position is the same and cache is fresh
		if (pos.equals(lastValidatedPos) &&
			(currentTick - lastValidationTick) < WATER_VALIDATION_INTERVAL) {
			return lastValidationResult;
		}

		// Revalidate
		Set<BlockPos> waterBody = getWaterBodySize(pos);
		lastValidatedPos = pos.immutable();
		lastValidationTick = currentTick;
		lastValidationResult = waterBody.size() >= MIN_WATER_AREA;

		return lastValidationResult;
	}

	private boolean findNearestWater() {
		BlockPos duckPos = duck.blockPosition();
		BlockPos bestPos = null;
		double bestDistSq = Double.MAX_VALUE;
		Set<BlockPos> checkedWaterBodies = new HashSet<>();

		for (BlockPos pos : BlockPos.withinManhattan(duckPos, SEARCH_RANGE, SEARCH_RANGE / 2, SEARCH_RANGE)) {
			if (pos.getX() == duckPos.getX() && pos.getZ() == duckPos.getZ()) {
				continue;
			}

			BlockState state = duck.level().getBlockState(pos);
			BlockState aboveState = duck.level().getBlockState(pos.above());

			// Look for water with air above (so duck can swim on surface)
			if (state.is(Blocks.WATER) && aboveState.isAir()) {
				// Skip if we've already checked this water body
				if (checkedWaterBodies.contains(pos)) {
					continue;
				}

				// Check if water body is large enough
				Set<BlockPos> waterBody = getWaterBodySize(pos);
				checkedWaterBodies.addAll(waterBody);

				if (waterBody.size() >= MIN_WATER_AREA) {
					double distSq = pos.distSqr(duckPos);
					if (distSq < bestDistSq) {
						// Verify path exists before accepting this water body
						Path path = duck.getNavigation().createPath(pos, 1);
						if (path != null && path.canReach()) {
							bestDistSq = distSq;
							bestPos = pos.immutable();
						}
					}
				}
			}
		}

		if (bestPos != null) {
			this.targetWaterPos = bestPos;
			return true;
		}

		return false;
	}

	/**
	 * Uses flood-fill to find all connected surface water blocks (water with air above).
	 * Returns early once MIN_WATER_AREA is reached for efficiency.
	 */
	private Set<BlockPos> getWaterBodySize(BlockPos start) {
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		visited.add(start);

		while (!queue.isEmpty() && visited.size() < MIN_WATER_AREA) {
			BlockPos current = queue.poll();

			// Check all 4 horizontal neighbors
			for (BlockPos neighbor : new BlockPos[]{
				current.north(), current.south(), current.east(), current.west()
			}) {
				if (visited.contains(neighbor)) {
					continue;
				}

				BlockState state = duck.level().getBlockState(neighbor);
				BlockState aboveState = duck.level().getBlockState(neighbor.above());

				// Only count surface water (water with air above)
				if (state.is(Blocks.WATER) && aboveState.isAir()) {
					visited.add(neighbor);
					queue.add(neighbor);
				}
			}
		}

		return visited;
	}

	@Override
	public boolean canContinueToUse() {
		// Stop if no longer stressed
		if (duck.getDuckStress() <= 0) {
			return false;
		}

		if (duck.isInWater()) {
			// Periodically verify water body is still valid (e.g., player didn't drain it)
			if (!isWaterBodyValid(duck.blockPosition())) {
				return false;
			}
			// Stay in water for minimum duration or until fully destressed
			return ticksInWater < MIN_SWIM_DURATION || duck.getDuckStress() > 0;
		}

		// Not in water - check navigation timeout
		if (ticksNavigating >= NAVIGATION_TIMEOUT) {
			return false;
		}

		// Check if target water still exists (quick check, not full validation)
		if (targetWaterPos != null) {
			BlockState state = duck.level().getBlockState(targetWaterPos);
			if (!state.is(Blocks.WATER)) {
				// Water was removed, try to find new water
				if (!findNearestWater()) {
					return false;
				}
			}
		}

		// Continue if still navigating or can find new water
		return !duck.getNavigation().isDone() || findNearestWater();
	}

	@Override
	public void start() {
		isRunning = true;
		ticksInWater = 0;
		ticksRunning = 0;
		ticksNavigating = 0;

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
		ticksNavigating = 0;
		targetWaterPos = null;
		duck.getNavigation().stop();

		// Clear validation cache
		lastValidatedPos = null;
		lastValidationTick = 0;
		lastValidationResult = false;
	}

	@Override
	public void tick() {
		ticksRunning++;

		if (duck.isInWater()) {
			ticksInWater++;
			ticksNavigating = 0; // Reset navigation timer when in water

			// Reduce stress periodically while swimming
			if (ticksInWater % DESTRESS_INTERVAL == 0) {
				duck.reduceDuckStress(1);
			}

			// Occasionally swim around randomly in the water
			if (ticksInWater % (2 * Anatidaephobia.TICKS_PER_SECOND) == 0) {
				swimRandomly();
			}
		} else {
			ticksNavigating++;

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
