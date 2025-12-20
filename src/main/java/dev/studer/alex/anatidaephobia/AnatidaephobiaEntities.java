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
	public static final Identifier DUCK_ID = Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, "duck");
	public static final EntityType<DuckEntity> DUCK = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			DUCK_ID,
			EntityType.Builder.of(DuckEntity::new, MobCategory.CREATURE).sized(0.85f, 0.85f).build(ResourceKey.create(
					Registries.ENTITY_TYPE,
					DUCK_ID
			))
	);

	public static final Identifier THROWN_DUCK_EGG_ID = Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, "thrown_duck_egg");
	public static final EntityType<ThrownDuckEggEntity> THROWN_DUCK_EGG = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			THROWN_DUCK_EGG_ID,
			EntityType.Builder.<ThrownDuckEggEntity>of(ThrownDuckEggEntity::new, MobCategory.MISC).noLootTable().sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build(ResourceKey.create(
					Registries.ENTITY_TYPE,
					THROWN_DUCK_EGG_ID
			))
	);

	public static void init() {
		FabricDefaultAttributeRegistry.register(DUCK, DuckEntity.createMobAttributes());
	}
}
