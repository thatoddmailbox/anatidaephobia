package dev.studer.alex.anatidaephobia.entity.ai.goal;

import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class LonelyGoal extends Goal {
	private static final int LONELINESS_THRESHOLD = 5;
	private static final int MAX_LONELINESS = 10;
	private static final int MAX_COOLDOWN = 8 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int JUMP_INTERVAL_TICKS = 6; // Jump every 6 ticks (~3 jumps per second)
	private static final float JUMP_STRENGTH = 0.45f;
	private static final int LAND_SEARCH_RANGE = 10;
	private static final int NAVIGATION_TIMEOUT = 5 * Anatidaephobia.TICKS_PER_SECOND;
	private static final double SPEED_MODIFIER = 1.2;

	private final Duck duck;
	private boolean isRunning;
	private int durationTicks;
	private int ticksRunning;
	private int cooldownTicks;
	private float lookAroundYaw;
	private int lookAroundDirection;
	private boolean isNavigatingToLand;
	private BlockPos targetLandPos;
	private int ticksNavigating;

	public LonelyGoal(Duck duck) {
		this.duck = duck;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
	}

	public boolean isRunning() {
		return this.isRunning;
	}

	@Override
	public boolean canUse() {
		int loneliness = duck.getDuckLoneliness();
		if (loneliness < LONELINESS_THRESHOLD) {
			cooldownTicks = 0;
			return false;
		}

		if (cooldownTicks > 0) {
			cooldownTicks--;
			return false;
		}

		// At loneliness 10, always trigger. At lower levels, random chance.
		if (loneliness >= MAX_LONELINESS) {
			return true;
		}

		// Probability increases with loneliness
		float probability = (loneliness - LONELINESS_THRESHOLD + 1) / (float)(MAX_LONELINESS - LONELINESS_THRESHOLD + 1);
		return duck.getRandom().nextFloat() < probability;
	}

	@Override
	public boolean canContinueToUse() {
		return ticksRunning < durationTicks && duck.getDuckLoneliness() >= LONELINESS_THRESHOLD;
	}

	@Override
	public void start() {
		isRunning = true;
		ticksRunning = 0;
		ticksNavigating = 0;
		isNavigatingToLand = false;
		targetLandPos = null;

		// Calculate duration based on loneliness
		int loneliness = duck.getDuckLoneliness();
		float lonelinessFactor = (loneliness - LONELINESS_THRESHOLD) / (float)(MAX_LONELINESS - LONELINESS_THRESHOLD);

		// Base duration: 2 seconds at loneliness 5, up to 6 seconds at loneliness 10
		int baseDuration = (int)(2 * Anatidaephobia.TICKS_PER_SECOND + lonelinessFactor * 4 * Anatidaephobia.TICKS_PER_SECOND);

		// Add variance
		float varianceFactor = 1.0f - lonelinessFactor;
		int maxVariance = 2 * Anatidaephobia.TICKS_PER_SECOND;
		int variance = (int)(duck.getRandom().nextFloat() * maxVariance * varianceFactor);

		durationTicks = baseDuration + variance;

		// Initialize looking around
		lookAroundYaw = duck.yBodyRot;
		lookAroundDirection = duck.getRandom().nextBoolean() ? 1 : -1;

		// If in water, try to find land first
		if (duck.isInWater()) {
			targetLandPos = findNearbyLand();
			if (targetLandPos != null) {
				isNavigatingToLand = true;
				duck.getNavigation().moveTo(
					targetLandPos.getX() + 0.5,
					targetLandPos.getY(),
					targetLandPos.getZ() + 0.5,
					SPEED_MODIFIER
				);
			}
			// If no land found, we'll just stay in water with particles
		} else {
			// Stop movement if already on land
			duck.getNavigation().stop();
		}
	}

	private BlockPos findNearbyLand() {
		// Use LandRandomPos to find a valid land position
		Vec3 landPos = LandRandomPos.getPos(duck, LAND_SEARCH_RANGE, 4);
		if (landPos != null) {
			return BlockPos.containing(landPos);
		}
		return null;
	}

	@Override
	public void stop() {
		isRunning = false;
		ticksRunning = 0;
		durationTicks = 0;
		isNavigatingToLand = false;
		targetLandPos = null;
		ticksNavigating = 0;
		duck.getNavigation().stop();

		// Set cooldown based on loneliness
		int loneliness = duck.getDuckLoneliness();
		if (loneliness >= MAX_LONELINESS) {
			cooldownTicks = 0;
		} else {
			float cooldownFactor = 1.0f - (loneliness - LONELINESS_THRESHOLD) / (float)(MAX_LONELINESS - LONELINESS_THRESHOLD);
			cooldownTicks = (int)(MAX_COOLDOWN * cooldownFactor);
		}
	}

	@Override
	public void tick() {
		ticksRunning++;

		// Always broadcast particles periodically, regardless of phase
		if (ticksRunning % (JUMP_INTERVAL_TICKS * 2) == 0 && duck.getRandom().nextInt(3) == 0) {
			duck.broadcastLonelyEvent();
		}

		// Handle navigation to land phase
		if (isNavigatingToLand) {
			ticksNavigating++;

			// Check if we've reached land
			if (!duck.isInWater() && duck.onGround()) {
				isNavigatingToLand = false;
				targetLandPos = null;
				ticksNavigating = 0;
				duck.getNavigation().stop();
				// Reset look direction for jumping phase
				lookAroundYaw = duck.yBodyRot;
			}
			// Check for navigation timeout or if navigation is done but still in water
			else if (ticksNavigating >= NAVIGATION_TIMEOUT ||
					 (duck.getNavigation().isDone() && targetLandPos != null)) {
				// Try to find a new land position
				BlockPos newLandPos = findNearbyLand();
				if (newLandPos != null && !newLandPos.equals(targetLandPos)) {
					targetLandPos = newLandPos;
					ticksNavigating = 0;
					duck.getNavigation().moveTo(
						targetLandPos.getX() + 0.5,
						targetLandPos.getY(),
						targetLandPos.getZ() + 0.5,
						SPEED_MODIFIER
					);
				} else {
					// Give up navigating, just stay in water with particles
					isNavigatingToLand = false;
					targetLandPos = null;
				}
			}

			// Do looking around behavior while navigating too
			doLookAroundBehavior();
			return;
		}

		// Not navigating - do the jumping behavior (or just particles if stuck in water)
		if (duck.isInWater()) {
			// Stuck in water with no land nearby - just do looking around with particles
			doLookAroundBehavior();
			return;
		}

		// On land - do the full jumping behavior
		// Stop horizontal movement but allow jumping
		duck.setDeltaMovement(0, duck.getDeltaMovement().y, 0);
		duck.getNavigation().stop();

		// Jump up and down rapidly when on ground
		if (duck.onGround() && ticksRunning % JUMP_INTERVAL_TICKS == 0) {
			duck.setDeltaMovement(duck.getDeltaMovement().x, JUMP_STRENGTH, duck.getDeltaMovement().z);
			duck.hurtMarked = true;

			// Broadcast sad event for particles on some jumps
			if (duck.getRandom().nextInt(3) == 0) {
				duck.broadcastLonelyEvent();
			}
		}

		doLookAroundBehavior();

		// Small body wobble while jumping (shows distress)
		if (!duck.onGround()) {
			float wobble = (ticksRunning % 2 == 0 ? 1 : -1) * 5.0f;
			duck.yBodyRot += wobble;
		}
	}

	private void doLookAroundBehavior() {
		// Look around frantically, searching for companions
		// Sweep head back and forth more and more frantically at higher loneliness
		int loneliness = duck.getDuckLoneliness();
		float sweepSpeed = 5.0f + (loneliness - LONELINESS_THRESHOLD) * 2.0f;
		float maxSweepAngle = 60.0f + (loneliness - LONELINESS_THRESHOLD) * 10.0f;

		lookAroundYaw += lookAroundDirection * sweepSpeed;

		// Reverse direction at extremes
		float yawDiff = lookAroundYaw - duck.yBodyRot;
		if (Math.abs(yawDiff) > maxSweepAngle) {
			lookAroundDirection *= -1;
		}

		duck.setYHeadRot(lookAroundYaw);

		// Occasionally look up hopefully, then down sadly
		if (ticksRunning % 15 == 0) {
			// Look up
			duck.setXRot(-25.0f);
		} else if (ticksRunning % 15 == 7) {
			// Look down sadly
			duck.setXRot(20.0f);
		} else if (ticksRunning % 15 == 10) {
			// Back to neutral
			duck.setXRot(0);
		}
	}
}
