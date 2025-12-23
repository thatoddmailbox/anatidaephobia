package dev.studer.alex.anatidaephobia.entity.ai.goal;

import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class StressGoal extends Goal {
	private static final int STRESS_THRESHOLD = 5;
	private static final int MAX_STRESS = 10;
	private static final int MAX_COOLDOWN = 4 * Anatidaephobia.TICKS_PER_SECOND;
	private static final double SPEED_MODIFIER = 1.6; // Faster than normal panic (1.4)
	private static final int STRESS_EVENT_INTERVAL = 7;

	private final Duck duck;
	private boolean isRunning;
	private int durationTicks;
	private int ticksRunning;
	private int cooldownTicks;
	private double targetX;
	private double targetY;
	private double targetZ;

	public StressGoal(Duck duck) {
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
			cooldownTicks = 0;
			return false;
		}

		if (cooldownTicks > 0) {
			cooldownTicks--;
			return false;
		}

		// At stress 10, always trigger. At lower levels, random chance.
		if (stress >= MAX_STRESS) {
			return findRandomPosition();
		}

		// Probability increases with stress
		float probability = (stress - STRESS_THRESHOLD + 1) / (float)(MAX_STRESS - STRESS_THRESHOLD + 1);
		if (duck.getRandom().nextFloat() < probability) {
			return findRandomPosition();
		}

		return false;
	}

	private boolean findRandomPosition() {
		Vec3 pos = DefaultRandomPos.getPos(this.duck, 8, 5);
		if (pos == null) {
			return false;
		}
		this.targetX = pos.x;
		this.targetY = pos.y;
		this.targetZ = pos.z;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return ticksRunning < durationTicks && duck.getDuckStress() >= STRESS_THRESHOLD;
	}

	@Override
	public void start() {
		isRunning = true;
		ticksRunning = 0;

		// Calculate duration based on stress - longer at higher stress
		int stress = duck.getDuckStress();
		float stressFactor = (stress - STRESS_THRESHOLD) / (float)(MAX_STRESS - STRESS_THRESHOLD);

		// Base duration: 3 seconds at stress 5, up to 8 seconds at stress 10
		int baseDuration = (int)(3 * Anatidaephobia.TICKS_PER_SECOND + stressFactor * 5 * Anatidaephobia.TICKS_PER_SECOND);

		// Add variance
		float varianceFactor = 1.0f - stressFactor;
		int maxVariance = 2 * Anatidaephobia.TICKS_PER_SECOND;
		int variance = (int)(duck.getRandom().nextFloat() * maxVariance * varianceFactor);

		durationTicks = baseDuration + variance;

		// Start moving to initial target
		duck.getNavigation().moveTo(targetX, targetY, targetZ, SPEED_MODIFIER);
	}

	@Override
	public void stop() {
		isRunning = false;
		ticksRunning = 0;
		durationTicks = 0;
		duck.getNavigation().stop();

		// Set cooldown based on stress
		int stress = duck.getDuckStress();
		if (stress >= MAX_STRESS) {
			cooldownTicks = 0;
		} else {
			float cooldownFactor = 1.0f - (stress - STRESS_THRESHOLD) / (float)(MAX_STRESS - STRESS_THRESHOLD);
			cooldownTicks = (int)(MAX_COOLDOWN * cooldownFactor);
		}
	}

	@Override
	public void tick() {
		ticksRunning++;

		// Broadcast stress event periodically
		if (ticksRunning % STRESS_EVENT_INTERVAL == 0) {
			duck.broadcastStressEvent();
		}

		// If we've reached our destination or got stuck, pick a new random position
		if (duck.getNavigation().isDone() || ticksRunning % (Anatidaephobia.TICKS_PER_SECOND) == 0) {
			if (findRandomPosition()) {
				duck.getNavigation().moveTo(targetX, targetY, targetZ, SPEED_MODIFIER);
			}
		}

		// Add frantic head movements while running
		if (duck.getRandom().nextInt(3) == 0) {
			float headOffset = (duck.getRandom().nextFloat() - 0.5f) * 40.0f;
			duck.setYHeadRot(duck.yBodyRot + headOffset);
		}
	}
}
