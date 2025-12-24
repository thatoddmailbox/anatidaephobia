package dev.studer.alex.anatidaephobia.entity;

import dev.studer.alex.anatidaephobia.AnatidaephobiaBlocks;
import dev.studer.alex.anatidaephobia.AnatidaephobiaEntities;
import dev.studer.alex.anatidaephobia.AnatidaephobiaItems;
import dev.studer.alex.anatidaephobia.world.level.portal.DuckyPortalShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

public class ThrownDuckEgg extends ThrowableItemProjectile {
	private static final EntityDimensions ZERO_SIZED_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);

	public ThrownDuckEgg(EntityType<ThrownDuckEgg> type, Level level) {
		super(type, level);
	}

	public ThrownDuckEgg(final Level level, final LivingEntity mob, final ItemStack itemStack) {
		super(AnatidaephobiaEntities.THROWN_DUCK_EGG, mob, level, itemStack);
	}

	public ThrownDuckEgg(final Level level, final double x, final double y, final double z, final ItemStack itemStack) {
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
			// Check if we hit inside a quackmium portal frame
			if (tryActivateDuckyPortal(hitResult)) {
				// Portal was activated, just discard the egg
				this.level().broadcastEntityEvent(this, (byte)3);
				this.discard();
				return;
			}

			// Normal duck egg behavior - chance to spawn ducks
			if (this.random.nextInt(3) == 0) {
				int count = 1;
				if (this.random.nextInt(16) == 0) {
					count = 4;
				}

				for(int i = 0; i < count; ++i) {
					Duck duck = AnatidaephobiaEntities.DUCK.create(this.level(), EntitySpawnReason.TRIGGERED);
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

	/**
	 * Try to activate a Ducky Portal at the hit location.
	 * Returns true if a portal was successfully created.
	 */
	private boolean tryActivateDuckyPortal(HitResult hitResult) {
		BlockPos hitPos;

		if (hitResult instanceof BlockHitResult blockHit) {
			// If we hit a block, check the position we hit and the air block in front
			hitPos = blockHit.getBlockPos();

			// If we hit a quackmium block, check the air block adjacent to it
			if (this.level().getBlockState(hitPos).is(AnatidaephobiaBlocks.QUACKMIUM_BLOCK)) {
				hitPos = hitPos.relative(blockHit.getDirection());
			}
		} else {
			// For entity hits or other types, use the entity position
			hitPos = BlockPos.containing(this.getX(), this.getY(), this.getZ());
		}

		// Check if this position is inside an empty quackmium portal frame
		if (!this.level().getBlockState(hitPos).isAir()) {
			return false;
		}

		// Check for nearby quackmium blocks to determine if we're in a potential portal frame
		boolean hasQuackmiumNearby = false;
		for (Direction dir : Direction.values()) {
			if (this.level().getBlockState(hitPos.relative(dir)).is(AnatidaephobiaBlocks.QUACKMIUM_BLOCK)) {
				hasQuackmiumNearby = true;
				break;
			}
		}

		if (!hasQuackmiumNearby) {
			return false;
		}

		// Try to find and activate a portal shape
		Optional<DuckyPortalShape> portalShape = DuckyPortalShape.findEmptyPortalShape(
				this.level(), hitPos, Direction.Axis.X
		);

		if (portalShape.isPresent()) {
			portalShape.get().createPortalBlocks(this.level());

			// Play activation sound
			this.level().playSound(
					null,
					hitPos,
					SoundEvents.END_PORTAL_SPAWN,
					SoundSource.BLOCKS,
					1.0F,
					1.0F
			);

			return true;
		}

		return false;
	}

	protected Item getDefaultItem() {
		return AnatidaephobiaItems.DUCK_EGG;
	}
}
