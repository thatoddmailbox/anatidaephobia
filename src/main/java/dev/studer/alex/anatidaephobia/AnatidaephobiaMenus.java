package dev.studer.alex.anatidaephobia;

import dev.studer.alex.anatidaephobia.menu.DuckMenu;
import dev.studer.alex.anatidaephobia.menu.DuckMenuData;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public class AnatidaephobiaMenus {
	public static final MenuType<DuckMenu> DUCK_MENU = Registry.register(
			BuiltInRegistries.MENU,
			"anatidaephobia:duck",
			new ExtendedScreenHandlerType<>(DuckMenu::new, DuckMenuData.STREAM_CODEC)
	);

	public static void init() {

	}
}
