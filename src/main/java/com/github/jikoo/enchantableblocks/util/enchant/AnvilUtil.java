package com.github.jikoo.enchantableblocks.util.enchant;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for anvil-related functions.
 */
public final class AnvilUtil {

    private static final AnvilResult EMPTY = new AnvilResult();

    /**
     * Set the repair count of an anvil inventory. The repair count is the number of
     * items that will be consumed from the second slot when the result is removed.
     *
     * @param inventory the anvil inventory
     * @param repairCount the repair count
     * @throws ReflectiveOperationException if the repair count field has changed
     */
    public static void setRepairCount(AnvilInventory inventory, int repairCount) throws ReflectiveOperationException {
        Object containerAnvil = inventory.getClass().getDeclaredMethod("getHandle").invoke(inventory);
        Field fieldRepairCount = containerAnvil.getClass().getDeclaredField("h");
        fieldRepairCount.setAccessible(true);
        fieldRepairCount.set(containerAnvil, repairCount);
    }

    static @NotNull AnvilResult combine(
            @NotNull ItemStack base, @NotNull ItemStack addition, @NotNull AnvilOperation operation) {
        ItemMeta baseMeta = base.getItemMeta();
        ItemMeta additionMeta = addition.getItemMeta();

        // Items must support stored repair cost value.
        if (!(baseMeta instanceof Repairable && additionMeta instanceof Repairable)) {
            return EMPTY;
        }

        AnvilResult result = null;
        boolean appliedBaseCost = false;

        if (operation.isMergeRepairs() && canRepairWithMerge(base, addition)) {
            // Do repairs via merge of identical materials.
            result = repairWithMerge(base, addition);
        } else if (canRepairWithMaterial(base, addition, operation)) {
            // Do repairs via supported material, i.e. diamonds are used to repair a diamond shovel.
            result = repairWithMaterial(base, addition);
            appliedBaseCost = result != null;
        }

        // If the operation is supposed to support enchantment merges, material combination must work.
        if (!operation.isCombineEnchants() || !operation.getMaterialCombines().test(base, addition)) {
            return result == null ? EMPTY : result;
        }

        if (result == null) {
            result = new AnvilResult(base.clone(), getBaseCost(base, addition));
        } else if (!appliedBaseCost) {
            result = new AnvilResult(result.getResult(), getBaseCost(base, addition) + result.getCost(), result.getRepairCount());
        }

        AnvilResult combineResult = combineEnchantments(result, addition, operation);

        if (combineResult != EMPTY && combineResult.getResult().isSimilar(base)) {
            return EMPTY;
        }
        return combineResult;
    }

    private static int getBaseCost(@NotNull ItemStack base, @NotNull ItemStack addition) {
        Repairable baseRepairable = (Repairable) Objects.requireNonNull(base.getItemMeta());
        Repairable addedRepairable = (Repairable) Objects.requireNonNull(addition.getItemMeta());
        int cost = 0;
        if (baseRepairable.hasRepairCost()) {
            cost += baseRepairable.getRepairCost();
        }
        if (addedRepairable.hasRepairCost()) {
            cost += addedRepairable.getRepairCost();
        }
        return cost;
    }

    private static boolean canRepairWithMaterial(@NotNull ItemStack toRepair, @NotNull ItemStack consumed, @NotNull AnvilOperation operation) {
        return canRepair(toRepair, () -> operation.getMaterialRepairs().test(toRepair, consumed));
    }

    private static boolean canRepairWithMerge(@NotNull ItemStack toRepair, @NotNull ItemStack consumed) {
        return canRepair(toRepair, () -> toRepair.getType() == consumed.getType());
    }

    private static boolean canRepair(@NotNull ItemStack toRepair, @NotNull BooleanSupplier materialComparison) {
        ItemMeta itemMeta = Objects.requireNonNull(toRepair.getItemMeta());
        // Ensure item is damageable.
        if (toRepair.getType().getMaxDurability() == 0 || itemMeta.isUnbreakable()) {
            return false;
        }
        // Run material comparison.
        if (!materialComparison.getAsBoolean()) {
            return false;
        }
        // Ensure that damageable tools have damage.
        return itemMeta instanceof Damageable && ((Damageable) itemMeta).hasDamage();
    }

    private static @Nullable AnvilResult repairWithMaterial(@NotNull ItemStack base, @NotNull ItemStack added) {
        // Safe - ItemMeta is always a Damageable Repairable by this point.
        Damageable damageable = (Damageable) Objects.requireNonNull(base.getItemMeta()).clone();
        int repaired = Math.min(damageable.getDamage(), base.getType().getMaxDurability() / 4);

        if (repaired <= 0) {
            return null;
        }

        int repairs = 0;
        while (repaired > 0 && repairs < added.getAmount()) {
            damageable.setDamage(damageable.getDamage() - repaired);
            ++repairs;
            repaired = Math.min(damageable.getDamage(), base.getType().getMaxDurability() / 4);
        }

        ItemStack result = base.clone();
        Repairable repairable = (Repairable) damageable;
        int baseCost = getBaseCost(base, added);
        repairable.setRepairCost(baseCost + repairs);
        result.setItemMeta((ItemMeta) damageable);

        return new AnvilResult(result, baseCost + repairs, repairs);
    }

    private static AnvilResult repairWithMerge(@NotNull ItemStack base, @NotNull ItemStack addition) {
        Damageable damageable = (Damageable) Objects.requireNonNull(base.getItemMeta());
        Damageable addedDurability = (Damageable) Objects.requireNonNull(addition.getItemMeta());

        int finalDamage = damageable.getDamage();
        finalDamage -= addition.getType().getMaxDurability() - addedDurability.getDamage();
        finalDamage -= addition.getType().getMaxDurability() * 12 / 100;
        finalDamage = Math.max(finalDamage, 0);
        damageable.setDamage(finalDamage);

        base = base.clone();
        base.setItemMeta((ItemMeta) damageable);
        return new AnvilResult(base, 2);
    }

    private static AnvilResult combineEnchantments(
            @NotNull AnvilResult oldResult,
            @NotNull ItemStack addition,
            @NotNull AnvilOperation operation) {
        ItemStack base = oldResult.getResult();

        if (base.getType().isAir()) {
            return EMPTY;
        }

        Map<Enchantment, Integer> baseEnchants = new HashMap<>(getEnchants(Objects.requireNonNull(base.getItemMeta())));
        Map<Enchantment, Integer> addedEnchants = getEnchants(Objects.requireNonNull(addition.getItemMeta()));

        int cost = oldResult.getCost();
        boolean affected = false;
        for (Map.Entry<Enchantment, Integer> added : addedEnchants.entrySet()) {
            int newValue = added.getValue();
            int oldValue = baseEnchants.getOrDefault(added.getKey(), 0);
            newValue = oldValue == newValue ? oldValue + 1 : Math.max(oldValue, newValue);
            newValue = Math.min(newValue, operation.getEnchantMaxLevel().applyAsInt(added.getKey()));

            if (enchantIncompatible(base, added.getKey(), operation)) {
                continue;
            }

            affected = true;
            baseEnchants.put(added.getKey(), newValue);

            int costMod = getMultiplier(added.getKey(), addition.getType() != Material.ENCHANTED_BOOK);

            cost += newValue * Math.max(1, costMod);
        }

        if (!affected) {
            return oldResult.getCost() == 0 ? EMPTY : oldResult;
        }

        ItemMeta meta = base.getItemMeta();
        baseEnchants.forEach((enchant, level) -> meta.addEnchant(enchant, level, true));
        Repairable repairable = (Repairable) meta;
        repairable.setRepairCost(cost);
        base = base.clone();
        base.setItemMeta((ItemMeta) repairable);

        return new AnvilResult(base, cost, oldResult.getRepairCount());
    }

    private static Map<Enchantment, Integer> getEnchants(ItemMeta meta) {
        if (meta instanceof EnchantmentStorageMeta) {
            return ((EnchantmentStorageMeta) meta).getStoredEnchants();
        }
        return meta.getEnchants();
    }

    private static boolean enchantIncompatible(@NotNull ItemStack base,
            @NotNull Enchantment newEnchant,
            @NotNull AnvilOperation operation) {
        return !operation.getEnchantApplies().test(newEnchant, base)
                || base.getEnchantments().keySet().stream().anyMatch(enchantment ->
                        operation.getEnchantConflicts().test(enchantment, newEnchant));
    }

    private static int getMultiplier(@NotNull Enchantment enchantment, boolean notBook) {
        int value = EnchantData.of(enchantment).getRarity().getAnvilValue();

        if (notBook) {
            return value;
        }

        return value / 2;
    }

    private AnvilUtil() {}

}
