package com.github.jikoo.enchantableblocks.mock.enchantments;

import java.util.Collection;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

// Simple data holder is a lot easier to write than mocking every single method
public class EnchantmentHolder extends Enchantment {

  private final int maxLevel;
  private final @NotNull EnchantmentTarget target;
  private final boolean treasure;
  private final boolean curse;
  private final @NotNull Collection<Enchantment> conflicts; // TODO more maintainable via multimap? more work to write first time

  public EnchantmentHolder(
      @NotNull NamespacedKey key,
      int maxLevel,
      @NotNull EnchantmentTarget target,
      boolean treasure,
      boolean curse,
      @NotNull Collection<Enchantment> conflicts) {
    super(key);
    this.maxLevel = maxLevel;
    this.target = target;
    this.treasure = treasure;
    this.curse = curse;
    this.conflicts = conflicts;
  }

  @Deprecated
  @Override
  public @NotNull String getName() {
    return getKey().getKey();
  }

  @Override
  public int getMaxLevel() {
    return maxLevel;
  }

  @Override
  public int getStartLevel() {
    return 1;
  }

  @Override
  public @NotNull EnchantmentTarget getItemTarget() {
    return target;
  }

  @Deprecated
  @Override
  public boolean isTreasure() {
    return treasure;
  }

  @Deprecated
  @Override
  public boolean isCursed() {
    return curse;
  }

  @Override
  public boolean conflictsWith(@NotNull Enchantment other) {
    return conflicts.contains(other);
  }

  @Override
  public boolean canEnchantItem(@NotNull ItemStack item) {
    return getItemTarget().includes(item);
  }
}
