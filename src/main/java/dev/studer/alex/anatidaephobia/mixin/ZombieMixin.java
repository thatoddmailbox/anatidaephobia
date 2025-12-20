package dev.studer.alex.anatidaephobia.mixin;

import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zombie.class)
public class ZombieMixin {
	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void addDuckAvoidance(CallbackInfo ci) {
		Zombie self = (Zombie)(Object)this;
		self.goalSelector.addGoal(3, new NearestAttackableTargetGoal(self, Duck.class, true));
	}
}
