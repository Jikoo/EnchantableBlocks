package com.github.jikoo.enchantableblocks.mock.world;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.ArgumentMatchers;

public final class BlockDataMocks {

  public static @Nullable BlockData newData(@NotNull Material material) {
    if (!material.isBlock()) {
      throw new IllegalArgumentException("Material must be a block!");
    }
    if (material == Material.AIR) {
      return null;
    }
    Class<?> data = material.data;

    if (!BlockData.class.isAssignableFrom(data)) {
      data = BlockData.class;
    }

    BlockData mock = (BlockData) mock(data);
    generic(material, mock);

    when(mock.clone()).thenAnswer(parameters -> newData(material));

    if (mock instanceof Directional directional) {
      directional(directional);
    }
    if (mock instanceof MultipleFacing multipleFacing) {
      multipleFacing(multipleFacing);
    }
    if (mock instanceof Orientable orientable) {
      orientable(orientable);
    }
    if (mock instanceof Rotatable rotatable) {
      rotatable(rotatable);
    }

    return mock;
  }

  private static void generic(@NotNull Material material, @NotNull BlockData blockData) {
    when(blockData.getMaterial()).thenReturn(material);
  }

  public static @NotNull Directional directional(@NotNull Material material) {
    Directional mock = mock(Directional.class);
    generic(material, mock);

    directional(mock);

    return mock;
  }

  private static void directional(@NotNull Directional mock) {
    Set<BlockFace> availableFaces = EnumSet.range(BlockFace.NORTH, BlockFace.DOWN);
    when(mock.getFaces()).thenReturn(availableFaces);

    AtomicReference<BlockFace> facing = new AtomicReference<>(BlockFace.NORTH);
    when(mock.getFacing()).thenAnswer(parameters -> facing.get());

    doAnswer(parameters -> {
      BlockFace newFacing = parameters.getArgument(0);
      if (!availableFaces.contains(newFacing)) {
        throw new IllegalArgumentException("Facing not allowed!");
      }
      facing.set(newFacing);
      return null;
    }).when(mock).setFacing(ArgumentMatchers.notNull());

    when(mock.clone()).thenAnswer(invocation -> {
      Directional clone = directional(mock.getMaterial());
      clone.setFacing(facing.get());
      return clone;
    });
  }

  public static @NotNull MultipleFacing multipleFacing(@NotNull Material material) {
    MultipleFacing mock = mock(MultipleFacing.class);
    generic(material, mock);

    multipleFacing(mock);

    return mock;
  }

  private static void multipleFacing(@NotNull MultipleFacing mock) {
    Set<BlockFace> allowedFaces = EnumSet.range(BlockFace.NORTH, BlockFace.DOWN);
    when(mock.getAllowedFaces()).thenReturn(allowedFaces);

    Set<BlockFace> faces = EnumSet.noneOf(BlockFace.class);
    when(mock.getFaces()).thenReturn(faces);

    when(mock.hasFace(ArgumentMatchers.any(BlockFace.class))).thenAnswer(parameters -> {
      BlockFace face = parameters.getArgument(0);
      return faces.contains(face);
    });

    doAnswer(parameters -> {
      BlockFace face = parameters.getArgument(0);
      boolean has = parameters.getArgument(1);

      if (!has) {
        faces.remove(face);
        return null;
      }

      if (!allowedFaces.contains(face)) {
        throw new IllegalArgumentException("Facing not allowed!");
      }

      faces.add(face);
      return null;
    }).when(mock).setFace(ArgumentMatchers.notNull(), ArgumentMatchers.anyBoolean());

    when(mock.clone()).thenAnswer(invocation -> {
      MultipleFacing clone = multipleFacing(mock.getMaterial());
      faces.forEach(face -> clone.setFace(face, true));
      return clone;
    });
  }

  public static @NotNull Orientable orientable(@NotNull Material material) {
    Orientable mock = mock(Orientable.class);
    generic(material, mock);

    orientable(mock);

    return mock;
  }

  private static void orientable(@NotNull Orientable mock) {
    AtomicReference<Axis> axis = new AtomicReference<>(Axis.Y);
    when(mock.getAxis()).thenAnswer(parameters -> axis.get());

    Set<Axis> axes = EnumSet.allOf(Axis.class);
    when(mock.getAxes()).thenReturn(axes);

    doAnswer(parameters -> {
      Axis newAxis = parameters.getArgument(0);
      if (!axes.contains(newAxis)) {
        throw new IllegalArgumentException("Axis not allowed!");
      }
      axis.set(newAxis);
      return null;
    }).when(mock).setAxis(ArgumentMatchers.notNull());

    when(mock.clone()).thenAnswer(invocation -> {
      Orientable clone = orientable(mock.getMaterial());
      clone.setAxis(axis.get());
      return clone;
    });
  }

  public static @NotNull Rotatable rotatable(@NotNull Material material) {
    Rotatable mock = mock(Rotatable.class);
    generic(material, mock);

    rotatable(mock);

    return mock;
  }

  private static void rotatable(@NotNull Rotatable mock) {
    AtomicReference<BlockFace> axis = new AtomicReference<>(BlockFace.NORTH);
    when(mock.getRotation()).thenAnswer(parameters -> axis.get());

    Set<BlockFace> axes = EnumSet.range(BlockFace.NORTH, BlockFace.DOWN);
    doAnswer(parameters -> {
      BlockFace newAxis = parameters.getArgument(0);
      if (!axes.contains(newAxis)) {
        throw new IllegalArgumentException("Rotatable may only face an axis!");
      }
      axis.set(newAxis);
      return null;
    }).when(mock).setRotation(ArgumentMatchers.notNull());

    when(mock.clone()).thenAnswer(invocation -> {
      Rotatable clone = rotatable(mock.getMaterial());
      clone.setRotation(axis.get());
      return clone;
    });
  }

  private BlockDataMocks() {}

}
