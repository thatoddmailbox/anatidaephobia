package dev.studer.alex.anatidaephobia;

import dev.studer.alex.anatidaephobia.effect.BreadSicknessMobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AnatidaephobiaMobEffects {
    // Sickly green/brown color (like moldy bread) - RGB: 139, 140, 70 = 0x8B8C46
    private static final int BREAD_SICKNESS_COLOR = 0x8B8C46;

    public static final Holder<MobEffect> BREAD_SICKNESS = register(
            "bread_sickness",
            new BreadSicknessMobEffect(MobEffectCategory.HARMFUL, BREAD_SICKNESS_COLOR)
    );

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        Identifier id = Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, name);
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id, effect);
    }

    public static void init() {}
}
