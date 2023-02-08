package com.github.jikoo.enchantableblocks.mock.world;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.jikoo.planarwrappers.util.Coords;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentMatchers;

public final class WorldMocks {

  public static @NotNull World newWorld(@NotNull String name) {
    World mock = mock(World.class);

    when(mock.getName()).thenReturn(name);

    Map<Coordinate, Block> blocks = new HashMap<>();
    when(mock.getBlockAt(anyInt(), anyInt(), anyInt()))
        .thenAnswer(parameters ->
            blocks.computeIfAbsent(
                new Coordinate(parameters.getArgument(0), parameters.getArgument(1), parameters.getArgument(2)),
                key -> BlockMocks.newBlock(mock, key.x(), key.y(), key.z())));
    when(mock.getBlockAt(any(Location.class))).thenAnswer(parameters -> {
      Location location = parameters.getArgument(0);
      return mock.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    });

    Map<ChunkCoordinate, Chunk> chunks = new HashMap<>();
    when(mock.getChunkAt(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenAnswer(invocation ->
      chunks.computeIfAbsent(
          new ChunkCoordinate(invocation.getArgument(0), invocation.getArgument(1)),
          key -> {
            Chunk chunk = mock(Chunk.class);
            when(chunk.getX()).thenReturn(key.chunkX());
            when(chunk.getZ()).thenReturn(key.chunkZ());
            when(chunk.getWorld()).thenReturn(mock);
            return chunk;
          }));
    when(mock.getChunkAt(ArgumentMatchers.any(Block.class))).thenAnswer(invocation -> {
      Block block = invocation.getArgument(0);
      return mock.getChunkAt(Coords.blockToChunk(block.getX()), Coords.blockToChunk(block.getZ()));
    });
    when(mock.getChunkAt(ArgumentMatchers.any(Location.class))).thenAnswer(invocation -> {
      Location location = invocation.getArgument(0);
      return mock.getChunkAt(Coords.blockToChunk(location.getBlockX()), Coords.blockToChunk(location.getBlockZ()));
    });

    return mock;
  }

  private record Coordinate(int x, int y, int z) {}

  private record ChunkCoordinate(int chunkX, int chunkZ) {}

  private WorldMocks() {}

}
