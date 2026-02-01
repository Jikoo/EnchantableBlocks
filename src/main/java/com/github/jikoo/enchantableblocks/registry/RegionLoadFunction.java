package com.github.jikoo.enchantableblocks.registry;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager.RegionStorageData;
import com.github.jikoo.enchantableblocks.util.Region;
import com.github.jikoo.enchantableblocks.util.RegionStorage;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link BiFunction} used to load data from disk.
 */
record RegionLoadFunction(
    @NotNull EnchantableBlockManager manager,
    @NotNull Path dataDir,
    @NotNull Logger logger)
    implements BiFunction<@NotNull Region, @NotNull Boolean, @Nullable RegionStorageData> {

  @Override
  public @Nullable RegionStorageData apply(@NotNull Region region, @NotNull Boolean create) {
    RegionStorage storage = new RegionStorage(dataDir(), region);

    if (!storage.getDataFile().exists() && !create) {
      return null;
    }

    try {
      storage.load();
    } catch (@NotNull IOException | InvalidConfigurationException e) {
      logger().log(Level.WARNING, e, e::getMessage);
    }

    return manager().new RegionStorageData(storage);
  }

}
