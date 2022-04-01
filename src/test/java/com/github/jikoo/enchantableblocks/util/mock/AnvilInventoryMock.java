package com.github.jikoo.enchantableblocks.util.mock;

import be.seeseemelk.mockbukkit.inventory.InventoryMock;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.Nullable;

public class AnvilInventoryMock extends InventoryMock implements AnvilInventory {

  private @Nullable String renameText;
  private int repairCost = 0;
  private int repairCostAmount = 0;
  private int maxRepairCost = 40;

  public AnvilInventoryMock(@Nullable InventoryHolder holder) {
    super(holder, InventoryType.ANVIL);
  }

  public void setRenameText(@Nullable String renameText) {
    this.renameText = renameText;
  }

  @Override
  public @Nullable String getRenameText() {
    return renameText;
  }

  @Override
  public int getRepairCostAmount() {
    return repairCostAmount;
  }

  @Override
  public void setRepairCostAmount(int amount) {
    this.repairCostAmount = amount;
  }

  @Override
  public int getRepairCost() {
    return repairCost;
  }

  @Override
  public void setRepairCost(int repairCost) {
    this.repairCost = repairCost;
  }

  @Override
  public int getMaximumRepairCost() {
    return maxRepairCost;
  }

  @Override
  public void setMaximumRepairCost(int maximumRepairCost) {
    this.maxRepairCost = maximumRepairCost;
  }

}
