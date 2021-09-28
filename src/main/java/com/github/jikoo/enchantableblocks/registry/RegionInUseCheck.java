package com.github.jikoo.enchantableblocks.registry;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager.RegionStorageData;
import com.github.jikoo.enchantableblocks.util.Region;
import com.github.jikoo.enchantableblocks.util.RegionStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

record RegionInUseCheck(@NotNull Logger logger)
    implements BiPredicate<@NotNull Region, @Nullable RegionStorageData> {

  @Override
  public boolean test(@NotNull Region key, @Nullable RegionStorageData value) {
    if (value == null) {
      return false;
    }

    RegionStorage storage = value.getStorage();
    World world = Bukkit.getWorld(storage.getRegion().worldName());
    boolean loaded = world != null && storage.getRegion().anyChunkMatch(world::isChunkLoaded);
    boolean dirty = value.isDirty();

    if (!dirty) {
      return loaded;
    }

    Collection<String> keys = storage.getKeys(true);
    boolean delete = true;
    for (String path : keys) {
      if (storage.get(path) != null) {
        delete = false;
        break;
      }
    }

    if (delete) {
      try {
        Files.deleteIfExists(storage.getDataFile().toPath());
        value.clean();
      } catch (IOException e) {
        logger().log(Level.WARNING, e, e::getMessage);
      }
      return loaded;
    }

    try {
      storage.save();
      value.clean();
    } catch (IOException e) {
      logger().log(Level.WARNING, e, e::getMessage);
    }

    return loaded;
  }

}
