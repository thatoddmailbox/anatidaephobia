package dev.studer.alex.anatidaephobia.mixin;

import dev.studer.alex.anatidaephobia.AnatidaephobiaEntities;
import dev.studer.alex.anatidaephobia.world.level.DuckShrine;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
	@Shadow
	public abstract Player getPlayerOwner();

	@Shadow
	private int nibble;

	@Inject(method = "retrieve", at = @At("HEAD"))
	private void onRetrieve(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
		FishingHook self = (FishingHook) (Object) this;
		Level level = self.level();

		// Only run on server side
		if (level.isClientSide()) {
			return;
		}

		// Check if there's a fish biting (nibble > 0 means fish is on the line)
		if (this.nibble <= 0) {
			return;
		}

		Player owner = this.getPlayerOwner();
		if (!(owner instanceof ServerPlayer serverPlayer)) {
			return;
		}

		// Check if bobber is in a valid duck shrine
		BlockPos bobberPos = self.blockPosition();
		if (!DuckShrine.isInValidShrine(level, bobberPos)) {
			return;
		}

		// Check player cooldown
		if (DuckShrine.isOnCooldown(serverPlayer)) {
			DuckShrine.sendCooldownMessage(serverPlayer);
			return;
		}

		// All conditions met - summon a duck!
		Duck duck = AnatidaephobiaEntities.DUCK.create(level, EntitySpawnReason.TRIGGERED);
		if (duck != null) {
			// Position the duck at the bobber location
			duck.snapTo(self.getX(), self.getY(), self.getZ(), self.getYRot(), 0.0F);

			// Give the duck some velocity toward the player (like a caught fish)
			Vec3 toPlayer = new Vec3(
					owner.getX() - self.getX(),
					owner.getY() - self.getY(),
					owner.getZ() - self.getZ()
			);
			duck.setDeltaMovement(
					toPlayer.x * 0.1,
					toPlayer.y * 0.1 + Math.sqrt(toPlayer.length()) * 0.08,
					toPlayer.z * 0.1
			);

			level.addFreshEntity(duck);

			// Mark cooldown and send success message
			DuckShrine.markSummoned(serverPlayer);
			DuckShrine.sendSuccessMessage(serverPlayer);
		}

		// Let normal fishing continue - player gets both duck AND fish as a bonus!
	}
}
