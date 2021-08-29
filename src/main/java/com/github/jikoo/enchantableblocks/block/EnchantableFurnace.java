package com.github.jikoo.enchantableblocks.block;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.config.EnchantableFurnaceConfig;
import com.github.jikoo.enchantableblocks.util.EmptyCookingRecipe;
import com.github.jikoo.planarwrappers.util.StringConverters;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.block.Smoker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Track and manage effects for enchanted furnaces.
 */
public class EnchantableFurnace extends EnchantableBlock {

	public static final Set<Material> MATERIALS = Collections.unmodifiableSet(EnumSet.of(Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER));
	public static final Collection<Enchantment> ENCHANTMENTS = List.of(Enchantment.DIG_SPEED, Enchantment.DURABILITY, Enchantment.LOOT_BONUS_BLOCKS, Enchantment.SILK_TOUCH);
	private static final Map<Integer, CookingRecipe<?>> BLASTING_RECIPES = new Int2ObjectOpenHashMap<>();
	private static final Map<Integer, CookingRecipe<?>> SMOKING_RECIPES = new Int2ObjectOpenHashMap<>();
	private static final Map<Integer, CookingRecipe<?>> FURNACE_RECIPES = new Int2ObjectOpenHashMap<>();
	private static final CookingRecipe<?> INVALID_INPUT = new EmptyCookingRecipe(
			Objects.requireNonNull(StringConverters.toNamespacedKey("enchantableblocks:invalid_input")));

	private static @Nullable EnchantableFurnaceConfig config;

	private final boolean canPause;
	private boolean updating = false;

	public EnchantableFurnace(final @NotNull Block block, final @NotNull ItemStack itemStack,
			final @NotNull ConfigurationSection storage) {
		super(block, itemStack, storage);
		this.canPause = itemStack.getEnchantments().containsKey(Enchantment.SILK_TOUCH);
		if (this.canPause && itemStack.getEnchantmentLevel(Enchantment.SILK_TOUCH) == 1) {
			// New furnaces shouldn't get 1 tick flame for free, but old furnaces need to re-light
			itemStack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 0);
			this.updateStorage();
		}
	}

	/**
	 * Get the {@link Furnace} for this {@code EnchantableFurnace}.
	 *
	 * @return the {@link Furnace} or {@code null} if not found
	 */
	public @Nullable Furnace getFurnaceTile() {
		BlockState state = this.getBlock().getState();
		return state instanceof Furnace ? (Furnace) state : null;
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
	 * @deprecated use {@link #shouldPause(Event)}
	 */
	@SuppressWarnings("unused")
	@Deprecated(since = "3.0.1", forRemoval = true)
	public boolean shouldPause(final @Nullable Event event, @Nullable CookingRecipe<?> recipe) {
		return shouldPause(event);
	}

	/**
	 * Get whether the furnace should pause after completion of the event occurring.
	 *
	 * @param event the event occurring
	 * @return true if the furnace should pause
	 */
	public boolean shouldPause(final @Nullable Event event) {
		if (!this.canPause) {
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
			final @Nullable Furnace furnace,
			final @Nullable ItemStack input,
			final @Nullable ItemStack result) {
		if (!this.canPause || furnace == null) {
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
		if (result != null) {
			int stack = result.getType().getMaxStackSize();
			if (result.getAmount() >= stack) {
				return true;
			}
		}

		CookingRecipe<?> recipe = getFurnaceRecipe(furnace.getInventory());

		// Does the current smelting item not have a recipe?
		if (recipe == null) {
			return true;
		}

		// Verify that the smelting item cannot produce a result
		return !recipe.getInputChoice().test(input)
				|| (result != null && result.getType() != Material.AIR
				&& !recipe.getResult().isSimilar(result));

	}

	/**
	 * Attempt to pause the furnace.
	 */
	public void pause() {
		if (!this.canPause || this.getFrozenTicks() > 0) {
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

	public boolean resume() {
		Furnace furnace = this.getFurnaceTile();
		// Is furnace unfrozen already?
		if (furnace == null || furnace.getBurnTime() > 0 || this.getFrozenTicks() < 1) {
			return false;
		}

		// Is there an input?
		FurnaceInventory furnaceInv = furnace.getInventory();
		if (furnaceInv.getSmelting() == null) {
			return false;
		}

		// Is the output full?
		if (furnaceInv.getResult() != null && furnaceInv.getResult().getAmount() == furnaceInv.getResult().getType().getMaxStackSize()) {
			return false;
		}

		CookingRecipe<?> recipe = getFurnaceRecipe(furnaceInv);

		if (recipe == null) {
			return false;
		}

		// Ensure result matches current output
		if ((furnaceInv.getResult() != null && furnaceInv.getResult().getType() != Material.AIR
				&& !recipe.getResult().isSimilar(furnaceInv.getResult()))) {
			return false;
		}

		furnace.setBurnTime(this.getFrozenTicks());
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
	private short getFrozenTicks() {
		return (short) this.getItemStack().getEnchantmentLevel(Enchantment.SILK_TOUCH);
	}

	@Override
	public boolean isCorrectType(final @NotNull Material type) {
		return isApplicableMaterial(type);
	}

	public static boolean isApplicableMaterial(Material material) {
		return MATERIALS.contains(material);
	}

	/**
	 * @deprecated {@link #applyCookTimeModifiers(int)}
	 */
	@Deprecated(since = "3.0.1", forRemoval = true)
	public void setCookTimeTotal(@NotNull CookingRecipe<?> recipe) {
		if (this.getCookModifier() == 0) {
			return;
		}

		Furnace furnace = this.getFurnaceTile();

		if (furnace == null) {
			return;
		}

		furnace.setCookTimeTotal(applyCookTimeModifiers(recipe.getCookingTime()));
		furnace.update();
	}

	/**
	 * Apply cook time modifiers to a cook time total. Caps to a value between {@code 0} and {@link Short#MAX_VALUE} to
	 * not cause issues with furnaces.
	 *
	 * @param totalCookTime the original total cook time
	 * @return the modified cook time
	 */
	public int applyCookTimeModifiers(int totalCookTime) {
		return getCappedTicks(totalCookTime, this.getCookModifier(), 0.5);
	}

	/**
	 * Apply burn time modifiers to a burn time total. Caps to a value between {@code 0} and {@link Short#MAX_VALUE} to
	 * not cause issues with furnaces.
	 *
	 * @param burnTime the original burn time
	 * @return the modified burn time
	 */
	public int applyBurnTimeModifiers(int burnTime) {
		// Unbreaking causes furnace to burn for longer, increase burn time
		burnTime = getCappedTicks(burnTime, -getBurnModifier(), 0.2);
		// Efficiency causes furnace to cook at different rates, change burn time to match cook rate change
		return applyCookTimeModifiers(burnTime);
	}

	/**
	 * Modify and sanitize ticks using a fractional ratio.
	 *
	 * @param baseTicks the base number of ticks
	 * @param baseModifier the base modifier
	 * @param fractionModifier the fractional increase
	 * @return the sanitized value
	 */
	private static int getCappedTicks(final int baseTicks, final int baseModifier, final double fractionModifier) {
		return Math.max(1, Math.min(Short.MAX_VALUE, getModifiedTicks(baseTicks, baseModifier, fractionModifier)));
	}

	/**
	 * Modify ticks based on a modifier and a fractional per-level increase.
	 *
	 * @param baseTicks the base number of ticks
	 * @param baseModifier the base modifier
	 * @param fractionModifier the fractional increase
	 * @return the modified value
	 */
	private static int getModifiedTicks(final int baseTicks, final int baseModifier, final double fractionModifier) {
		if (baseModifier == 0) {
			return baseTicks;
		}
		if (baseModifier > 0) {
			return (int) (baseTicks / (1 + baseModifier * fractionModifier));
		}
		return (int) (baseTicks * (1 - baseModifier * fractionModifier));
	}

	/**
	 * Update the state of a potential {@code EnchantableFurnace}.
	 *
	 * @param plugin the {@link EnchantableBlocksPlugin} instance
	 * @param inventory the furnace inventory that may need an update
	 */
	public static void update(EnchantableBlocksPlugin plugin, FurnaceInventory inventory) {

		if (inventory.getHolder() == null) {
			return;
		}

		EnchantableBlock enchantableBlock = plugin.getEnchantableBlockByBlock(inventory.getHolder().getBlock());

		if (!(enchantableBlock instanceof EnchantableFurnace enchantableFurnace)) {
			return;
		}

		if (enchantableFurnace.updating || !enchantableFurnace.canPause()) {
			return;
		}

		enchantableFurnace.updating = true;

		plugin.getServer().getScheduler().runTask(plugin, () -> {
			boolean shouldPause = enchantableFurnace.shouldPause(inventory.getHolder(), inventory.getSmelting(), inventory.getResult());
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

	/**
	 * Get a {@link CookingRecipe} for a {@link FurnaceInventory}'s state.
	 *
	 * @param inventory the {@link FurnaceInventory}
	 * @return the {@link CookingRecipe} or {@code null} if no valid recipe is found
	 */
	public static @Nullable CookingRecipe<?> getFurnaceRecipe(@NotNull FurnaceInventory inventory) {
		ItemStack smelting = inventory.getSmelting();
		if (smelting == null) {
			return null;
		}

		ItemStack cacheData = smelting.clone();
		cacheData.setAmount(1);
		Integer cacheId = cacheData.hashCode();
		Map<Integer, CookingRecipe<?>> recipes;
		if (inventory.getHolder() instanceof BlastFurnace) {
			recipes = BLASTING_RECIPES;
		} else if (inventory.getHolder() instanceof Smoker) {
			recipes = SMOKING_RECIPES;
		} else {
			recipes = FURNACE_RECIPES;
		}

		CookingRecipe<?> recipe = recipes.computeIfAbsent(cacheId, key -> locateRecipe(inventory.getHolder(), smelting));

		if (!recipe.getInputChoice().test(smelting)) {
			return null;
		}

		return recipe;
	}

	/**
	 * Match a {@link CookingRecipe} for a particular {@link ItemStack} in an inventory belonging to a specific
	 * {@link InventoryHolder}.
	 *
	 * @param holder the inventory holder
	 * @param smelting the recipe input
	 * @return the {@link CookingRecipe} or a default invalid recipe if no match was found
	 */
	private static @NotNull CookingRecipe<?> locateRecipe(@Nullable InventoryHolder holder, @NotNull ItemStack smelting) {
		Iterator<Recipe> iterator = Bukkit.recipeIterator();
		while (iterator.hasNext()) {
			Recipe next = iterator.next();

			if (!(next instanceof CookingRecipe nextCooking) || isIneligibleRecipe(holder, next)) {
				continue;
			}

			if (nextCooking.getInputChoice().test(smelting)) {
				return nextCooking;
			}
		}

		return INVALID_INPUT;
	}

	/**
	 * Check if an {@link InventoryHolder} is eligible to use a certain {@link Recipe}.
	 * @param holder the inventory holder
	 * @param recipe the recipe
	 * @return true if the holder is allowed to use the recipe
	 */
	private static boolean isIneligibleRecipe(@Nullable InventoryHolder holder, @NotNull Recipe recipe) {
		if (holder instanceof BlastFurnace) {
			return !(recipe instanceof BlastingRecipe);
		}
		if (holder instanceof Smoker) {
			return !(recipe instanceof SmokingRecipe);
		}
		return holder instanceof Furnace && !(recipe instanceof FurnaceRecipe);
	}

	/**
	 * Clear cached recipes and parsed configuration nodes.
	 */
	public static void clearCache() {
		BLASTING_RECIPES.clear();
		SMOKING_RECIPES.clear();
		FURNACE_RECIPES.clear();
		config = null;
	}

	/**
	 * Get the configuration for this block type.
	 *
	 * @return the configuration
	 */
	public static @NotNull EnchantableFurnaceConfig getConfig() {
		if (config == null) {
			config = new EnchantableFurnaceConfig(JavaPlugin.getPlugin(EnchantableBlocksPlugin.class).getConfig());
		}
		return config;
	}

}
