package com.github.jikoo.enchantableblocks.util.mock;

import org.bukkit.Material;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public class BlastFurnaceMock extends FurnaceMock implements BlastFurnace {

  public BlastFurnaceMock(@NotNull Material material) {
    super(material);
  }

  public BlastFurnaceMock(@NotNull Block block) {
    super(block);
  }

  public BlastFurnaceMock(@NotNull FurnaceMock state) {
    super(state);
  }

}
