package dev.studer.alex.anatidaephobia.mixin;

import dev.studer.alex.anatidaephobia.DuckEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public class CreeperMixin {
	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void addDuckAvoidance(CallbackInfo ci) {
		Creeper self = (Creeper)(Object)this;
		self.goalSelector.addGoal(3, new AvoidEntityGoal<>(
				self,
				DuckEntity.class,  // The duck entity class
				8.0F,              // Detection range
				1.0D,              // Walk speed
				1.4D               // Sprint speed
		));
	}
}
