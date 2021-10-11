package com.github.jikoo.enchantableblocks.registry;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import java.util.Collection;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registration entry for an {@link EnchantableBlock} implementation.
 */
public abstract class EnchantableRegistration {

  protected final @NotNull Plugin plugin;
  private final @NotNull Class<? extends EnchantableBlock> blockClass;
  private @Nullable EnchantableBlockConfig config;

  /**
   * Constructor for a new {@code EnchantableRegistration}.
   * @param plugin      the plugin providing the registration
   * @param blockClass  the class of the {@link EnchantableBlock}
   */
  protected EnchantableRegistration(
      @NotNull Plugin plugin,
      @NotNull Class<? extends EnchantableBlock> blockClass) {
    this.plugin = plugin;
    this.blockClass = blockClass;
  }

  /**
   * Get the class of the {@link EnchantableBlock}.
   *
   * @return the class of the {@code EnchantableBlock}
   */
  public @NotNull Class<? extends EnchantableBlock> getBlockClass() {
    return blockClass;
  }

  /**
   * Construct a new {@link EnchantableBlock} instance using the given parameters.
   *
   * @param block     the in-world {@link Block}
   * @param itemStack the {@link ItemStack} representation of the object
   * @param storage   the {@link ConfigurationSection} used to store any necessary data
   * @return the {@code EnchantableBlock}
   */
  protected abstract @NotNull EnchantableBlock newBlock(
      @NotNull final Block block,
      @NotNull final ItemStack itemStack,
      @NotNull ConfigurationSection storage);

  public @NotNull EnchantableBlockConfig getConfig() {
    if (config == null) {
      config = loadFullConfig(plugin.getConfig());
    }

    return config;
  }

  /**
   * Load a new copy of the block-specific configuration from the main plugin configuration.
   *
   * @param fileConfiguration the main plugin configuration
   * @return the block-specific configuration
   */
  protected final @NotNull EnchantableBlockConfig loadFullConfig(@NotNull FileConfiguration fileConfiguration) {
    String path = "blocks." + blockClass.getSimpleName();
    ConfigurationSection configurationSection = fileConfiguration.getConfigurationSection(path);
    if (configurationSection == null) {
      configurationSection = fileConfiguration.createSection(path);
    }

    return loadConfig(configurationSection);
  }

  /**
   * Load a new copy of the block-specific configuration from its {@link ConfigurationSection}.
   *
   * <p>Block-specific configuration is stored in the main plugin configuration by class simple
   * name, i.e. a class
   * {@code com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock} is
   * configurable using the section {@code blocks.DummyEnchantableBlock}.
   *
   * <p>This method is only called when a new configuration instance is being requested. Caching is
   * handled by the {@link #getConfig} method.
   *
   * @param configurationSection the block-specific section
   * @return the configuration instance created
   */
  protected abstract @NotNull EnchantableBlockConfig loadConfig(@NotNull ConfigurationSection configurationSection);

  /**
   * Get all possible enchantments for use in enchantment assignment or combination.
   *
   * <p>Note that configurations may later disable certain enchantments. This is only a listing of
   * enchantments that have implemented functions, not allowed enchantments.
   *
   * <p>Note that this list also may not be an exhaustive list of enchantments with functionality -
   * there may be enchantments with game breaking capabilities that can only be created via creative
   * mode or administrative powers.
   *
   * @return a collection of potential enchantments
   */
  public abstract @NotNull Collection<@NotNull Enchantment> getEnchants();

  /**
   * Get a collection of all {@link Material Materials} that can be represented by this
   * registration.
   *
   * @return a collection of supported materials
   */
  public abstract @NotNull Collection<@NotNull Material> getMaterials();

  /**
   * Reload any implementation-specific details, clear caches, etc.
   */
  protected void reload() {
    config = null;
  }

  /**
   * Check if a {@link Permissible} has permission to enchant this registration's block using a
   * particular enchantment technique.
   *
   * @param permissible the {@code Permissible}
   * @param enchanterType the type of the enchanter
   * @return true if the {@code Permissible} is allowed to enchant the block
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean hasEnchantPermission(
      @NotNull Permissible permissible,
      @NotNull String enchanterType) {
    String pluginName = plugin.getName().toLowerCase(Locale.ROOT);
    String blockType = getBlockClass().getSimpleName().toLowerCase(Locale.ROOT);
    String node = String.format("%s.enchant.%s.%s", pluginName, enchanterType, blockType);
    return hasNodeOrParent(permissible, node);
  }

  /**
   * Recursively check a node and its parents.
   *
   * <p>If the {@link Permissible} has the node, returns true. If it is set and they lack it,
   * returns false. If the node is unset, the parent node is checked. This repeats until the top
   * level node is reached, at which point false is returned.
   *
   * @param permissible the {@code Permissible} who may have the node
   * @param node the node to check
   * @return true if the {@code Permissible} has the node
   */
  private static boolean hasNodeOrParent(@NotNull Permissible permissible, @NotNull String node) {
    if (permissible.hasPermission(node)) {
      // Permission is true.
      return true;
    } else if (permissible.isPermissionSet(node)) {
      // Permission is explicitly set to false.
      return false;
    }

    // Ensure node is not a top level node.
    int nodeSeparator = node.lastIndexOf('.');
    if (nodeSeparator == -1) {
      return false;
    }

    // Fall through to parent node.
    return hasNodeOrParent(permissible, node.substring(0, nodeSeparator));
  }

}
