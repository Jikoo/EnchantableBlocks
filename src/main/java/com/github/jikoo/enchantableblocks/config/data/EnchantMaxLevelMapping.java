package com.github.jikoo.enchantableblocks.config.data;

import com.github.jikoo.planarwrappers.config.ParsedMapping;
import com.github.jikoo.planarwrappers.util.StringConverters;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link com.github.jikoo.planarwrappers.config.Mapping Mapping} for
 * {@link Enchantment Enchantments} to max levels.
 */
public class EnchantMaxLevelMapping extends ParsedMapping<Enchantment, Integer> {

  public EnchantMaxLevelMapping(
      @NotNull ConfigurationSection section,
      @NotNull String path) {
    super(section, path, Enchantment::getMaxLevel);
  }

  @Override
  protected @Nullable Enchantment convertKey(@NotNull String key) {
    return StringConverters.toEnchant(key);
  }

  @Override
  protected boolean testValue(@NotNull ConfigurationSection localSection, @NotNull String path) {
    return localSection.isInt(path);
  }

  @Override
  protected @NotNull Integer convertValue(
      @NotNull ConfigurationSection localSection,
      @NotNull String path) {
    return localSection.getInt(path);
  }

}
