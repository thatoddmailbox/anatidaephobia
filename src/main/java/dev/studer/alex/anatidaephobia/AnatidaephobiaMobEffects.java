package dev.studer.alex.anatidaephobia;

import dev.studer.alex.anatidaephobia.effect.BreadSicknessMobEffect;
import dev.studer.alex.anatidaephobia.effect.WrathOfTheDuckGodsMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AnatidaephobiaMobEffects {
    // Sickly green/brown color (like moldy bread) - RGB: 139, 140, 70 = 0x8B8C46
    private static final int BREAD_SICKNESS_COLOR = 0x8B8C46;

    // Dark purple/black with hints of lightning yellow - ominous divine wrath
    private static final int WRATH_COLOR = 0x2D1B4E;

    public static final Holder<MobEffect> BREAD_SICKNESS = register(
            "bread_sickness",
            new BreadSicknessMobEffect(MobEffectCategory.HARMFUL, BREAD_SICKNESS_COLOR)
    );

    public static final Holder<MobEffect> WRATH_OF_THE_DUCK_GODS = register(
            "wrath_of_the_duck_gods",
            new WrathOfTheDuckGodsMobEffect(MobEffectCategory.HARMFUL, WRATH_COLOR).withSoundOnAdded(SoundEvents.WARDEN_ROAR)
    );

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        Identifier id = Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, name);
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id, effect);
    }

    public static void init() {}
}
