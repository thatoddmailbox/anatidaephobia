package dev.studer.alex.anatidaephobia;

import com.google.common.collect.Maps;
import dev.studer.alex.anatidaephobia.item.DuckEggItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;
import java.util.function.Function;

public class AnatidaephobiaItems {
	public static final Item DUCK_EGG = register("duck_egg", DuckEggItem::new, new Item.Properties());
	public static final Item DUCK_SPAWN_EGG = register("duck_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(AnatidaephobiaEntities.DUCK));

	public static final Item RAW_QUACKMIUM = register("raw_quackmium", Item::new, new Item.Properties());
	public static final Item QUACKMIUM_INGOT = register("quackmium_ingot", Item::new, new Item.Properties());

	public static final TagKey<Item> DUCK_FOOD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, "duck_food"));
	public static final TagKey<Item> DUCK_INTERACTIVE = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, "duck_interactive"));

	// Quackmium Armor
	public static final TagKey<Item> REPAIRS_QUACKMIUM_ARMOR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, "repairs_quackmium_armor"));
	public static final ResourceKey<EquipmentAsset> QUACKMIUM_EQUIPMENT_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, "quackmium"));

	// Between iron and diamond: durability=24, defense=(boots=2, legs=6, chest=7, helm=3, body=6), enchantValue=10, toughness=1.0, knockback=0
	public static final ArmorMaterial QUACKMIUM_ARMOR_MATERIAL = new ArmorMaterial(
			24, // durability multiplier (between iron 15 and diamond 33)
			Maps.newEnumMap(Map.of(
					ArmorType.BOOTS, 2,
					ArmorType.LEGGINGS, 6,
					ArmorType.CHESTPLATE, 7,
					ArmorType.HELMET, 3,
					ArmorType.BODY, 6
			)),
			10, // enchantment value (between iron 9 and diamond 10)
			SoundEvents.ARMOR_EQUIP_IRON, // equip sound
			1.0F, // toughness (between iron 0 and diamond 2.0)
			0.0F, // knockback resistance (same as iron)
			REPAIRS_QUACKMIUM_ARMOR,
			QUACKMIUM_EQUIPMENT_ASSET
	);

	public static final Item QUACKMIUM_HELMET = register("quackmium_helmet", Item::new, new Item.Properties().humanoidArmor(QUACKMIUM_ARMOR_MATERIAL, ArmorType.HELMET));
	public static final Item QUACKMIUM_CHESTPLATE = register("quackmium_chestplate", Item::new, new Item.Properties().humanoidArmor(QUACKMIUM_ARMOR_MATERIAL, ArmorType.CHESTPLATE));
	public static final Item QUACKMIUM_LEGGINGS = register("quackmium_leggings", Item::new, new Item.Properties().humanoidArmor(QUACKMIUM_ARMOR_MATERIAL, ArmorType.LEGGINGS));
	public static final Item QUACKMIUM_BOOTS = register("quackmium_boots", Item::new, new Item.Properties().humanoidArmor(QUACKMIUM_ARMOR_MATERIAL, ArmorType.BOOTS));

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

		// Quackmium Armor - add to Combat tab
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT)
				.register((itemGroup) -> {
					itemGroup.accept(QUACKMIUM_HELMET);
					itemGroup.accept(QUACKMIUM_CHESTPLATE);
					itemGroup.accept(QUACKMIUM_LEGGINGS);
					itemGroup.accept(QUACKMIUM_BOOTS);
				});
	}
}
