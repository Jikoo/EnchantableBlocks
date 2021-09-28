package com.github.jikoo.enchantableblocks.config;

import com.github.jikoo.enchantableblocks.config.data.MultimapEnchantEnchantSetting;
import com.github.jikoo.enchantableblocks.config.data.SetEnchantSetting;
import com.github.jikoo.enchantableblocks.util.enchant.Enchantability;
import com.github.jikoo.planarwrappers.config.Mapping;
import com.github.jikoo.planarwrappers.config.ParsedMapping;
import com.github.jikoo.planarwrappers.config.Setting;
import com.github.jikoo.planarwrappers.config.impl.BooleanSetting;
import com.github.jikoo.planarwrappers.config.impl.EnumSetting;
import com.github.jikoo.planarwrappers.util.StringConverters;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EnchantableBlockConfig {

  protected final ConfigurationSection section;
  public final Setting<Boolean> enabled;
  public final Setting<Enchantability> tableEnchantability;
  public final Setting<Set<Enchantment>> tableDisabledEnchants;
  public final Setting<Multimap<Enchantment, Enchantment>> tableEnchantmentConflicts;
  public final Setting<Set<Enchantment>> anvilDisabledEnchants;
  public final Setting<Multimap<Enchantment, Enchantment>> anvilEnchantmentConflicts;
  public final Mapping<Enchantment, Integer> anvilEnchantmentMax;

  protected EnchantableBlockConfig(@NotNull ConfigurationSection configurationSection) {
    this.section = configurationSection;

    this.enabled = new BooleanSetting(section, "enabled", true);
    this.tableEnchantability = new EnumSetting<>(section, "tableEnchantability", Enchantability.STONE);
    this.tableDisabledEnchants = new SetEnchantSetting(section, "tableDisabledEnchantments", Set.of());
    Multimap<Enchantment, Enchantment> enchantIncompatibilities = HashMultimap.create();
    enchantIncompatibilities.put(Enchantment.SILK_TOUCH, Enchantment.LOOT_BONUS_BLOCKS);
    this.tableEnchantmentConflicts = new MultimapEnchantEnchantSetting(
        section,
        "tableEnchantmentConflicts",
        enchantIncompatibilities);
    this.anvilDisabledEnchants = new SetEnchantSetting(section, "anvilDisabledEnchantments", Set.of());
    this.anvilEnchantmentConflicts = new MultimapEnchantEnchantSetting(
        section,
        "anvilEnchantmentConflicts",
        enchantIncompatibilities);
    this.anvilEnchantmentMax = new ParsedMapping<>(
        section,
        "anvilEnchantmentMax",
        Enchantment::getMaxLevel) {

      @Override
      protected @Nullable Enchantment convertKey(@NotNull String key) {
        return StringConverters.toEnchant(key);
      }

      @Override
      protected boolean testValue(@NotNull ConfigurationSection localSection, @NotNull String path) {
        return localSection.isInt(path);
      }

      @Override
      protected @NotNull Integer convertValue(@NotNull ConfigurationSection localSection, @NotNull String path) {
        return localSection.getInt(path);
      }
    };
  }

}
