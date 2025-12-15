package dev.studer.alex.anatidaephobia;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class AnatidaephobiaEntities {
    public static final Identifier DUCK_ID = Identifier.tryBuild(Anatidaephobia.MOD_ID, "duck");
    public static final EntityType<DuckEntity> DUCK = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DUCK_ID,
            EntityType.Builder.of(DuckEntity::new, MobCategory.CREATURE).sized(1, 1).build(ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    DUCK_ID
            ))
    );

    public static void init() {
        FabricDefaultAttributeRegistry.register(DUCK, DuckEntity.createMobAttributes());
    }
}
