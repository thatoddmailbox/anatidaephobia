package dev.studer.alex.anatidaephobia.entity.ai.goal;

import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that allows ducks to socialize with each other to reduce loneliness.
 * Uses a mutual claiming pattern: when one duck initiates socializing, both ducks
 * store references to each other, blocking both from doing other movement goals.
 */
public class SocializeGoal extends Goal {
	private static final int LONELINESS_THRESHOLD = 3;
	private static final double SEARCH_RANGE = 10.0;
	private static final double TALK_DISTANCE = 2.0;
	private static final double MAX_CONTINUE_DISTANCE = 16.0;
	private static final int REDUCTION_INTERVAL = 40; // 2 seconds
	private static final int REDUCTION_AMOUNT = 1;
	private static final double SPEED_MODIFIER = 1.0;
	private static final int PARTICLE_INTERVAL = 20; // 1 second between particles

	private final Duck duck;
	private boolean isRunning;
	private int ticksRunning;
	private int ticksTalking; // Ticks spent within talk distance

	public SocializeGoal(Duck duck) {
		this.duck = duck;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	public boolean isRunning() {
		return this.isRunning;
	}

	@Override
	public boolean canUse() {
		// If we already have a partner (we were claimed by another duck), activate
		if (duck.getSocializePartner() != null) {
			return true;
		}

		// Only initiate if lonely enough
		if (duck.getDuckLoneliness() < LONELINESS_THRESHOLD) {
			return false;
		}

		// Try to find an eligible partner
		Duck partner = findEligiblePartner();
		if (partner != null) {
			claimPartner(partner);
			return true;
		}

		return false;
	}

	/**
	 * Find a nearby duck that is available for socializing.
	 * Eligible partners must:
	 * - Have loneliness > 0 (something to reduce)
	 * - Not already be socializing with someone
	 * - Not be panicking
	 * - Not be running stress/hungry/lonely expression goals
	 */
	private Duck findEligiblePartner() {
		AABB searchBox = duck.getBoundingBox().inflate(SEARCH_RANGE);
		List<Duck> nearbyDucks = duck.level().getEntitiesOfClass(
			Duck.class,
			searchBox,
			this::isEligiblePartner
		);

		// Find the closest eligible duck
		Duck closest = null;
		double closestDistSq = Double.MAX_VALUE;

		for (Duck candidate : nearbyDucks) {
			double distSq = duck.distanceToSqr(candidate);
			if (distSq < closestDistSq) {
				closestDistSq = distSq;
				closest = candidate;
			}
		}

		return closest;
	}

	private boolean isEligiblePartner(Duck other) {
		if (other == duck) {
			return false;
		}
		if (other.getDuckLoneliness() <= 0) {
			return false;
		}
		if (other.getSocializePartner() != null) {
			return false;
		}
		if (!other.isAvailableForSocializing()) {
			return false;
		}
		return true;
	}

	/**
	 * Claim a partner bidirectionally - both ducks store references to each other.
	 * This ensures the partner's SocializeGoal will also activate.
	 */
	private void claimPartner(Duck partner) {
		duck.setSocializePartner(partner);
		partner.setSocializePartner(duck);
	}

	/**
	 * Release the partner bidirectionally - clear references on both ducks.
	 */
	private void releasePartner() {
		Duck partner = duck.getSocializePartner();
		if (partner != null) {
			partner.setSocializePartner(null);
		}
		duck.setSocializePartner(null);
	}

	@Override
	public boolean canContinueToUse() {
		Duck partner = duck.getSocializePartner();

		// Partner must exist and be alive
		if (partner == null || !partner.isAlive()) {
			return false;
		}

		// Partner must still be paired with us (handles case where partner's goal was interrupted)
		if (partner.getSocializePartner() != duck) {
			return false;
		}

		// Partner must still be socializing
		// (this should be true if the above check passed, but just to make sure)
		if (partner.getDuckStateEnum() != Duck.DuckState.SOCIALIZING) {
			return false;
		}

		// Don't continue if too far apart
		if (duck.distanceToSqr(partner) > MAX_CONTINUE_DISTANCE * MAX_CONTINUE_DISTANCE) {
			return false;
		}

		// Continue only while both ducks still have loneliness
		return duck.getDuckLoneliness() > 0 && partner.getDuckLoneliness() > 0;
	}

	@Override
	public void start() {
		isRunning = true;
		ticksRunning = 0;
		ticksTalking = 0;
	}

	@Override
	public void stop() {
		isRunning = false;
		ticksRunning = 0;
		ticksTalking = 0;

		// Release partner bidirectionally
		releasePartner();

		duck.getNavigation().stop();
	}

	@Override
	public void tick() {
		ticksRunning++;

		Duck partner = duck.getSocializePartner();
		if (partner == null) {
			return;
		}

		double distanceSq = duck.distanceToSqr(partner);
		double distance = Math.sqrt(distanceSq);

		// Look at partner
		duck.getLookControl().setLookAt(partner, 30.0f, 30.0f);

		if (distance > TALK_DISTANCE) {
			// Walk towards partner
			ticksTalking = 0;
			duck.getNavigation().moveTo(partner, SPEED_MODIFIER);
		} else {
			// Within talk distance - stop moving and "talk"
			duck.getNavigation().stop();
			ticksTalking++;

			// Show particles periodically while chatting
			if (ticksTalking % PARTICLE_INTERVAL == 0) {
				duck.broadcastSocializeEvent();
			}

			// Reduce loneliness on both ducks periodically
			// Only the duck with lower ID reduces to avoid double-reduction
			if (ticksTalking % REDUCTION_INTERVAL == 0 && duck.getId() < partner.getId()) {
				duck.reduceDuckLoneliness(REDUCTION_AMOUNT);
				partner.reduceDuckLoneliness(REDUCTION_AMOUNT);
			}
		}
	}
}
