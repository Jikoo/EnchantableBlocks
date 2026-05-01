package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import com.github.jikoo.planarenchanting.anvil.AnvilBehavior;
import com.google.common.collect.Multimap;
import org.bukkit.enchantments.Enchantment;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;

@NullMarked
public class BlockAnvilBehavior<T> implements AnvilBehavior<T> {

  private final EnchantableBlockConfig config;
  private final String worldName;
  private final Collection<Enchantment> enchants;

  public BlockAnvilBehavior(
      EnchantableRegistration registration,
      String worldName
  ) {
    this.config = registration.getConfig();
    this.worldName = worldName;
    this.enchants = registration.getEnchants();
  }

  @Override
  public boolean enchantApplies(Enchantment enchantment, T base) {
    return enchants.contains(enchantment) && !config.anvilDisabledEnchants().get(worldName).contains(enchantment);
  }

  @Override
  public boolean enchantsConflict(Enchantment enchant1, Enchantment enchant2) {
    Multimap<Enchantment, Enchantment> conflicts = config.anvilEnchantmentConflicts().get(worldName);
    return conflicts.containsEntry(enchant1, enchant2) || conflicts.containsEntry(enchant2, enchant1);
  }

  @Override
  public int getEnchantMaxLevel(Enchantment enchantment) {
    return config.anvilEnchantmentMax().get(worldName, enchantment);
  }

  @Override
  public boolean itemsCombineEnchants(T base, T addition) {
    return true;
  }

  @Override
  public boolean itemRepairedBy(T repaired, T repairMat) {
    return false;
  }

}
