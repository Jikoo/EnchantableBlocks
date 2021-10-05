package com.github.jikoo.enchantableblocks.block.impl.furnace;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import com.github.jikoo.enchantableblocks.util.ItemStackHelper;
import com.github.jikoo.enchantableblocks.util.MathHelper;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Track and manage effects for enchanted furnaces.
 */
class EnchantableFurnace extends EnchantableBlock {

  private final boolean canPause;
  private boolean updating = false;

  EnchantableFurnace(
      final @NotNull EnchantableFurnaceRegistration registration,
      final @NotNull Block block,
      final @NotNull ItemStack itemStack,
      final @NotNull ConfigurationSection storage) {
    super(registration, block, itemStack, storage);
    this.canPause = this.getItemStack().getEnchantments().containsKey(Enchantment.SILK_TOUCH);
    if (this.canPause && this.getItemStack().getEnchantmentLevel(Enchantment.SILK_TOUCH) == 1) {
      // New furnaces shouldn't get 1 tick flame for free, but old furnaces need to re-light
      this.getItemStack().addUnsafeEnchantment(Enchantment.SILK_TOUCH, 0);
      this.updateStorage();
    }
  }

  @Override
  public @NotNull EnchantableFurnaceRegistration getRegistration() {
    return (EnchantableFurnaceRegistration) super.getRegistration();
  }

  @Override
  public @NotNull EnchantableFurnaceConfig getConfig() {
    return getRegistration().getConfig();
  }

  /**
   * Get the {@link Furnace} for this {@code EnchantableFurnace}.
   *
   * @return the {@link Furnace} or {@code null} if not found
   */
  public @Nullable Furnace getFurnaceTile() {
    BlockState state = this.getBlock().getState();
    return state instanceof Furnace furnace ? furnace : null;
  }

  /**
   * Get the modifier for cooking speed to be used in calculations.
   *
   * @return the cooking speed modifier
   */
  public int getCookModifier() {
    return this.getItemStack().getEnchantmentLevel(Enchantment.DIG_SPEED);
  }

  /**
   * Get the modifier for fuel burn rate to be used in calculations.
   *
   * @return the fuel burn rate modifier
   */
  public int getBurnModifier() {
    return this.getItemStack().getEnchantmentLevel(Enchantment.DURABILITY);
  }

  /**
   * Get the fortune level for creating extra results.
   *
   * @return the fortune level
   */
  public int getFortune() {
    return this.getItemStack().getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
  }

  /**
   * Get whether the furnace is allowed to "pause" and preserve existing burn time.
   *
   * @return true if the furnace can pause
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean canPause() {
    return this.canPause;
  }

  /**
   * Get whether the furnace should pause after completion of the event occurring.
   *
   * @param event the event occurring
   * @return true if the furnace should pause
   */
  public boolean shouldPause(final @Nullable Event event) {
    if (!this.canPause()) {
      return false;
    }

    Furnace furnace = getFurnaceTile();

    if (furnace == null) {
      // Null furnace tile means no handling. Shouldn't happen, but rare edge cases with tile entity differences occur.
      return false;
    }

    ItemStack input;
    ItemStack result;
    if (event instanceof FurnaceSmeltEvent smeltEvent) {
      // Special case FurnaceSmeltEvent - since smelt has not completed, input and result are slightly different.
      // Decrease input for post-smelt
      input = smeltEvent.getSource().clone();
      input.setAmount(input.getAmount() - 1);
      // Use post-smelt result
      result = smeltEvent.getResult();
    } else {
      // In all other cases use current contents of furnace.
      FurnaceInventory inventory = furnace.getInventory();
      input = inventory.getSmelting();
      result = inventory.getResult();
    }

    return shouldPause(furnace, input, result);
  }

  /**
   * Get whether the furnace should pause based on the input and result items. Note that depending on the event
   * occurring, the input and result items may not match the current furnace contents.
   *
   * @param furnace the furnace
   * @param input the input item
   * @param result the result item
   * @return true if the furnace should pause
   */
  private boolean shouldPause(
      final @NotNull Furnace furnace,
      final @Nullable ItemStack input,
      final @Nullable ItemStack result) {
    if (!this.canPause()) {
      return false;
    }

    if (this.getFrozenTicks() > 0) {
      // Don't pause (again) and delete fuel if there was a problem unpausing.
      return false;
    }

    // Is there no input?
    if (input == null || input.getAmount() <= 0) {
      return true;
    }

    // Is the result slot too full for more product?
    if (!ItemStackHelper.isEmpty(result)) {
      int stack = result.getType().getMaxStackSize();
      if (result.getAmount() >= stack) {
        return true;
      }
    }

    CookingRecipe<?> recipe = getRegistration().getFurnaceRecipe(furnace.getInventory());

    // Does the current smelting item not have a recipe?
    if (recipe == null) {
      return true;
    }

    // Verify that the smelting item cannot produce a result
    return !recipe.getInputChoice().test(input)
        || (result != null && result.getType() != Material.AIR && !recipe.getResult().isSimilar(result));

  }

  /**
   * Attempt to pause the furnace.
   */
  public void pause() {
    if (!this.canPause() || this.getFrozenTicks() > 0) {
      return;
    }

    Furnace furnace = this.getFurnaceTile();

    if (furnace == null) {
      return;
    }

    this.getItemStack().addUnsafeEnchantment(Enchantment.SILK_TOUCH, furnace.getBurnTime());
    this.updateStorage();
    furnace.setBurnTime((short) 0);
    furnace.update(true);
  }

  /**
   * Attempt to unpause the furnace.
   *
   * @return whether the furnace is unpaused
   */
  public boolean resume() {
    Furnace furnace = this.getFurnaceTile();
    // Is furnace unfrozen already?
    if (furnace == null || !this.isPaused()) {
      return false;
    }

    // Is there an input?
    FurnaceInventory furnaceInv = furnace.getInventory();
    if (ItemStackHelper.isEmpty(furnaceInv.getSmelting())) {
      return false;
    }

    // Is the output full?
    ItemStack result = furnaceInv.getResult();
    if (!ItemStackHelper.isEmpty(result) && result.getAmount() == result.getType().getMaxStackSize()) {
      return false;
    }

    CookingRecipe<?> recipe = getRegistration().getFurnaceRecipe(furnaceInv);

    if (recipe == null) {
      return false;
    }

    // Ensure result matches current output
    if (!ItemStackHelper.isEmpty(result) && !recipe.getResult().isSimilar(result)) {
      return false;
    }

    furnace.setBurnTime(
        MathHelper.clampPositiveShort(((long) furnace.getBurnTime()) + this.getFrozenTicks()));
    furnace.update(true);
    this.getItemStack().addUnsafeEnchantment(Enchantment.SILK_TOUCH, 0);
    this.updateStorage();
    return true;
  }

  public boolean forceResume() {
    if (!this.canPause() || this.getFrozenTicks() < 1) {
      return false;
    }

    Furnace furnace = this.getFurnaceTile();

    if (furnace == null) {
      return false;
    }

    furnace.setBurnTime(
        MathHelper.clampPositiveShort(((long) furnace.getBurnTime()) + this.getFrozenTicks()));
    furnace.update(true);
    this.getItemStack().addUnsafeEnchantment(Enchantment.SILK_TOUCH, 0);
    this.updateStorage();
    return true;
  }

  /**
   * Check if the furnace is paused.
   *
   * @return true if the furnace is paused
   */
  public boolean isPaused() {
    return this.canPause && this.getItemStack().getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;
  }

  /**
   * Get the number of ticks of fuel remaining when the furnace is unpaused.
   *
   * @return the number of ticks of fuel
   */
  @VisibleForTesting
  short getFrozenTicks() {
    return (short) this.getItemStack().getEnchantmentLevel(Enchantment.SILK_TOUCH);
  }

  /**
   * Apply modifiers to total cook time. Higher cook modifiers yield shorter cooking times.
   *
   * <p>Uses an inverse sigmoid function. Resulting values are capped between {@code 0} and the
   * lowest of {@code 2 * totalCookTime} or {@link Short#MAX_VALUE} to not cause display issues.
   *
   * @param totalCookTime the original total cook time
   * @return the modified cook time
   */
  public short applyCookTimeModifiers(double totalCookTime) {
    // Invert sign of cook modifier to invert sigmoid.
    return MathHelper.clampPositiveShort(MathHelper.sigmoid(totalCookTime, -getCookModifier(), 2.0));
  }

  /**
   * Apply modifiers to fuel burn time. Higher burn modifiers yield longer burn times. Higher cook
   * modifiers yield shorter burn times proportionate to the cooking speed increase.
   *
   * <p>Uses a sigmoid function. Resulting values are capped between {@code 0} and the lowest of
   * {@code 2 * burnTime} or {@link Short#MAX_VALUE} to not cause display issues.
   *
   * @param burnTime the original burn time
   * @return the modified burn time
   */
  public short applyBurnTimeModifiers(int burnTime) {
    // Apply burn time modifiers.
    double baseTicks = MathHelper.sigmoid(burnTime, getBurnModifier(), 3.0);
    // Round up so that the same number of items are likely to be able to smelt.
    baseTicks += 0.5;
    // Apply cook speed reduction
    return applyCookTimeModifiers(baseTicks);
  }

  @Override
  public String toString() {
    return "EnchantableFurnace{" +
        "block=" + getBlock() +
        "itemStack=" + getItemStack() +
        "canPause=" + canPause +
        '}';
  }

  /**
   * Update the state of a potential {@code EnchantableFurnace}.
   *
   * @param plugin the {@link Plugin} instance
   * @param inventory the furnace inventory that may need an update
   */
  public static void update(
      @NotNull Plugin plugin,
      @NotNull EnchantableBlockManager manager,
      @NotNull FurnaceInventory inventory) {

    Furnace furnace = inventory.getHolder();
    if (furnace == null) {
      return;
    }

    var enchantableBlock = manager.getBlock(furnace.getBlock());

    if (!(enchantableBlock instanceof EnchantableFurnace enchantableFurnace)) {
      return;
    }

    if (enchantableFurnace.updating || !enchantableFurnace.canPause()) {
      return;
    }

    enchantableFurnace.updating = true;

    plugin.getServer().getScheduler().runTask(plugin, () -> {
      boolean shouldPause = enchantableFurnace.shouldPause(furnace, inventory.getSmelting(), inventory.getResult());
      if (enchantableFurnace.isPaused() == shouldPause) {
        enchantableFurnace.updating = false;
        return;
      }
      if (enchantableFurnace.isPaused()) {
        enchantableFurnace.resume();
      } else {
        enchantableFurnace.pause();
      }
      enchantableFurnace.updating = false;
    });
  }

}
