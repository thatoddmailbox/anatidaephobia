package dev.studer.alex.anatidaephobia.entity.ai.goal;

import com.mojang.logging.LogUtils;
import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.DuckNestManager;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class NestGoal extends Goal {
	public enum State {
		SEARCHING,
		MOVING,
		SETTLING,
		NESTING,
		LEAVING,
		FINISHED
	}

	// Distance thresholds
	private static final double CLOSE_ENOUGH_DISTANCE = 1.5;  // Switch to SETTLING
	private static final double ON_NEST_DISTANCE = 0.5;       // Switch to NESTING
	private static final double LEAVE_DISTANCE = 7;           // Must walk this far away
	private static final double POSITION_TOLERANCE = 0.1;     // Considered "same position"

	// Timing
	private static final int NEST_DURATION = 2 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int NEST_COOLDOWN = 2 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int PATH_RECALC_INTERVAL = 10;  // ticks
	private static final int SETTLE_TIMEOUT = 3 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int STUCK_THRESHOLD = 2 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int MOVE_TIMEOUT = 30 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int LEAVE_TIMEOUT = 5 * Anatidaephobia.TICKS_PER_SECOND;

	// Search parameters
	private static final int SEARCH_RANGE = 16;
	private static final int VERTICAL_SEARCH_RANGE = 8;

	private final Duck duck;
	private final double speedModifier;

	private State state = State.SEARCHING;
	private BlockPos targetNest = null;
	private int ticksSincePathCalc = 0;
	private int settlingTicks = 0;
	private int nestingTicks = 0;
	private int movingTicks = 0;
	private int leavingTicks = 0;
	private int stuckTicks = 0;
	private int cooldownTicks = 0;
	private Vec3 lastPosition = null;

	protected boolean isRunning;

	public NestGoal(Duck duck, double speedModifier, int searchRange) {
		this.duck = duck;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	public boolean isRunning() {
		return this.isRunning;
	}

	public State getState() {
		return this.state;
	}

	@Override
	public boolean isInterruptable() {
		// Only allow interruption when not actively nesting, and when not leaving (to ensure the duck clears the area first)
		return state != State.NESTING && state != State.SETTLING && state != State.LEAVING;
	}

	@Override
	public boolean canUse() {
		// Cooldown between nesting attempts
		if (cooldownTicks > 0) {
			cooldownTicks--;
			return false;
		}

		// Try to find a nest
		return findNearestNest();
	}

	@Override
	public boolean canContinueToUse() {
		// If we're finished or searching failed, stop
		if (state == State.FINISHED) {
			return false;
		}

		// LEAVING state always continues until done
		if (state == State.LEAVING) {
			return true;
		}

		// If we have no target and aren't actively searching, stop
		if (targetNest == null && state != State.SEARCHING) {
			return false;
		}

		// If the nest is no longer valid, stop
		if (targetNest != null && !isValidNest(targetNest)) {
			return false;
		}

		return true;
	}

	@Override
	public void start() {
		isRunning = true;
		state = State.MOVING;
		ticksSincePathCalc = 0;
		settlingTicks = 0;
		nestingTicks = 0;
		movingTicks = 0;
		stuckTicks = 0;
		lastPosition = null;

		LogUtils.getLogger().info("[{}] NestGoal starting, target: {}", duck.getUUID(), targetNest);
		DuckNestManager.claimNest(duck, targetNest);
		navigateToNest();
	}

	@Override
	public void stop() {
		isRunning = false;
		cooldownTicks = NEST_COOLDOWN;

		LogUtils.getLogger().info("[{}] NestGoal stopping", duck.getUUID());

		// Only unclaim if we didn't already (LEAVING state unclaims in tickNesting)
		if (state != State.LEAVING && state != State.FINISHED) {
			DuckNestManager.unclaimNest(duck);
		}

		// Reset state
		state = State.SEARCHING;
		targetNest = null;
		duck.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		switch (state) {
			case SEARCHING:
				// Shouldn't really be here during tick, but handle it
				if (!findNearestNest()) {
					state = State.FINISHED;
				} else {
					state = State.MOVING;
					DuckNestManager.claimNest(duck, targetNest);
					navigateToNest();
				}
				break;

			case MOVING:
				tickMoving();
				break;

			case SETTLING:
				tickSettling();
				break;

			case NESTING:
				tickNesting();
				break;

			case LEAVING:
				tickLeaving();
				break;

			case FINISHED:
				// Will cause canContinueToUse() to return false
				break;
		}
	}

	private void tickMoving() {
		movingTicks++;
		double dist = distanceToNestCenter();

		// Check if close enough to settle
		if (dist < CLOSE_ENOUGH_DISTANCE) {
			LogUtils.getLogger().info("[{}] close enough to settle, dist={}", duck.getUUID(), dist);
			state = State.SETTLING;
			settlingTicks = 0;
			stuckTicks = 0;
			return;
		}

		// Check for move timeout
		if (movingTicks > MOVE_TIMEOUT) {
			LogUtils.getLogger().info("[{}] move timeout, giving up", duck.getUUID());
			state = State.FINISHED;
			return;
		}

		// Check if stuck (no movement for STUCK_THRESHOLD ticks)
		if (isStuck()) {
			LogUtils.getLogger().info("[{}] stuck, forcing re-navigation", duck.getUUID());
			stuckTicks = 0;
			navigateToNest();
		}

		// Regular path recalculation, or if navigation reports done but we're not close
		ticksSincePathCalc++;
		if (ticksSincePathCalc >= PATH_RECALC_INTERVAL || duck.getNavigation().isDone()) {
			navigateToNest();
			ticksSincePathCalc = 0;
		}
	}

	private void tickSettling() {
		settlingTicks++;
		double dist = distanceToNestCenter();

		// Success: on the nest
		if (dist < ON_NEST_DISTANCE) {
			LogUtils.getLogger().info("[{}] on nest, starting nesting", duck.getUUID());
			state = State.NESTING;
			nestingTicks = 0;
			duck.getNavigation().stop();
			return;
		}

		// Pushed away: go back to MOVING
		if (dist > CLOSE_ENOUGH_DISTANCE * 1.5) {
			LogUtils.getLogger().info("[{}] pushed away from nest, going back to MOVING", duck.getUUID());
			state = State.MOVING;
			movingTicks = 0;
			stuckTicks = 0;
			navigateToNest();
			return;
		}

		// Still settling: use MoveControl for fine adjustment
		Vec3 nestCenter = getNestCenter();
		duck.getMoveControl().setWantedPosition(nestCenter.x, nestCenter.y, nestCenter.z, speedModifier);

		// Timeout: abandon this nest and try another
		if (settlingTicks > SETTLE_TIMEOUT) {
			LogUtils.getLogger().info("[{}] settle timeout, abandoning nest", duck.getUUID());
			DuckNestManager.unclaimNest(duck);
			targetNest = null;

			// Try to find another nest
			if (findNearestNest()) {
				state = State.MOVING;
				movingTicks = 0;
				stuckTicks = 0;
				DuckNestManager.claimNest(duck, targetNest);
				navigateToNest();
			} else {
				state = State.FINISHED;
			}
		}
	}

	private void tickNesting() {
		nestingTicks++;

		// Check if we got pushed off
		double dist = distanceToNestCenter();
		if (dist > ON_NEST_DISTANCE * 2) {
			LogUtils.getLogger().info("[{}] pushed off during nesting, going back to SETTLING", duck.getUUID());
			state = State.SETTLING;
			settlingTicks = 0;
			return;
		}

		if (nestingTicks > NEST_DURATION) {
			LogUtils.getLogger().info("[{}] nesting complete!", duck.getUUID());

			// Spawn the nest drop
			if (duck.level() instanceof ServerLevel serverLevel) {
				duck.spawnAtLocation(serverLevel, duck.getRandomNestDrop());
			}
			duck.gameEvent(GameEvent.ENTITY_PLACE);

			// Update duck stats
			duck.addDuckXP(1);
			duck.addDuckStress(Mth.ceil(duck.getDuckLevel() / 2.0f));
			duck.addDuckLoneliness(1);

			// Unclaim the nest now so other ducks can use it
			DuckNestManager.unclaimNest(duck);

			// Move away from nest
			moveAwayFromNest();
			state = State.LEAVING;
			leavingTicks = 0;
		}
	}

	private void tickLeaving() {
		leavingTicks++;
		double dist = distanceToNestCenter();

		// Successfully moved away
		if (dist >= LEAVE_DISTANCE && duck.getNavigation().isDone()) {
			LogUtils.getLogger().info("[{}] left nest area", duck.getUUID());
			state = State.FINISHED;
			return;
		}

		// Timeout - just finish anyway
		if (leavingTicks > LEAVE_TIMEOUT) {
			LogUtils.getLogger().info("[{}] leave timeout, finishing anyway", duck.getUUID());
			state = State.FINISHED;
			return;
		}

		// If navigation stopped but we're not far enough, try again
		if (duck.getNavigation().isDone()) {
			moveAwayFromNest();
		}
	}

	private void moveAwayFromNest() {
		// TODO: should avoid other nests?
		double angle = duck.getRandom().nextDouble() * 2 * Math.PI;
		double distance = LEAVE_DISTANCE + duck.getRandom().nextDouble() * 3.0;
		double targetX = targetNest.getX() + 0.5 + Math.cos(angle) * distance;
		double targetZ = targetNest.getZ() + 0.5 + Math.sin(angle) * distance;
		LogUtils.getLogger().info("[{}] moving away to {}, {}", duck.getUUID(), targetX, targetZ);
		duck.getNavigation().moveTo(targetX, targetNest.getY() + 1, targetZ, speedModifier);
	}

	private void navigateToNest() {
		if (targetNest == null) return;
		Vec3 center = getNestCenter();
		duck.getNavigation().moveTo(center.x, center.y, center.z, speedModifier);
	}

	private Vec3 getNestCenter() {
		// Stand ON the block (Y + 1), at the center (X + 0.5, Z + 0.5)
		return new Vec3(targetNest.getX() + 0.5, targetNest.getY() + 1, targetNest.getZ() + 0.5);
	}

	private double distanceToNestCenter() {
		if (targetNest == null) return Double.MAX_VALUE;
		Vec3 nestCenter = getNestCenter();
		return duck.position().distanceTo(nestCenter);
	}

	private boolean isStuck() {
		Vec3 currentPos = duck.position();
		if (lastPosition != null) {
			double distSqr = currentPos.distanceToSqr(lastPosition);
			if (distSqr < POSITION_TOLERANCE * POSITION_TOLERANCE) {
				stuckTicks++;
			} else {
				stuckTicks = 0;
			}
		}
		lastPosition = currentPos;
		return stuckTicks > STUCK_THRESHOLD;
	}

	private boolean findNearestNest() {
		BlockPos mobPos = duck.blockPosition();
		BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

		// Search in expanding pattern similar to MoveToBlockGoal
		for (int y = 0; y <= VERTICAL_SEARCH_RANGE; y = y > 0 ? -y : 1 - y) {
			for (int r = 0; r < SEARCH_RANGE; r++) {
				for (int x = 0; x <= r; x = x > 0 ? -x : 1 - x) {
					for (int z = (x < r && x > -r) ? r : 0; z <= r; z = z > 0 ? -z : 1 - z) {
						checkPos.setWithOffset(mobPos, x, y, z);
						if (isValidNest(checkPos)) {
							targetNest = checkPos.immutable();
							return true;
						}
					}
				}
			}
		}

		targetNest = null;
		return false;
	}

	private boolean isValidNest(BlockPos pos) {
		LevelReader level = duck.level();
		if (!DuckNestManager.isCentralBlockOfValidNest(level, pos)) {
			return false;
		}
		if (!DuckNestManager.isNestFree(pos, duck.getUUID())) {
			return false;
		}
		return true;
	}
}
