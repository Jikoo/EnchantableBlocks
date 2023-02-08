package com.github.jikoo.enchantableblocks.mock.world;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.mockito.stubbing.Answer;

public final class BlockMocks {

  public static @NotNull Block newBlock(@NotNull World world, int x, int y, int z) {
    Block mock = mock(Block.class);

    when(mock.getWorld()).thenReturn(world);

    when(mock.getX()).thenReturn(x);
    when(mock.getY()).thenReturn(y);
    when(mock.getZ()).thenReturn(z);

    when(mock.getLocation()).thenReturn(new Location(world, x, y, z));
    when(mock.getLocation(any(Location.class))).thenAnswer(parameters -> {
      Location parameter = parameters.getArgument(0);
      parameter.setWorld(world);
      parameter.setX(x);
      parameter.setY(y);
      parameter.setZ(z);
      parameter.setPitch(0);
      parameter.setYaw(0);
      return parameter;
    });

    when(mock.getChunk()).thenAnswer(invocation -> world.getChunkAt(mock));

    mockType(mock);
    mockRelative(mock);

    return mock;
  }

  public static void mockType(@NotNull Block block) {
    AtomicReference<Material> type = new AtomicReference<>(Material.AIR);
    when(block.getType()).thenAnswer(parameters -> type.get());
    Answer<Block> setType = parameters -> {
      Material argument = parameters.getArgument(0);
      if (!argument.isBlock()) {
        throw new IllegalArgumentException("Cannot set block type to non-block material!");
      }
      type.set(argument);
      return null;
    };
    doAnswer(setType).when(block).setType(any(Material.class));
    doAnswer(setType).when(block).setType(any(Material.class), anyBoolean());

    AtomicReference<BlockData> data = new AtomicReference<>();
    when(block.getBlockData()).thenAnswer(parameters -> data.get());
    Answer<Block> setData = parameters -> {
      BlockData newData = parameters.getArgument(0);
      Material newType = newData.getMaterial();
      if (!newType.isBlock()) {
        throw new IllegalArgumentException("Cannot set block type to non-block material!");
      }
      type.set(newType);
      data.set(newData);
      return null;
    };
    doAnswer(setData).when(block).setBlockData(any(BlockData.class));
    doAnswer(setData).when(block).setBlockData(any(BlockData.class), anyBoolean());
  }

  public static void mockRelative(@NotNull Block block) {
    when(block.getRelative(any(BlockFace.class)))
        .thenAnswer(parameters -> block.getRelative(parameters.getArgument(0), 1));

    when(block.getRelative(any(BlockFace.class), anyInt()))
        .thenAnswer(parameters -> {
          BlockFace face = parameters.getArgument(0);
          int distance = parameters.getArgument(1);
          return block.getRelative(face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
        });

    when(block.getRelative(anyInt(), anyInt(), anyInt()))
        .thenAnswer(parameters ->
            block.getWorld().getBlockAt(
                block.getX() + parameters.getArgument(0, Integer.class),
                block.getY() + parameters.getArgument(1, Integer.class),
                block.getZ() + parameters.getArgument(2, Integer.class)));
  }

  private BlockMocks() {}

}
