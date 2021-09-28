package com.github.jikoo.enchantableblocks.registry;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registration entry for an {@link EnchantableBlock} implementation.
 *
 * @param <T> the class of the {@code EnchantableBlock}
 * @param <U> the configuration class
 */
public abstract class EnchantableRegistration<T extends EnchantableBlock<T, U>, U extends EnchantableBlockConfig> {

  private final @NotNull Plugin plugin;
  private final @NotNull Class<T> blockClass;
  private @Nullable U config;

  /**
   * Constructor for a new {@code EnchantableRegistration}.
   * @param plugin      the plugin providing the registration
   * @param blockClass  the class of the {@link EnchantableBlock}
   */
  public EnchantableRegistration(
      @NotNull Plugin plugin,
      @NotNull Class<T> blockClass) {
    this.plugin = plugin;
    this.blockClass = blockClass;
  }

  /**
   * Get the class of the {@link EnchantableBlock}.
   *
   * @return the class of the {@code EnchantableBlock}
   */
  public @NotNull Class<T> getBlockClass() {
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
  protected abstract @NotNull T newBlock(
      @NotNull final Block block,
      @NotNull final ItemStack itemStack,
      @NotNull ConfigurationSection storage);

  public U getConfig() {
    if (config != null) {
      return config;
    }

    return config = loadFullConfig(plugin.getConfig());
  }

  /**
   * Load a new copy of the block-specific configuration from the main plugin configuration.
   *
   * @param fileConfiguration the main plugin configuration
   * @return the block-specific configuration
   */
  protected final @NotNull U loadFullConfig(@NotNull FileConfiguration fileConfiguration) {
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
   * <p>Block-specific configuration is stored in the main plugin configuration by block class
   * name,
   * i.e. {@link com.github.jikoo.enchantableblocks.block.impl.EnchantableFurnace
   * EnchantableFurnaces} are configurable using the section {@code blocks.EnchantableFurnace}.
   *
   * <p>This method is only called when a new configuration instance is being requested. Caching is
   * handled by the registry.
   *
   * @param configurationSection the block-specific section
   * @return the configuration instance created
   */
  protected abstract @NotNull U loadConfig(@NotNull ConfigurationSection configurationSection);

  /**
   * Get all possible enchantments for use in enchantment assignment or combination.
   *
   * <p>Note that configurations may later disable certain enchantments. This is only a listing of
   * enchantments that have implemented functions, not allowed enchantments.
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

}