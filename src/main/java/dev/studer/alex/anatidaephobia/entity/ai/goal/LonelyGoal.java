package dev.studer.alex.anatidaephobia.entity.ai.goal;

import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class LonelyGoal extends Goal {
	private static final int LONELINESS_THRESHOLD = 5;
	private static final int MAX_LONELINESS = 10;
	private static final int MAX_COOLDOWN = 8 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int JUMP_INTERVAL_TICKS = 6; // Jump every 6 ticks (~3 jumps per second)
	private static final float JUMP_STRENGTH = 0.45f;

	private final Duck duck;
	private boolean isRunning;
	private int durationTicks;
	private int ticksRunning;
	private int cooldownTicks;
	private float lookAroundYaw;
	private int lookAroundDirection;

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

		// Stop movement
		duck.getNavigation().stop();
	}

	@Override
	public void stop() {
		isRunning = false;
		ticksRunning = 0;
		durationTicks = 0;

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

		// Small body wobble while jumping (shows distress)
		if (!duck.onGround()) {
			float wobble = (ticksRunning % 2 == 0 ? 1 : -1) * 5.0f;
			duck.yBodyRot += wobble;
		}
	}
}
