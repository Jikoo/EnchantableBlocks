package com.github.jikoo.enchantableblocks.util.mock;

import be.seeseemelk.mockbukkit.inventory.InventoryMock;
import org.bukkit.block.Furnace;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

// TODO PR to MockBukkit
public class FurnaceInventoryMock extends InventoryMock implements FurnaceInventory {

  public FurnaceInventoryMock(@Nullable Furnace holder) {
    super(holder, InventoryType.FURNACE);
  }

  @Override
  public @Nullable ItemStack getResult() {
    return getItem(2);
  }

  @Override
  public void setResult(@Nullable ItemStack itemStack) {
    setItem(2, itemStack);
  }

  @Override
  public @Nullable ItemStack getFuel() {
    return getItem(1);
  }

  @Override
  public void setFuel(@Nullable ItemStack itemStack) {
    setItem(1, itemStack);
  }

  @Override
  public @Nullable ItemStack getSmelting() {
    return getItem(0);
  }

  @Override
  public void setSmelting(@Nullable ItemStack itemStack) {
    setItem(0, itemStack);
  }

  @Override
  public @Nullable Furnace getHolder() {
    return (Furnace) super.getHolder();
  }

}
