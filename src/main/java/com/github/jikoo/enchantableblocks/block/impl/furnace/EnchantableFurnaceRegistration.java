package com.github.jikoo.enchantableblocks.block.impl.furnace;

import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import com.github.jikoo.enchantableblocks.util.EmptyCookingRecipe;
import com.github.jikoo.planarwrappers.util.StringConverters;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.Smoker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * EnchantableRegistration for furnace variants.
 */
public class EnchantableFurnaceRegistration extends EnchantableRegistration {

  private static final List<Enchantment> ENCHANTMENTS = List.of(
      Enchantment.EFFICIENCY,
      Enchantment.UNBREAKING,
      Enchantment.FORTUNE,
      Enchantment.SILK_TOUCH);
  private static final Set<Material> MATERIALS =
      Set.of(Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER);
  private static final CookingRecipe<?> INVALID_INPUT = new EmptyCookingRecipe(
      Objects.requireNonNull(StringConverters.toNamespacedKey("enchantableblocks:invalid_input")));

  private final Map<Integer, CookingRecipe<?>> blastFurnaceCache = new Int2ObjectOpenHashMap<>();
  private final Map<Integer, CookingRecipe<?>> smokerCache = new Int2ObjectOpenHashMap<>();
  private final Map<Integer, CookingRecipe<?>> furnaceCache = new Int2ObjectOpenHashMap<>();
  private final @NotNull Listener listener;

  /**
   * Construct a new {@code EnchantableFurnaceRegistration} for the given {@link Plugin} and
   * {@link EnchantableBlockManager}.
   *
   * @param plugin the owning {@code Plugin}
   * @param manager the owning {@code EnchantableBlockManager}
   */
  public EnchantableFurnaceRegistration(
      @NotNull Plugin plugin,
      @NotNull EnchantableBlockManager manager) {
    super(plugin, EnchantableFurnace.class);
    this.listener = new FurnaceListener(plugin, manager);
    // TODO move this to an enable/disable
    plugin.getServer().getPluginManager().registerEvents(listener, plugin);
  }

  @Override
  public @NotNull EnchantableFurnace newBlock(@NotNull Block block, @NotNull ItemStack itemStack,
      @NotNull ConfigurationSection storage) {
    return new EnchantableFurnace(this, block, itemStack, storage);
  }

  @Override
  public @NotNull EnchantableFurnaceConfig getConfig() {
    return (EnchantableFurnaceConfig) super.getConfig();
  }

  @Override
  protected @NotNull EnchantableFurnaceConfig loadConfig(
      @NotNull ConfigurationSection configurationSection) {
    return new EnchantableFurnaceConfig(configurationSection);
  }

  @Override
  public @NotNull Collection<Enchantment> getEnchants() {
    return ENCHANTMENTS;
  }

  @Override
  public @NotNull Collection<Material> getMaterials() {
    return MATERIALS;
  }

  @Override
  protected void reload() {
    super.reload();
    blastFurnaceCache.clear();
    smokerCache.clear();
    furnaceCache.clear();
    HandlerList.unregisterAll(listener);
    plugin.getServer().getPluginManager().registerEvents(listener, plugin);
  }

  /**
   * Get a {@link CookingRecipe} for a {@link FurnaceInventory}'s state.
   *
   * @param inventory the {@link FurnaceInventory}
   * @return the {@link CookingRecipe} or {@code null} if no valid recipe is found
   */
  public @Nullable CookingRecipe<?> getFurnaceRecipe(@NotNull FurnaceInventory inventory) {
    ItemStack smelting = inventory.getSmelting();
    if (smelting == null || smelting.getType() == Material.AIR) {
      return null;
    }

    Furnace holder = inventory.getHolder();
    if (holder == null) {
      return null;
    }

    // Obtain cache for holder type.
    Map<Integer, CookingRecipe<?>> recipes;
    if (holder instanceof BlastFurnace) {
      recipes = blastFurnaceCache;
    } else if (holder instanceof Smoker) {
      recipes = smokerCache;
    } else {
      recipes = furnaceCache;
    }

    // Retrieve recipe, caching if necessary, for item.
    // Note: Does not support recipes that smelt multiple items per.
    ItemStack cacheData = smelting.clone();
    cacheData.setAmount(1);
    Integer cacheId = cacheData.hashCode();
    CookingRecipe<?> recipe = recipes.computeIfAbsent(cacheId,
        key -> locateRecipe(holder, smelting));

    if (!recipe.getInputChoice().test(smelting)) {
      return null;
    }

    return recipe;
  }

  /**
   * Match a {@link CookingRecipe} for a particular {@link ItemStack} in an inventory belonging to a
   * specific {@link InventoryHolder}.
   *
   * @param holder the inventory holder
   * @param smelting the recipe input
   * @return the {@link CookingRecipe} or a default invalid recipe if no match was found
   */
  @VisibleForTesting
  CookingRecipe<?> locateRecipe(
      @NotNull Furnace holder,
      @NotNull ItemStack smelting) {
    Iterator<Recipe> iterator = Bukkit.recipeIterator();
    while (iterator.hasNext()) {
      Recipe next = iterator.next();

      if (!(next instanceof CookingRecipe<?> nextCooking) || isIneligibleRecipe(holder, next)) {
        continue;
      }

      if (nextCooking.getInputChoice().test(smelting)) {
        return nextCooking;
      }
    }

    return INVALID_INPUT;
  }

  /**
   * Check if a {@link Furnace} is eligible to use a certain {@link Recipe}.
   *
   * @param holder the furnace
   * @param recipe the recipe
   * @return true if the holder is allowed to use the recipe
   */
  private boolean isIneligibleRecipe(@NotNull Furnace holder, @NotNull Recipe recipe) {
    if (holder instanceof BlastFurnace) {
      return !(recipe instanceof BlastingRecipe);
    }
    if (holder instanceof Smoker) {
      return !(recipe instanceof SmokingRecipe);
    }
    return !(recipe instanceof FurnaceRecipe);
  }

}
