package com.github.jikoo.enchantableblocks.block.impl.furnace;

import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.planarwrappers.config.Setting;
import com.github.jikoo.planarwrappers.config.impl.BooleanSetting;
import com.github.jikoo.planarwrappers.config.impl.MaterialSetSetting;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for {@link EnchantableFurnace EnchantableFurnaces}.
 */
class EnchantableFurnaceConfig extends EnchantableBlockConfig {

  public final Setting<Boolean> fortuneListIsBlacklist;
  public final Setting<Set<Material>> fortuneList;

  /**
   * Construct a new {@code EnchantableFurnaceConfig} with the given {@link ConfigurationSection}.
   *
   * @param configurationSection the configuration
   */
  EnchantableFurnaceConfig(@NotNull ConfigurationSection configurationSection) {
    super(configurationSection);
    fortuneListIsBlacklist = new BooleanSetting(section, "fortuneListIsBlacklist", true);
    fortuneList = new MaterialSetSetting(
        section,
        "fortuneList",
        EnumSet.of(Material.WET_SPONGE, Material.STONE_BRICKS));
  }

}
