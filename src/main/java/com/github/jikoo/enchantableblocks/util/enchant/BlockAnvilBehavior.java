package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import com.github.jikoo.planarenchanting.anvil.AnvilBehavior;
import com.github.jikoo.planarenchanting.util.MetaCachedStack;
import com.google.common.collect.Multimap;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class BlockAnvilBehavior implements AnvilBehavior {

  private final @NotNull EnchantableBlockConfig config;
  private final @NotNull String worldName;
  private final @NotNull Collection<Enchantment> enchants;

  public BlockAnvilBehavior(
      @NotNull EnchantableRegistration registration,
      @NotNull String worldName
  ) {
    this.config = registration.getConfig();
    this.worldName = worldName;
    this.enchants = registration.getEnchants();
  }

  @Override
  public boolean enchantApplies(@NotNull Enchantment enchantment, @NotNull MetaCachedStack base) {
    return enchants.contains(enchantment) && !config.anvilDisabledEnchants().get(worldName).contains(enchantment);
  }

  @Override
  public boolean enchantsConflict(@NotNull Enchantment enchant1, @NotNull Enchantment enchant2) {
    Multimap<Enchantment, Enchantment> conflicts = config.anvilEnchantmentConflicts().get(worldName);
    return conflicts.containsEntry(enchant1, enchant2) || conflicts.containsEntry(enchant2, enchant1);
  }

  @Override
  public int getEnchantMaxLevel(@NotNull Enchantment enchantment) {
    return config.anvilEnchantmentMax().get(worldName, enchantment);
  }

  @Override
  public boolean itemsCombineEnchants(@NotNull MetaCachedStack base, @NotNull MetaCachedStack addition) {
    return true;
  }

  @Override
  public boolean itemRepairedBy(@NotNull MetaCachedStack repaired, @NotNull MetaCachedStack repairMat) {
    return false;
  }

}
