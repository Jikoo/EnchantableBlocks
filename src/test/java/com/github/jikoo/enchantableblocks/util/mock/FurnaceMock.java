package com.github.jikoo.enchantableblocks.util.mock;

import be.seeseemelk.mockbukkit.block.state.TileStateMock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.FurnaceInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO PR to MockBukkit
public class FurnaceMock extends TileStateMock implements Furnace {

  private short burnTime = 0;
  private short cookTime = 0;
  private int cookTimeTotal = 0;
  private @Nullable String customName;
  private @NotNull String lock = "";
  private @Nullable FurnaceInventory inventory;

  public FurnaceMock(@NotNull Material material) {
    super(material);
  }

  public FurnaceMock(@NotNull Block block) {
    super(block);
  }

  public FurnaceMock(@NotNull FurnaceMock state) {
    super(state);
    this.burnTime = state.burnTime;
    this.cookTime = state.cookTime;
    this.cookTimeTotal = state.cookTimeTotal;
    this.customName = state.customName;
    this.lock = state.lock;
    this.inventory = state.inventory;
  }

  @Override
  public @NotNull BlockState getSnapshot() {
    return new FurnaceMock(this);
  }

  @Override
  public short getBurnTime() {
    return this.burnTime;
  }

  @Override
  public void setBurnTime(short burnTime) {
    this.burnTime = burnTime;
  }

  @Override
  public short getCookTime() {
    return this.cookTime;
  }

  @Override
  public void setCookTime(short cookTime) {
    this.cookTime = cookTime;
  }

  @Override
  public int getCookTimeTotal() {
    return this.cookTimeTotal;
  }

  @Override
  public void setCookTimeTotal(int cookTimeTotal) {
    this.cookTimeTotal = cookTimeTotal;
  }

  @Override
  public @NotNull FurnaceInventory getInventory() {
    if (inventory == null) {
      inventory = new FurnaceInventoryMock(this);
    }

    return inventory;
  }

  @Override
  public @NotNull FurnaceInventory getSnapshotInventory() {
    return new FurnaceInventoryMock(this);
  }

  @Override
  public @Nullable String getCustomName() {
    return this.customName;
  }

  @Override
  public void setCustomName(@Nullable String customName) {
    this.customName = customName;
  }

  @Override
  public boolean isLocked() {
    return !this.lock.isEmpty();
  }

  @Override
  public @NotNull String getLock() {
    return this.lock;
  }

  @Override
  public void setLock(@Nullable String lock) {
    this.lock = lock == null ? "" : lock;
  }

}
