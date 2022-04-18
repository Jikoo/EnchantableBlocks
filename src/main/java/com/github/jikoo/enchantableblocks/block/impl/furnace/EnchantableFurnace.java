package com.github.jikoo.enchantableblocks.block.impl.furnace;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import com.github.jikoo.enchantableblocks.util.MathHelper;
import com.github.jikoo.planarenchanting.util.ItemUtil;
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
 * Track and manage effects for enchanted furnace variants.
 */
class EnchantableFurnace extends EnchantableBlock {

  private static final String PATH_CAN_PAUSE = "silk.enabled";
  private static final String PATH_FROZEN_TICKS = "silk.ticks";

  private final boolean canPause;
  private short frozenTicks;
  private boolean updating = false;

  /**
   * Construct a new {@code EnchantableFurnace} instance.
   *
   * @param registration the {@link EnchantableFurnaceRegistration} creating the instance
   * @param block the in-world {@link Block}
   * @param itemStack the {@link ItemStack} used in creation
   * @param storage the {@link ConfigurationSection} containing save data
   */
  EnchantableFurnace(
      final @NotNull EnchantableFurnaceRegistration registration,
      final @NotNull Block block,
      @NotNull ItemStack itemStack,
      final @NotNull ConfigurationSection storage) {
    super(registration, block, itemStack, storage);
    if (storage.isBoolean(PATH_CAN_PAUSE)) {
      // Existing furnace, use stored data.
      this.canPause = storage.getBoolean(PATH_CAN_PAUSE, false);
      this.frozenTicks = MathHelper.clampPositiveShort(storage.getInt(PATH_FROZEN_TICKS, 0));
    } else {
      // New or legacy furnace.
      itemStack = this.getItemStack();
      this.canPause = itemStack.getEnchantments().containsKey(Enchantment.SILK_TOUCH);
      this.frozenTicks = 0;
      // Convert legacy furnaces - silk enchant level used for frozen ticks.
      if (this.canPause && itemStack.getEnchantmentLevel(Enchantment.SILK_TOUCH) != 1) {
        itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
        this.frozenTicks = MathHelper.clampPositiveShort(
            itemStack.getEnchantmentLevel(Enchantment.SILK_TOUCH));
      }
      storage.set(PATH_CAN_PAUSE, canPause);
      storage.set(PATH_FROZEN_TICKS, frozenTicks);
      this.setDirty(true);
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
      // Null furnace tile means no handling. Shouldn't happen, but rare edge cases occur.
      return false;
    }

    ItemStack input;
    ItemStack result;
    if (event instanceof FurnaceSmeltEvent smeltEvent) {
      // Special case FurnaceSmeltEvent: smelt has not completed, input and result are different.
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
   * Get whether the furnace should pause based on the input and result items. Note that depending
   * on the event occurring, the input and result items may not match the current furnace contents.
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
    if (!ItemUtil.isEmpty(result)) {
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
        || (result != null
        && result.getType() != Material.AIR
        && !recipe.getResult().isSimilar(result));

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

    this.setFrozenTicks(furnace.getBurnTime());
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
    if (ItemUtil.isEmpty(furnaceInv.getSmelting())) {
      return false;
    }

    // Is the output full?
    ItemStack result = furnaceInv.getResult();
    if (!ItemUtil.isEmpty(result)
        && result.getAmount() == result.getType().getMaxStackSize()) {
      return false;
    }

    CookingRecipe<?> recipe = getRegistration().getFurnaceRecipe(furnaceInv);

    if (recipe == null) {
      return false;
    }

    // Ensure result matches current output
    if (!ItemUtil.isEmpty(result) && !recipe.getResult().isSimilar(result)) {
      return false;
    }

    furnace.setBurnTime(
        MathHelper.clampPositiveShort(((long) furnace.getBurnTime()) + this.getFrozenTicks()));
    furnace.update(true);
    this.setFrozenTicks((short) 0);
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
    this.setFrozenTicks((short) 0);
    this.updateStorage();
    return true;
  }

  /**
   * Check if the furnace is paused.
   *
   * @return true if the furnace is paused
   */
  public boolean isPaused() {
    return this.canPause && this.frozenTicks > 0;
  }

  /**
   * Set the number of ticks the furnace is paused for.
   *
   * @param frozenTicks the number of ticks to freeze
   */
  @VisibleForTesting
  void setFrozenTicks(short frozenTicks) {
    this.frozenTicks = frozenTicks;
    this.getStorage().set(PATH_FROZEN_TICKS, this.frozenTicks);
    this.setDirty(true);
  }

  /**
   * Get the number of ticks of fuel remaining when the furnace is unpaused.
   *
   * @return the number of ticks of fuel
   */
  @VisibleForTesting
  short getFrozenTicks() {
    return this.frozenTicks;
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
    return MathHelper.clampPositiveShort(
        MathHelper.sigmoid(totalCookTime, -getCookModifier(), 2.0));
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
    return "EnchantableFurnace{"
        + "block=" + getBlock()
        + "itemStack=" + getItemStack()
        + "canPause=" + canPause
        + "frozenTicks=" + frozenTicks
        + '}';
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
      boolean shouldPause =
          enchantableFurnace.shouldPause(furnace, inventory.getSmelting(), inventory.getResult());
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
