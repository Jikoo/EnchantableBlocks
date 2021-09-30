package com.github.jikoo.enchantableblocks.block.impl;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * A dummy EnchantableBlock implementation.
 */
public abstract class DummyEnchantableBlock extends EnchantableBlock {

  private DummyEnchantableBlock(
      @NotNull DummyEnchantableRegistration registration,
      @NotNull Block block,
      @NotNull ItemStack itemStack,
      @NotNull ConfigurationSection storage) {
    super(registration, block, itemStack, storage);
  }

  /**
   * A dummy EnchantableRegistration implementation.
   */
  public static class DummyEnchantableRegistration extends EnchantableRegistration {

    private final Collection<@NotNull Enchantment> enchants;
    private final Collection<@NotNull Material> materials;

    public DummyEnchantableRegistration(
        @NotNull Plugin plugin,
        @NotNull Collection<@NotNull Enchantment> enchants,
        @NotNull Collection<@NotNull Material> materials) {
      super(plugin, DummyEnchantableBlock.class);
      this.enchants = Set.copyOf(enchants);
      this.materials = EnumSet.copyOf(materials);
    }

    @Override
    protected @NotNull DummyEnchantableBlock newBlock(
        @NotNull Block block,
        @NotNull ItemStack itemStack,
        @NotNull ConfigurationSection storage) {
      return new DummyEnchantableBlock(this, block, itemStack, storage) {
        @Override
        public boolean isCorrectType(@NotNull Material material) {
          return materials.contains(material);
        }
      };
    }

    @Override
    protected @NotNull DummyEnchantableConfig loadConfig(
        @NotNull ConfigurationSection configurationSection) {
      return new DummyEnchantableConfig(configurationSection);
    }

    @Override
    public @NotNull Collection<@NotNull Enchantment> getEnchants() {
      return enchants;
    }

    @Override
    public @NotNull Collection<@NotNull Material> getMaterials() {
      return materials;
    }
  }

  /**
   * A dummy EnchantableBlockConfig implementation.
   */
  public static class DummyEnchantableConfig extends EnchantableBlockConfig {
    private DummyEnchantableConfig(
        @NotNull ConfigurationSection configurationSection) {
      super(configurationSection);
    }
  }

}
