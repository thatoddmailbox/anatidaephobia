package dev.studer.alex.anatidaephobia;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;

public class DuckEntity extends PathfinderMob {
	private byte EVENT_ID_LOVE = 100;

	public DuckEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
		// Make the nametag always visible
		this.setCustomNameVisible(true);
	}

	@Override
	public Component getName() {
		// Generate a dynamic nametag showing duck name + state
		String duckName = generateDuckName();
		String state = getDuckState();
		return Component.literal(duckName + " [" + state + "]");
	}

	private String generateDuckName() {
		// Generate a name based on UUID to keep it consistent
		String[] names = {"Quackers", "Waddles", "Donald", "Daffy", "Howard", "Mallard", "Puddles"};
		int index = Math.abs(this.getUUID().hashCode()) % names.length;
		return names[index];
	}

	private String getDuckState() {
		// Show current state - you can customize this to show whatever you want
		if (this.isDeadOrDying()) {
			return "Dead";
		}

		return "Cool";
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
		this.goalSelector.addGoal(2, new TemptGoal(this, 1.0, itemStack -> itemStack.is(AnatidaephobiaItems.DUCK_FOOD), false));
		this.goalSelector.addGoal(3, new NestGoal(this, 1.0, 16));
		this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0));
//		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
	}

	@Override
	public boolean canBeLeashed() {
		// TODO: it would be funny if this worked?
		return false;
	}

	@Override
	public void handleEntityEvent(final byte id) {
		if (id == EVENT_ID_LOVE) {
			for(int i = 0; i < 7; ++i) {
				double xa = this.random.nextGaussian() * 0.02;
				double ya = this.random.nextGaussian() * 0.02;
				double za = this.random.nextGaussian() * 0.02;
				this.level().addParticle(ParticleTypes.HEART, this.getRandomX((double)1.0F), this.getRandomY() + (double)0.5F, this.getRandomZ((double)1.0F), xa, ya, za);
			}
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		DuckNestManager.unclaimNest(this);
		super.remove(reason);
	}

	@Override
	public InteractionResult mobInteract(final Player player, final InteractionHand hand) {
		ItemStack itemStack = player.getItemInHand(hand);
		if (itemStack.is(Items.BREAD)) {
			if (player instanceof ServerPlayer) {
				ServerPlayer serverPlayer = (ServerPlayer) player;
				serverPlayer.sendSystemMessage(Component.translatable("message.anatidaephobia.duck_hurt"));

				this.usePlayerItem(player, hand, itemStack);
				this.level().broadcastEntityEvent(this, EVENT_ID_LOVE);

				return InteractionResult.SUCCESS_SERVER;
			}

			if (this.level().isClientSide()) {
				return InteractionResult.CONSUME;
			}
		}

		return super.mobInteract(player, hand);
	}

	@Override
	public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
		boolean result = super.hurtServer(serverLevel, damageSource, f);

		// If we're dying then we already would have triggered the behavior in die() so skip this code
		if (result && !isDeadOrDying()) {
			if (damageSource.getEntity() != null) {
				if (damageSource.getEntity() instanceof ServerPlayer) {
					ServerPlayer player = (ServerPlayer) damageSource.getEntity();

					// Strike the player down with lightning.
					// We make it visual-only and call thunderHit ourselves manually.
					// This is so we don't get collateral damage (such as, importantly, the duck!)
					LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
					bolt.snapTo(player.getX(), player.getY(), player.getZ());
					bolt.setVisualOnly(true);
					serverLevel.addFreshEntity(bolt);
					player.thunderHit(serverLevel, bolt);

					player.sendSystemMessage(Component.translatable("message.anatidaephobia.duck_hurt"));
				}
			}
		}

		return result;
	}

	@Override
	public void die(DamageSource damageSource) {
		if (damageSource.getEntity() instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer) damageSource.getEntity();

			// TODO: lightning storm?
			player.level().setWeatherParameters(0, 10*Anatidaephobia.TICKS_PER_SECOND, true, true);

			player.sendSystemMessage(Component.translatable("message.anatidaephobia.duck_death"));
		}

		super.die(damageSource);
	}

	public static AttributeSupplier.Builder createMobAttributes() {
		return Animal.createAnimalAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.25F)
				.add(Attributes.MAX_HEALTH, 10.0)
				.add(Attributes.ATTACK_DAMAGE, 2.0)
				.add(Attributes.SAFE_FALL_DISTANCE, 5.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	public class NestGoal extends MoveToBlockGoal {
		private static final int NEST_DURATION = 2 * Anatidaephobia.TICKS_PER_SECOND;
		private static final int NEST_TIMEOUT = 2 * Anatidaephobia.TICKS_PER_SECOND;

		private final DuckEntity duck;
		private boolean nesting = false;
		private int nestingTicks = 0;
		private int nestingTimeoutTicks = 0;

		public NestGoal(DuckEntity duck, double speedModifier, int searchRange) {
			super(duck, speedModifier, searchRange);
			this.duck = duck;
		}

		@Override
		public void start() {
			super.start();

			LogUtils.getLogger().info("[{}] starting", duck.getUUID());
			DuckNestManager.claimNest(duck, this.blockPos);
		}

		@Override
		public void stop() {
			super.stop();

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

					// TODO: should lay a special golden duck egg or something?
					if (this.duck.level() instanceof ServerLevel) {
						this.duck.spawnAtLocation((ServerLevel) this.duck.level(), new ItemStack(Items.EGG));
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
}
