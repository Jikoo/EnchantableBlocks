package com.github.jikoo.enchantableblocks.util;

import com.github.jikoo.planarwrappers.util.Coords;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import org.bukkit.Chunk;
import org.bukkit.block.Block;

/**
 * A record representing a Minecraft region (32x32 chunks, 512x512 blocks).
 */
public record Region(String worldName, int x, int z) {

  public Region(Chunk chunk) {
    this(chunk.getWorld().getName(), Coords.chunkToRegion(chunk.getX()),
        Coords.chunkToRegion(chunk.getZ()));
  }

  public Region(Block block) {
    this(block.getWorld().getName(), Coords.blockToRegion(block.getX()),
        Coords.blockToRegion(block.getZ()));
  }

  /**
   * Check if any chunk in the {@code Region} matches the given {@link BiPredicate}.
   *
   * @param chunkPredicate a predicate accepting chunk X and Z coordinates
   * @return true if any chunk matches
   */
  public boolean anyChunkMatch(BiPredicate<Integer, Integer> chunkPredicate) {
    int minChunkX = Coords.regionToChunk(x);
    int minChunkZ = Coords.regionToChunk(z);
    int maxChunkX = Coords.regionToChunk(x + 1);
    int maxChunkZ = Coords.regionToChunk(z + 1);

    for (int chunkX = minChunkX; chunkX < maxChunkX; ++chunkX) {
      for (int chunkZ = minChunkZ; chunkZ < maxChunkZ; ++chunkZ) {
        if (chunkPredicate.test(chunkX, chunkZ)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Run a {@link BiConsumer} over every chunk in the {@code Region}.
   *
   * @param chunkConsumer a consumer accepting chunk X and Z coordinates
   */
  public void forEachChunk(BiConsumer<Integer, Integer> chunkConsumer) {
    int minChunkX = Coords.regionToChunk(x);
    int minChunkZ = Coords.regionToChunk(z);
    int maxChunkX = Coords.regionToChunk(x + 1);
    int maxChunkZ = Coords.regionToChunk(z + 1);

    for (int chunkX = minChunkX; chunkX < maxChunkX; ++chunkX) {
      for (int chunkZ = minChunkZ; chunkZ < maxChunkZ; ++chunkZ) {
        chunkConsumer.accept(chunkX, chunkZ);
      }
    }
  }

}
