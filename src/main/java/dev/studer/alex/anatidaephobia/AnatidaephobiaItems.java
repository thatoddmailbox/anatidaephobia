package dev.studer.alex.anatidaephobia;

import dev.studer.alex.anatidaephobia.item.DuckEggItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.function.Function;

public class AnatidaephobiaItems {
	public static final Item DUCK_EGG = register("duck_egg", DuckEggItem::new, new Item.Properties());
	public static final Item DUCK_SPAWN_EGG = register("duck_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(AnatidaephobiaEntities.DUCK));

	public static final Item RAW_QUACKMIUM = register("raw_quackmium", Item::new, new Item.Properties());
	public static final Item QUACKMIUM_INGOT = register("quackmium_ingot", Item::new, new Item.Properties());

	public static final TagKey<Item> DUCK_FOOD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, "duck_food"));

	public static Item register(String name, Function<Item.Properties, Item> itemFactory, Item.Properties settings) {
		// Create the item key.
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, name));

		// Create the item instance.
		Item item = itemFactory.apply(settings.setId(itemKey));

		// Register the item.
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);

		return item;
	}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS)
				.register((itemGroup) -> itemGroup.accept(DUCK_SPAWN_EGG));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
				.register((itemGroup) -> itemGroup.accept(DUCK_EGG));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
				.register((itemGroup) -> itemGroup.accept(RAW_QUACKMIUM));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
				.register((itemGroup) -> itemGroup.accept(QUACKMIUM_INGOT));
	}
}
