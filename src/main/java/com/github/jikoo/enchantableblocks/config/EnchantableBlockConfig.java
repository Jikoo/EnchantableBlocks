package com.github.jikoo.enchantableblocks.config;

import com.github.jikoo.enchantableblocks.config.data.EnchantMaxLevelMapping;
import com.github.jikoo.enchantableblocks.config.data.EnchantabilitySetting;
import com.github.jikoo.enchantableblocks.config.data.MultimapEnchantEnchantSetting;
import com.github.jikoo.enchantableblocks.config.data.SetEnchantSetting;
import com.github.jikoo.planarenchanting.table.Enchantability;
import com.github.jikoo.planarwrappers.config.Mapping;
import com.github.jikoo.planarwrappers.config.Setting;
import com.github.jikoo.planarwrappers.config.impl.BooleanSetting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

/**
 * The base settings for all
 * {@link com.github.jikoo.enchantableblocks.block.EnchantableBlock EnchantableBlocks}.
 */
public abstract class EnchantableBlockConfig {

  protected final @NotNull ConfigurationSection section;
  /** @deprecated use {@link #enabled()} */
  @Deprecated(forRemoval = true, since = "4.1.0")
  public final @NotNull Setting<Boolean> enabled;
  /** @deprecated use {@link #tableEnchantability()} */
  @Deprecated(forRemoval = true, since = "4.1.0")
  public final @NotNull Setting<Enchantability> tableEnchantability;
  /** @deprecated use {@link #tableDisabledEnchants()} */
  @Deprecated(forRemoval = true, since = "4.1.0")
  public final @NotNull Setting<Set<Enchantment>> tableDisabledEnchants;
  /** @deprecated use {@link #tableEnchantmentConflicts()} */
  @Deprecated(forRemoval = true, since = "4.1.0")
  public final @NotNull Setting<Multimap<Enchantment, Enchantment>> tableEnchantmentConflicts;
  /** @deprecated use {@link #anvilDisabledEnchants()} */
  @Deprecated(forRemoval = true, since = "4.1.0")
  public final @NotNull Setting<Set<Enchantment>> anvilDisabledEnchants;
  /** @deprecated use {@link #anvilEnchantmentConflicts()} */
  @Deprecated(forRemoval = true, since = "4.1.0")
  public final @NotNull Setting<Multimap<Enchantment, Enchantment>> anvilEnchantmentConflicts;
  /** @deprecated use {@link #anvilEnchantmentMax()} */
  @Deprecated(forRemoval = true, since = "4.1.0")
  public final @NotNull Mapping<Enchantment, Integer> anvilEnchantmentMax;

  protected EnchantableBlockConfig(@NotNull ConfigurationSection configurationSection) {
    this.section = configurationSection;

    this.enabled = new BooleanSetting(section, "enabled", true);
    this.tableEnchantability = new EnchantabilitySetting(
        section,
        "tableEnchantability",
        Enchantability.STONE);
    this.tableDisabledEnchants = new SetEnchantSetting(
        section,
        "tableDisabledEnchantments",
        Set.of());
    Multimap<Enchantment, Enchantment> enchantIncompatibilities = HashMultimap.create();
    enchantIncompatibilities.put(Enchantment.SILK_TOUCH, Enchantment.FORTUNE);
    this.tableEnchantmentConflicts = new MultimapEnchantEnchantSetting(
        section,
        "tableEnchantmentConflicts",
        enchantIncompatibilities);
    this.anvilDisabledEnchants = new SetEnchantSetting(
        section,
        "anvilDisabledEnchantments",
        Set.of());
    this.anvilEnchantmentConflicts = new MultimapEnchantEnchantSetting(
        section,
        "anvilEnchantmentConflicts",
        enchantIncompatibilities);
    this.anvilEnchantmentMax = new EnchantMaxLevelMapping(section, "anvilEnchantmentMax");
  }

  public @NotNull Setting<Boolean> enabled() {
    return enabled;
  }

  public @NotNull Setting<Enchantability> tableEnchantability() {
    return tableEnchantability;
  }

  public @NotNull Setting<Set<Enchantment>> tableDisabledEnchants() {
    return tableDisabledEnchants;
  }

  public @NotNull Setting<Multimap<Enchantment, Enchantment>> tableEnchantmentConflicts() {
    return tableEnchantmentConflicts;
  }

  public @NotNull Setting<Set<Enchantment>> anvilDisabledEnchants() {
    return anvilDisabledEnchants;
  }

  public @NotNull Setting<Multimap<Enchantment, Enchantment>> anvilEnchantmentConflicts() {
    return anvilEnchantmentConflicts;
  }

  public @NotNull Mapping<Enchantment, Integer> anvilEnchantmentMax() {
    return anvilEnchantmentMax;
  }

}
