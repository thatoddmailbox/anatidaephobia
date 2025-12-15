package dev.studer.alex.anatidaephobia;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

public class DuckEntity extends PathfinderMob {
	public DuckEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
//		this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
	}

	@Override
	public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
		boolean result = super.hurtServer(serverLevel, damageSource, f);

		if (result) {
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

					player.sendSystemMessage(Component.translatable("biome.minecraft.basalt_deltas"));
				}
			}
		}

		return result;
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
