package com.github.jikoo.enchantableblocks.util.mock;

import org.bukkit.block.Block;
import org.bukkit.block.Smoker;
import org.jetbrains.annotations.NotNull;

public class SmokerMock extends FurnaceMock implements Smoker {

  public SmokerMock(@NotNull Block block) {
    super(block);
  }

  public SmokerMock(@NotNull FurnaceMock state) {
    super(state);
  }

}
