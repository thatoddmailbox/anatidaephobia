package dev.studer.alex.anatidaephobia.effect;

import dev.studer.alex.anatidaephobia.Anatidaephobia;
import dev.studer.alex.anatidaephobia.AnatidaephobiaMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The wrath of the duck gods descends upon those who slay their children.
 * An intense lightning storm surrounds the player, increasing in intensity over time.
 */
public class WrathOfTheDuckGodsMobEffect extends MobEffect {
    // Effect duration: 15 seconds = 300 ticks
    public static final int DURATION = 15 * Anatidaephobia.TICKS_PER_SECOND;

    // Lightning spawns every 10 ticks (0.5 seconds)
    private static final int LIGHTNING_INTERVAL = 10;

    // Particle effects every 2 ticks
    private static final int PARTICLE_INTERVAL = 2;

    // Sound effects at specific intervals
    private static final int HEARTBEAT_INTERVAL = 25; // ~1.25 seconds
    private static final int ROAR_INTERVAL = 100; // Every 5 seconds

    // Lightning radius shrinks from 20 blocks to 3 blocks over the duration
    private static final double MAX_RADIUS = 20.0;
    private static final double MIN_RADIUS = 3.0;

    public WrathOfTheDuckGodsMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // Get remaining duration from the effect instance
        MobEffectInstance instance = entity.getEffect(AnatidaephobiaMobEffects.WRATH_OF_THE_DUCK_GODS);
        int remainingDuration = instance != null ? instance.getDuration() : DURATION;
        int elapsedTicks = DURATION - remainingDuration;
        float progress = Math.min(1.0F, (float) elapsedTicks / DURATION); // 0.0 to 1.0

        // Calculate current radius (shrinks over time)
        double currentRadius = MAX_RADIUS - (progress * (MAX_RADIUS - MIN_RADIUS));

        // Only spawn lightning every LIGHTNING_INTERVAL ticks (not every particle tick)
        if (elapsedTicks % LIGHTNING_INTERVAL == 0) {
            // Calculate lightning count (increases over time: 1-2 at start, 4-6 at end)
            int baseLightningCount = 1 + (int) (progress * 4);
            int lightningCount = baseLightningCount + level.getRandom().nextInt(2);

            // Spawn lightning bolts
            spawnLightningStorm(level, entity, currentRadius, lightningCount);
        }

        // Apply Darkness effect (pulsing - short duration so it fades in and out)
        entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0, false, false, false));

        // Apply Slowness (constant while effect is active)
        entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 0, false, false, false));

        // Spawn ominous particles around the player
        spawnOminousParticles(level, entity, currentRadius);

        // Play heartbeat sound periodically
        if (elapsedTicks % HEARTBEAT_INTERVAL == 0) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE,
                    1.5F, 0.8F + level.getRandom().nextFloat() * 0.2F);
        }

        // Play roar/horn at dramatic moments
        if (elapsedTicks == 0) {
            // Initial dramatic horn blast
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2.0F, 0.5F);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.5F, 0.7F);
        } else if (elapsedTicks % ROAR_INTERVAL == 0) {
            // Periodic angry sounds
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.WARDEN_ANGRY, SoundSource.HOSTILE, 1.2F, 0.9F);
        }

        // Near the end (last 3 seconds), play increasingly frantic sounds
        if (progress > 0.8F && elapsedTicks % 15 == 0) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.WARDEN_NEARBY_CLOSEST, SoundSource.HOSTILE, 1.0F, 1.2F);
        }

        return true;
    }

    private void spawnLightningStorm(ServerLevel level, LivingEntity entity, double radius, int count) {
        for (int i = 0; i < count; i++) {
            // Random angle around the player
            double angle = level.getRandom().nextDouble() * 2 * Math.PI;
            // Random distance within radius
            double distance = radius * 0.5 + level.getRandom().nextDouble() * radius * 0.5;

            double x = entity.getX() + Math.cos(angle) * distance;
            double z = entity.getZ() + Math.sin(angle) * distance;

            // Find ground level at this position
            BlockPos groundPos = level.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    BlockPos.containing(x, entity.getY(), z));

            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
            if (lightning != null) {
                lightning.snapTo(Vec3.atBottomCenterOf(groundPos));
                // Not visual only - these lightning bolts are REAL and set fires
                lightning.setVisualOnly(false);
                level.addFreshEntity(lightning);
            }
        }
    }

    private void spawnOminousParticles(ServerLevel level, LivingEntity entity, double radius) {
        // Soul fire flames circling the player
        for (int i = 0; i < 8; i++) {
            double angle = level.getRandom().nextDouble() * 2 * Math.PI;
            double distance = 1.5 + level.getRandom().nextDouble() * 3;
            double x = entity.getX() + Math.cos(angle) * distance;
            double y = entity.getY() + 0.5 + level.getRandom().nextDouble() * 2;
            double z = entity.getZ() + Math.sin(angle) * distance;

            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0, 0.05, 0, 0.02);
        }

        // Souls rising from the ground
        if (level.getRandom().nextFloat() < 0.3F) {
            double angle = level.getRandom().nextDouble() * 2 * Math.PI;
            double distance = level.getRandom().nextDouble() * radius * 0.5;
            double x = entity.getX() + Math.cos(angle) * distance;
            double z = entity.getZ() + Math.sin(angle) * distance;

            level.sendParticles(ParticleTypes.SOUL, x, entity.getY(), z, 1, 0, 0.1, 0, 0.01);
        }

        // Large smoke columns
        for (int i = 0; i < 4; i++) {
            double angle = level.getRandom().nextDouble() * 2 * Math.PI;
            double distance = 2 + level.getRandom().nextDouble() * 4;
            double x = entity.getX() + Math.cos(angle) * distance;
            double z = entity.getZ() + Math.sin(angle) * distance;

            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, entity.getY(), z, 2, 0.3, 0.5, 0.3, 0.01);
        }

        // Angry villager particles above player's head (duck gods are ANGRY)
        if (level.getRandom().nextFloat() < 0.2F) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    entity.getX(), entity.getY() + 2.5, entity.getZ(),
                    1, 0.5, 0.2, 0.5, 0);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        // Apply every LIGHTNING_INTERVAL ticks for lightning
        // But also need to apply for particles which are more frequent
        return tickCount % PARTICLE_INTERVAL == 0;
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        // Set weather to thunderstorm for the duration
        if (entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.setWeatherParameters(0, DURATION + 100, true, true);
        }
    }
}
