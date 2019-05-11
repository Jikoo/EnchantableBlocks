package com.github.jikoo.enchantableblocks.block;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Iterator;

/**
 * Class for tracking custom furnace properties and applying certain effects.
 *
 * @author Jikoo
 */
public class EnchantableFurnace extends EnchantableBlock {

	private static final EnumSet<Material> MATERIALS = EnumSet.of(Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER);
	private final boolean canPause;

	public EnchantableFurnace(final Block block, final ItemStack itemStack, ConfigurationSection storage) {
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

	public boolean shouldPause(final Event event, FurnaceRecipe recipe) {
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

		FurnaceRecipe recipe = getFurnaceRecipe(furnaceInv);

		if (recipe == null) {
			return false;
		}

		if (!recipe.getInputChoice().test(furnaceInv.getSmelting())
				|| (furnaceInv.getResult() != null && furnaceInv.getResult().getType() != Material.AIR
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
	public boolean isCorrectType(final Material type) {
		return isApplicableMaterial(type);
	}

	public static boolean isApplicableMaterial(Material material) {
		return MATERIALS.contains(material);
	}

	public void setCookTimeTotal(final int duration) {
		Furnace furnace = this.getFurnaceTile();
		if (furnace != null) {
			furnace.setCookTimeTotal(duration);
		}
	}

	public static @Nullable FurnaceRecipe getFurnaceRecipe(@NotNull FurnaceInventory inventory) {
		if (inventory.getSmelting() == null) {
			return null;
		}

		Iterator<Recipe> iterator = Bukkit.recipeIterator();
		FurnaceRecipe bestRecipe = null;
		while (iterator.hasNext()) {
			Recipe recipe = iterator.next();
			if (!(recipe instanceof FurnaceRecipe)) {
				continue;
			}

			FurnaceRecipe furnaceRecipe = ((FurnaceRecipe) recipe);

			if (furnaceRecipe.getInputChoice().test(inventory.getSmelting())) {
				bestRecipe = furnaceRecipe;
				break;
			}
		}

		return bestRecipe;

	}

}
