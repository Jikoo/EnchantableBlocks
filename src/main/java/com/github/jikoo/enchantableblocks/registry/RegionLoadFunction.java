package com.github.jikoo.enchantableblocks.registry;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager.RegionStorageData;
import com.github.jikoo.enchantableblocks.util.Region;
import com.github.jikoo.enchantableblocks.util.RegionStorage;
import java.io.IOException;
import java.util.function.BiFunction;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

record RegionLoadFunction(
    @NotNull Plugin plugin,
    @NotNull EnchantableBlockManager manager)
    implements BiFunction<@NotNull Region, @NotNull Boolean, @Nullable RegionStorageData> {

  @Override
  public @Nullable RegionStorageData apply(@NotNull Region region, @NotNull Boolean create) {
    RegionStorage storage = new RegionStorage(plugin(), region);

    if (!storage.getDataFile().exists() && Boolean.FALSE.equals(create)) {
      return null;
    }

    try {
      storage.load();
    } catch (@NotNull IOException | InvalidConfigurationException e) {
      plugin().getLogger().log(Level.WARNING, e, e::getMessage);
    }

    return manager().new RegionStorageData(storage);
  }

}
