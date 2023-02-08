package com.github.jikoo.enchantableblocks.mock.inventory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentMatchers;

public class InventoryMocks {

  public static @NotNull AnvilInventory newAnvilMock() {
    return newMock(AnvilInventory.class, InventoryType.ANVIL, 3);
  }

  public static @NotNull FurnaceInventory newFurnaceMock() {
    return newFurnaceMock(InventoryType.FURNACE);
  }

  public static @NotNull FurnaceInventory newFurnaceMock(InventoryType furnaceType) {
    var furnace = newMock(FurnaceInventory.class, furnaceType, 3);

    doAnswer(invocation -> furnace.getItem(0)).when(furnace).getSmelting();
    doAnswer(invocation -> {
      furnace.setItem(0, invocation.getArgument(0));
      return null;
    }).when(furnace).setSmelting(any());

    doAnswer(invocation -> furnace.getItem(1)).when(furnace).getFuel();
    doAnswer(invocation -> {
      furnace.setItem(1, invocation.getArgument(0));
      return null;
    }).when(furnace).setFuel(any());

    doAnswer(invocation -> furnace.getItem(2)).when(furnace).getResult();
    doAnswer(invocation -> {
      furnace.setItem(2, invocation.getArgument(0));
      return null;
    }).when(furnace).setResult(any());

    return furnace;
  }

  public static <T extends Inventory> @NotNull T newMock(Class<T> inventoryClass, InventoryType type, int slots) {
    T inventory = mock(inventoryClass);

    when(inventory.getType()).thenReturn(type);
    when(inventory.getSize()).thenReturn(slots);

    List<ItemStack> items = new ArrayList<>(slots);
    for (int i = 0; i < slots; ++i) {
      items.add(null);
    }

    when(inventory.getContents()).thenAnswer(invocation -> items.toArray(new ItemStack[0]));
    when(inventory.getItem(ArgumentMatchers.anyInt())).thenAnswer(invocation -> {
      int index = invocation.getArgument(0);
      if (index < 0 || index >= slots) {
        throw new IndexOutOfBoundsException(index);
      }
      return items.get(index);
    });
    doAnswer(invocation -> {
      int index = invocation.getArgument(0);
      if (index < 0 || index >= slots) {
        throw new IndexOutOfBoundsException(index);
      }
      items.set(index, invocation.getArgument(1));
      return null;
    }).when(inventory).setItem(ArgumentMatchers.anyInt(), ArgumentMatchers.any());

    return inventory;
  }

}
