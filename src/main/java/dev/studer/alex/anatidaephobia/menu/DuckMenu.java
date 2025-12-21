package dev.studer.alex.anatidaephobia.menu;

import dev.studer.alex.anatidaephobia.AnatidaephobiaMenus;
import dev.studer.alex.anatidaephobia.entity.Duck;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class DuckMenu extends AbstractContainerMenu {
	private final Duck duck;

	public DuckMenu(int containerId, Inventory playerInventory, DuckMenuData data) {
		this(containerId, playerInventory,
				(Duck) playerInventory.player.level().getEntity(data.entityId()));
	}

	public DuckMenu(int containerId, Inventory playerInventory, Duck duck) {
		super(AnatidaephobiaMenus.DUCK_MENU, containerId);
		this.duck = duck;

		// Add custom slots here if needed
		// this.addSlot(new Slot(...));

		// Add player inventory slots
		this.addStandardInventorySlots(playerInventory, 8, 84);
	}

	public Duck getDuck() {
		return duck;
	}

	@Override
	public boolean stillValid(Player player) {
		return duck != null && duck.isAlive() &&
				player.isWithinEntityInteractionRange(duck, 4.0);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		// Handle shift-click behavior
		return ItemStack.EMPTY;
	}
}
