package com.github.jikoo.enchantableblocks.block;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.config.EnchantableFurnaceConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 * Class for tracking custom furnace properties and applying certain effects.
 *
 * @author Jikoo
 */
public class EnchantableFurnace extends EnchantableBlock {

	public static final Set<Material> MATERIALS = Collections.unmodifiableSet(EnumSet.of(Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER));
	public static final Collection<Enchantment> ENCHANTMENTS = List.of(Enchantment.DIG_SPEED, Enchantment.DURABILITY, Enchantment.LOOT_BONUS_BLOCKS, Enchantment.SILK_TOUCH);
	private static final Map<Integer, CookingRecipe<?>> BLASTING_RECIPES = new HashMap<>();
	private static final Map<Integer, CookingRecipe<?>> SMOKING_RECIPES = new HashMap<>();
	private static final Map<Integer, CookingRecipe<?>> FURNACE_RECIPES = new HashMap<>();

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

	public @Nullable Furnace getFurnaceTile() {
		BlockState state = this.getBlock().getState();
		return state instanceof Furnace ? (Furnace) state : null;
	}

	public int getCookModifier() {
		return this.getItemStack().getEnchantmentLevel(Enchantment.DIG_SPEED);
	}

	public int getBurnModifier() {
		return this.getItemStack().getEnchantmentLevel(Enchantment.DURABILITY);
	}

	public int getFortune() {
		return this.getItemStack().getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
	}

	public boolean canPause() {
		return this.canPause;
	}

	public boolean shouldPause(final @Nullable Event event, @Nullable CookingRecipe<?> recipe) {
		if (!this.canPause) {
			return false;
		}

		Furnace furnace = this.getFurnaceTile();
		if (furnace == null || furnace.getBurnTime() <= 0) {
			return false;
		}

		// Is there an input?
		FurnaceInventory furnaceInv = furnace.getInventory();
		if (furnaceInv.getSmelting() == null) {
			return true;
		}

		// Is the result slot too full for more product?
		if (furnaceInv.getResult() != null) {
			int stack = furnaceInv.getResult().getType().getMaxStackSize();
			if (event instanceof FurnaceSmeltEvent) {
				stack -= 1;
			}
			if (furnaceInv.getResult().getAmount() >= stack) {
				return true;
			}
		}

		// Will the input slot be empty once the FurnaceSmeltEvent has completed?
		if (event instanceof FurnaceSmeltEvent && furnaceInv.getSmelting() != null && furnaceInv.getSmelting().getAmount() == 1) {
			return true;
		}

		if (recipe == null) {
			recipe = getFurnaceRecipe(furnaceInv);
		}

		if (recipe == null) {
			return true;
		}

		// Verify that the smelting item cannot produce a result
		return !recipe.getInputChoice().test(furnaceInv.getSmelting())
				|| (furnaceInv.getResult() != null && furnaceInv.getResult().getType() != Material.AIR
				&& !recipe.getResult().isSimilar(furnaceInv.getResult()));

	}

	public void pause() {
		if (!this.canPause) {
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

	public boolean isPaused() {
		return this.canPause && this.getItemStack().getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0;
	}

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
	@Deprecated
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

	public int applyCookTimeModifiers(int totalCookTime) {
		return getCappedTicks(totalCookTime, this.getCookModifier(), 0.5);
	}

	public int applyBurnTimeModifiers(int burnTime) {
		// Unbreaking causes furnace to burn for longer, increase burn time
		burnTime = getCappedTicks(burnTime, -getBurnModifier(), 0.2);
		// Efficiency causes furnace to burn at different rates, change burn time to match smelt rate change
		return getCappedTicks(burnTime, getCookModifier(), 0.5);
	}

	private static int getCappedTicks(final int baseTicks, final int baseModifier, final double fractionModifier) {
		return Math.max(1, Math.min(Short.MAX_VALUE, getModifiedTicks(baseTicks, baseModifier, fractionModifier)));
	}

	private static int getModifiedTicks(final int baseTicks, final int baseModifier, final double fractionModifier) {
		if (baseModifier == 0) {
			return baseTicks;
		}
		if (baseModifier > 0) {
			return (int) (baseTicks / (1 + baseModifier * fractionModifier));
		}
		return (int) (baseTicks * (1 - baseModifier * fractionModifier));
	}

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
			boolean shouldPause = enchantableFurnace.shouldPause(null, null);
			if (enchantableFurnace.isPaused() == shouldPause) {
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

	public static @Nullable CookingRecipe<?> getFurnaceRecipe(@NotNull FurnaceInventory inventory) {
		if (inventory.getSmelting() == null) {
			return null;
		}

		ItemStack cacheData = inventory.getSmelting().clone();
		cacheData.setAmount(1);
		Integer cacheID = cacheData.hashCode();
		Map<Integer, CookingRecipe<?>> recipes;
		if (inventory.getHolder() instanceof BlastFurnace) {
			recipes = BLASTING_RECIPES;
		} else if (inventory.getHolder() instanceof Smoker) {
			recipes = SMOKING_RECIPES;
		} else {
			recipes = FURNACE_RECIPES;
		}

		if (recipes.containsKey(cacheID)) {
			CookingRecipe<?> recipe = recipes.get(cacheID);
			if (recipe == null || !recipe.getInputChoice().test(inventory.getSmelting())) {
				return null;
			}
			return recipe;
		}

		Iterator<Recipe> iterator = Bukkit.recipeIterator();
		while (iterator.hasNext()) {
			Recipe next = iterator.next();

			if (isIneligibleRecipe(inventory.getHolder(), next)) {
				continue;
			}

			CookingRecipe<?> recipe = (CookingRecipe<?>) next;

			if (recipe.getInputChoice().test(inventory.getSmelting())) {
				recipes.put(cacheID, recipe);
				return recipe;
			}
		}

		recipes.put(cacheID, null);
		return null;
	}

	private static boolean isIneligibleRecipe(@Nullable InventoryHolder holder, @NotNull Recipe recipe) {
		return !(recipe instanceof CookingRecipe)
				|| holder instanceof BlastFurnace && !(recipe instanceof BlastingRecipe)
				|| holder instanceof Smoker && !(recipe instanceof SmokingRecipe)
				|| holder instanceof Furnace && !(recipe instanceof FurnaceRecipe);
	}

	public static void clearCache() {
		BLASTING_RECIPES.clear();
		SMOKING_RECIPES.clear();
		FURNACE_RECIPES.clear();
		config = null;
	}

	public static @NotNull EnchantableFurnaceConfig getConfig() {
		if (config == null) {
			config = new EnchantableFurnaceConfig(JavaPlugin.getPlugin(EnchantableBlocksPlugin.class).getConfig());
		}
		return config;
	}

}
