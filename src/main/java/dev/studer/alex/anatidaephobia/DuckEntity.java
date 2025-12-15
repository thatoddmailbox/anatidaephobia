package dev.studer.alex.anatidaephobia;

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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class DuckEntity extends PathfinderMob {
	private byte EVENT_ID_LOVE = 100;

	public DuckEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new TemptGoal(this, 1.0, itemStack -> itemStack.is(AnatidaephobiaItems.DUCK_FOOD), false));
		this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0));
//		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
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
}
