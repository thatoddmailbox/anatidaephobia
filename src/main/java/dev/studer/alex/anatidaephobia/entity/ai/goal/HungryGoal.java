package dev.studer.alex.anatidaephobia.entity.ai.goal;

import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class HungryGoal extends Goal {
	private static final int HUNGER_THRESHOLD = 5;
	private static final int MAX_HUNGER = 10;
	private static final int MAX_COOLDOWN = 6 * Anatidaephobia.TICKS_PER_SECOND; // 6 seconds at hunger 5

	private final Duck duck;
	private boolean isRunning;
	private int durationTicks;
	private int ticksRunning;
	private int cooldownTicks;

	public HungryGoal(Duck duck) {
		this.duck = duck;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	public boolean isRunning() {
		return this.isRunning;
	}

	@Override
	public boolean canUse() {
		int hunger = duck.getDuckHunger();
		if (hunger < HUNGER_THRESHOLD) {
			cooldownTicks = 0;
			return false;
		}

		// At hunger 10, no cooldown. At hunger 5, full cooldown.
		if (cooldownTicks > 0) {
			cooldownTicks--;
			return false;
		}

		// At hunger 10, always trigger (100%). At lower levels, random chance.
		if (hunger >= MAX_HUNGER) {
			return true;
		}

		// Probability increases with hunger: ~17% at 5, 100% at 10
		float probability = (hunger - HUNGER_THRESHOLD + 1) / (float)(MAX_HUNGER - HUNGER_THRESHOLD + 1);
		return duck.getRandom().nextFloat() < probability;
	}

	@Override
	public boolean canContinueToUse() {
		return ticksRunning < durationTicks && duck.getDuckHunger() >= HUNGER_THRESHOLD;
	}

	@Override
	public void start() {
		isRunning = true;
		ticksRunning = 0;

		// Calculate duration based on hunger
		int hunger = duck.getDuckHunger();
		float hungerFactor = (hunger - HUNGER_THRESHOLD) / (float)(MAX_HUNGER - HUNGER_THRESHOLD);

		// Base duration: 2 seconds at hunger 5, up to 5 seconds at hunger 10
		int baseDuration = (int)(2 * Anatidaephobia.TICKS_PER_SECOND + hungerFactor * 3 * Anatidaephobia.TICKS_PER_SECOND);

		// Variance decreases with hunger: full variance at 5, none at 10
		float varianceFactor = 1.0f - hungerFactor;
		int maxVariance = 2 * Anatidaephobia.TICKS_PER_SECOND;
		int variance = (int)(duck.getRandom().nextFloat() * maxVariance * varianceFactor);

		durationTicks = baseDuration + variance;

		// Stop movement
		duck.getNavigation().stop();
	}

	@Override
	public void stop() {
		isRunning = false;
		ticksRunning = 0;
		durationTicks = 0;

		// Set cooldown based on hunger: full cooldown at 5, no cooldown at 10
		int hunger = duck.getDuckHunger();
		if (hunger >= MAX_HUNGER) {
			cooldownTicks = 0;
		} else {
			float cooldownFactor = 1.0f - (hunger - HUNGER_THRESHOLD) / (float)(MAX_HUNGER - HUNGER_THRESHOLD);
			cooldownTicks = (int)(MAX_COOLDOWN * cooldownFactor);
		}
	}

	@Override
	public void tick() {
		ticksRunning++;

		// Stop any movement
		duck.setDeltaMovement(0, duck.getDeltaMovement().y, 0);
		duck.getNavigation().stop();

		// Violent shaking - rapidly oscillate body and head rotation
		float shakeAmount = 15.0f; // degrees
		float shakeOffset = (ticksRunning % 2 == 0 ? 1 : -1) * shakeAmount;

		// Add some randomness to the shake
		shakeOffset += (duck.getRandom().nextFloat() - 0.5f) * 10.0f;

		duck.yBodyRot = duck.yBodyRot + shakeOffset;
		duck.setYHeadRot(duck.yBodyRot + (duck.getRandom().nextFloat() - 0.5f) * 30.0f);

		// Occasionally look up/down frantically
		if (duck.getRandom().nextInt(3) == 0) {
			duck.setXRot((duck.getRandom().nextFloat() - 0.5f) * 40.0f);
		}
	}
}
