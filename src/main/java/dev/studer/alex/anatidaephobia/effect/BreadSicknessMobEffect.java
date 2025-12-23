package dev.studer.alex.anatidaephobia.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * A lethal poison effect caused by feeding ducks bread.
 * Unlike regular Poison, this effect CAN kill the entity.
 */
public class BreadSicknessMobEffect extends MobEffect {
    // Ticks between damage applications (same as poison: 25 ticks = 1.25 seconds at amplifier 0)
    public static final int DAMAGE_INTERVAL = 25;

    // Delay before first damage tick (4 seconds) - gives time to see the effect icon
    public static final int INITIAL_DELAY = 80;

    public BreadSicknessMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        // Unlike PoisonMobEffect, we don't check if health > 1, so this can kill
        mob.hurtServer(level, mob.damageSources().magic(), 1.0F);

        // Spawn extra sneeze/sick particles for visual effect
        if (mob.getRandom().nextFloat() < 0.3F) {
            double x = mob.getX() + (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth();
            double y = mob.getY() + mob.getRandom().nextDouble() * mob.getBbHeight();
            double z = mob.getZ() + (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth();
            level.sendParticles(ParticleTypes.SNEEZE, x, y, z, 1, 0.0, 0.1, 0.0, 0.0);
        }

        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        // Wait for initial delay before first damage
        if (tickCount < INITIAL_DELAY) {
            return false;
        }

        // Same formula as poison: interval halves with each amplifier level
        int adjustedTick = tickCount - INITIAL_DELAY;
        int interval = DAMAGE_INTERVAL >> amplification;
        if (interval > 0) {
            return adjustedTick % interval == 0;
        } else {
            return true;
        }
    }
}
