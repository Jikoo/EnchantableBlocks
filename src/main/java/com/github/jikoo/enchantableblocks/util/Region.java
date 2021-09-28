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

  public boolean anyChunkMatch(BiPredicate<Integer, Integer> chunkPredicate) {
    int minChunkX = Coords.regionToChunk(x);
    int minChunkZ = Coords.regionToChunk(z);

    for (int chunkX = minChunkX, maxChunkX = minChunkX + 32; chunkX < maxChunkX; ++chunkX) {
      for (int chunkZ = minChunkZ, maxChunkZ = minChunkZ + 32; chunkZ < maxChunkZ; ++chunkZ) {
        if (chunkPredicate.test(chunkX, chunkZ)) {
          return true;
        }
      }
    }

    return false;
  }

  public void forEachChunk(BiConsumer<Integer, Integer> chunkConsumer) {
    int minChunkX = Coords.regionToChunk(x);
    int minChunkZ = Coords.regionToChunk(z);

    for (int chunkX = minChunkX, maxChunkX = minChunkX + 32; chunkX < maxChunkX; ++chunkX) {
      for (int chunkZ = minChunkZ, maxChunkZ = minChunkZ + 32; chunkZ < maxChunkZ; ++chunkZ) {
        chunkConsumer.accept(chunkX, chunkZ);
      }
    }
  }

}
