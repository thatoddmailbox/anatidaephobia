package dev.studer.alex.anatidaephobia;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import dev.studer.alex.anatidaephobia.world.level.portal.DuckyPortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public class AnatidaephobiaBlocks {
	public static final Block NEST_LINING = register(
			"nest_lining",
			Block::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_YELLOW)
					.instrument(NoteBlockInstrument.BANJO)
					.strength(0.5F)
					.sound(SoundType.GRASS)
	);

	public static final Block QUACKMIUM_BLOCK = register(
			"quackmium_block",
			Block::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL)
					.requiresCorrectToolForDrops()
					.strength(5.0F, 6.0F)
					.sound(SoundType.METAL)
	);

	// Ducky Portal - activated by throwing a duck egg into a quackmium frame
	public static final Block DUCKY_PORTAL = registerNoItem(
			"ducky_portal",
			DuckyPortalBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_YELLOW)
					.noCollision()
					.strength(-1.0F) // Unbreakable by hand
					.sound(SoundType.GLASS)
					.lightLevel(state -> 11)
					.noLootTable()
					.pushReaction(PushReaction.BLOCK)
	);

	public static final TagKey<Block> DUCK_SHRINE_BORDER = TagKey.create(
			Registries.BLOCK,
			Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, "duck_shrine_border")
	);

	public static final TagKey<Block> NEST_CENTER = TagKey.create(
			Registries.BLOCK,
			Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, "nest_center")
	);

	public static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties properties) {
		// Create the block key
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, name));

		// Create the block instance
		Block block = blockFactory.apply(properties.setId(blockKey));

		// Register the block
		Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

		// Create and register the block item
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, name));
		BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey));
		Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

		return block;
	}

	// Register a block without creating a corresponding BlockItem (for portal blocks, etc.)
	public static Block registerNoItem(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties properties) {
		// Create the block key
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Anatidaephobia.MOD_ID, name));

		// Create the block instance
		Block block = blockFactory.apply(properties.setId(blockKey));

		// Register the block (no BlockItem)
		Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

		return block;
	}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS)
				.register((itemGroup) -> itemGroup.accept(NEST_LINING));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS)
				.register((itemGroup) -> itemGroup.accept(QUACKMIUM_BLOCK));
	}
}
