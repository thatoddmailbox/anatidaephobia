package dev.studer.alex.anatidaephobia.entity.ai.goal;

import com.mojang.logging.LogUtils;
import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.DuckNestManager;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;

public class NestGoal extends MoveToBlockGoal {
	private static final int NEST_DURATION = 2 * Anatidaephobia.TICKS_PER_SECOND;
	private static final int NEST_TIMEOUT = 2 * Anatidaephobia.TICKS_PER_SECOND;

	private final Duck duck;
	private boolean nesting = false;
	private int nestingTicks = 0;
	private int nestingTimeoutTicks = 0;

	protected boolean isRunning;

	public NestGoal(Duck duck, double speedModifier, int searchRange) {
		super(duck, speedModifier, searchRange);
		this.duck = duck;
	}

	public boolean isRunning() {
		return this.isRunning;
	}

	@Override
	public void start() {
		super.start();

		isRunning = true;

		LogUtils.getLogger().info("[{}] starting", duck.getUUID());
		DuckNestManager.claimNest(duck, this.blockPos);
	}

	@Override
	public void stop() {
		super.stop();

		isRunning = false;

		nesting = false;
		nestingTicks = 0;
		nestingTimeoutTicks = 0;

		LogUtils.getLogger().info("[{}] stopping", duck.getUUID());
		DuckNestManager.unclaimNest(duck);
	}

	@Override
	public void tick() {
		super.tick();

		if (this.isReachedTarget() && !nesting) {
			LogUtils.getLogger().info("[{}] reached goal", duck.getUUID());
			nesting = true;
			nestingTicks = 0;
		}

		if (nesting) {
			nestingTicks++;
			if (nestingTicks > NEST_DURATION) {
				LogUtils.getLogger().info("[{}] nested", duck.getUUID());
				// we did it

				if (this.duck.level() instanceof ServerLevel) {
					this.duck.spawnAtLocation((ServerLevel) this.duck.level(), this.duck.getRandomNestDrop());
				}
				this.duck.gameEvent(GameEvent.ENTITY_PLACE);

				nesting = false;
				nestingTicks = 0;
				nestingTimeoutTicks = 0;

				// Move away from nest
				double angle = this.duck.getRandom().nextDouble() * 2 * Math.PI;
				double distance = 3.0 + this.duck.getRandom().nextDouble() * 3.0;
				double targetX = Math.round(blockPos.getX() + Math.cos(angle) * distance);
				double targetZ = Math.round(blockPos.getZ() + Math.sin(angle) * distance);
				LogUtils.getLogger().info("[{}] go to {} {} {}", duck.getUUID(), targetX, blockPos.getY(), targetZ);
				this.duck.getNavigation().moveTo(targetX, blockPos.getY(), targetZ, this.speedModifier);

				blockPos = BlockPos.ZERO;
			}
		}
	}

	@Override
	public boolean canUse() {
		if (nestingTimeoutTicks < NEST_TIMEOUT) {
			nestingTimeoutTicks++;
			return false;
		}

		return super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
//			LogUtils.getLogger().info("[{}] canContinueToUse 1", duck.getUUID());
		if (super.canContinueToUse()) {
			return true;
		}

//			LogUtils.getLogger().info("[{}] canContinueToUse 2", duck.getUUID());
		if (blockPos != BlockPos.ZERO && isValidTarget(this.mob.level(), blockPos)) {
			// if we have somewhere to go, keep trying to go there (so we hold onto the nest claim)
			return true;
		}

//			LogUtils.getLogger().info("[{}] canContinueToUse 3", duck.getUUID());
		if (nesting && isValidTarget(this.mob.level(), blockPos)) {
			return true;
		}

//			LogUtils.getLogger().info("[{}] canContinueToUse 4", duck.getUUID());
		return false;
	}

	@Override
	protected int nextStartTick(PathfinderMob pathfinderMob) {
		// TODO: fix me
		return reducedTickDelay(40);
	}

//		@Override
//		public double acceptedDistance() {
//			// Ensure ducks go right on the hay
//			return 0.001;
//		}

	@Override
	protected boolean isValidTarget(LevelReader level, BlockPos pos) {
		// Only nest on hay bales with air above
		boolean isHay = level.getBlockState(pos).is(Blocks.HAY_BLOCK) &&
				level.getBlockState(pos.above()).isAir();
		if (!isHay) {
			return false;
		}

		if (!DuckNestManager.isNestFree(pos, duck.getUUID())) {
			return false;
		}

		return true;
	}
}
