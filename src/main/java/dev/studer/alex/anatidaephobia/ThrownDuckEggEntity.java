package dev.studer.alex.anatidaephobia;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownDuckEggEntity extends ThrowableItemProjectile {
	private static final EntityDimensions ZERO_SIZED_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);

	public ThrownDuckEggEntity(EntityType<ThrownDuckEggEntity> type, Level level) {
		super(type, level);
	}

	public ThrownDuckEggEntity(final Level level, final LivingEntity mob, final ItemStack itemStack) {
		super(AnatidaephobiaEntities.THROWN_DUCK_EGG, mob, level, itemStack);
	}

	public ThrownDuckEggEntity(final Level level, final double x, final double y, final double z, final ItemStack itemStack) {
		super(AnatidaephobiaEntities.THROWN_DUCK_EGG, x, y, z, level, itemStack);
	}

	public void handleEntityEvent(final byte id) {
		if (id == 3) {
			double v = 0.08;

			for(int i = 0; i < 8; ++i) {
				this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), ((double)this.random.nextFloat() - (double)0.5F) * 0.08, ((double)this.random.nextFloat() - (double)0.5F) * 0.08, ((double)this.random.nextFloat() - (double)0.5F) * 0.08);
			}
		}
	}

	protected void onHitEntity(final EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		hitResult.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
	}

	protected void onHit(final HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide()) {
			if (this.random.nextInt(3) == 0) {
				int count = 1;
				if (this.random.nextInt(16) == 0) {
					count = 4;
				}

				for(int i = 0; i < count; ++i) {
					DuckEntity duck = AnatidaephobiaEntities.DUCK.create(this.level(),  EntitySpawnReason.TRIGGERED);
					if (duck != null) {
						duck.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
						if (!duck.fudgePositionAfterSizeChange(ZERO_SIZED_DIMENSIONS)) {
							break;
						}

						this.level().addFreshEntity(duck);
					}
				}
			}

			this.level().broadcastEntityEvent(this, (byte)3);
			this.discard();
		}

	}

	protected Item getDefaultItem() {
		return AnatidaephobiaItems.DUCK_EGG;
	}
}
