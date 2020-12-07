package com.github.jikoo.enchantableblocks.util.enchant;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.NotNull;

public final class AnvilUtil {

    private static final AnvilResult EMPTY = new AnvilResult();

    private static final Map<Material, RecipeChoice> MATERIALS_TO_REPAIRABLE = new EnumMap<>(Material.class);

    static {
        String[] armor = new String[] { "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS" };
        String[] tools = new String[] { "_AXE", "_SHOVEL", "_PICKAXE", "_HOE" };
        String[] armortools = new String[armor.length + tools.length];
        System.arraycopy(armor, 0, armortools, 0, armor.length);
        System.arraycopy(tools, 0, armortools, armor.length, tools.length);

        addGear("LEATHER", armor, Material.LEATHER);
        RecipeChoice choiceIronIngot = exactChoice(Material.IRON_INGOT);
        addGear("CHAINMAIL", armor, choiceIronIngot);
        addGear("IRON", armortools, choiceIronIngot);
        MATERIALS_TO_REPAIRABLE.put(Material.SHEARS, choiceIronIngot);
        addGear("GOLDEN", armortools, Material.GOLD_INGOT);
        addGear("DIAMOND", armortools, Material.DIAMOND);
        MATERIALS_TO_REPAIRABLE.put(Material.TURTLE_HELMET, exactChoice(Material.SCUTE));
        addGear("NETHERITE", armortools, Material.NETHERITE_INGOT);
        addGear("WOODEN", tools, new RecipeChoice.MaterialChoice(Tag.PLANKS));
        addGear("STONE", tools, new RecipeChoice.MaterialChoice(Tag.ITEMS_STONE_TOOL_MATERIALS));
    }

    private static void addGear(String type, String[] gearType, RecipeChoice repairChoice) {
        for (String toolType : gearType) {
            Material material = Material.getMaterial(type + toolType);
            if (material != null) {
                MATERIALS_TO_REPAIRABLE.put(material, repairChoice);
            }
        }
    }

    private static void addGear(String type, String[] gearType, Material repairMaterial) {
        addGear(type, gearType, exactChoice(repairMaterial));
    }

    private static RecipeChoice exactChoice(Material material) {
        // RecipeChoice.ExactChoice is draft API, just use singleton list instead.
        return new RecipeChoice.MaterialChoice(Collections.singletonList(material));
    }

    static boolean isRepairMaterial(Material material, ItemStack repairMat) {
        RecipeChoice choice = MATERIALS_TO_REPAIRABLE.get(material);
        return choice != null && choice.test(repairMat);
    }

    public static @NotNull AnvilResult combine(@NotNull ItemStack base, @NotNull ItemStack addition,
            @NotNull BiPredicate<ItemStack, Enchantment> itemCompat,
            @NotNull BiPredicate<Enchantment, Enchantment> enchantCompat,
            boolean doMaterialRepair) {
        ItemMeta baseMeta = base.getItemMeta();
        ItemMeta additionMeta = addition.getItemMeta();

        // Items must support stored repair cost value.
        if (!(baseMeta instanceof Repairable) || !(additionMeta instanceof Repairable)) {
            return EMPTY;
        }

        if (doMaterialRepair && canRepairMaterial(base, addition)) {
            return repairMaterial(base, addition);
        }

        AnvilResult result;
        if (canRepairCombine(base, addition)) {
            result = repairCombine(base, addition);
        } else {
            result = new AnvilResult(base.clone(), 0);
        }

        return combineEnchantments(result, addition, itemCompat, enchantCompat);
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

    private static boolean canRepairMaterial(@NotNull ItemStack toRepair, @NotNull ItemStack consumed) {
        return canRepair(toRepair, consumed, () -> isRepairMaterial(toRepair.getType(), consumed));
    }

    private static boolean canRepairCombine(@NotNull ItemStack toRepair, @NotNull ItemStack consumed) {
        return canRepair(toRepair, consumed, () -> {
            if (toRepair.getType() != consumed.getType()) {
                return false;
            }
            return !Objects.requireNonNull(consumed.getItemMeta()).isUnbreakable();
        });
    }

    private static boolean canRepair(@NotNull ItemStack toRepair, @NotNull ItemStack consumed,
            @NotNull BooleanSupplier materialComparison) {
        // Material must not match - matching materials are combination repairs.
        if (toRepair.getType() == consumed.getType()) {
            return false;
        }
        ItemMeta itemMeta = Objects.requireNonNull(toRepair.getItemMeta());
        // Ensure item is damageable.
        if (toRepair.getType().getMaxDurability() == 0 || itemMeta.isUnbreakable()) {
            return false;
        }
        // Run extra comparison.
        if (!materialComparison.getAsBoolean()) {
            return false;
        }
        // Ensure that damageable tools have damage.
        return itemMeta instanceof Damageable && ((Damageable) itemMeta).hasDamage();
    }

    private static AnvilResult repairMaterial(@NotNull ItemStack base, @NotNull ItemStack added) {
        // Safe - ItemMeta is always a Damageable Repairable by this point.
        Damageable damageable = (Damageable) Objects.requireNonNull(base.getItemMeta()).clone();
        int repaired = Math.min(damageable.getDamage(), base.getType().getMaxDurability() / 4);

        if (repaired <= 0) {
            return new AnvilResult();
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

    private static AnvilResult repairCombine(@NotNull ItemStack base, @NotNull ItemStack addition) {
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

    private static AnvilResult combineEnchantments(@NotNull AnvilResult oldResult, @NotNull ItemStack addition,
            @NotNull BiPredicate<ItemStack, Enchantment> itemCompat,
            @NotNull BiPredicate<Enchantment, Enchantment> enchantCompat) {
        ItemStack base = oldResult.getResult();

        Map<Enchantment, Integer> baseEnchants = getEnchants(Objects.requireNonNull(base.getItemMeta()));
        Map<Enchantment, Integer> addedEnchants = getEnchants(Objects.requireNonNull(addition.getItemMeta()));

        int cost = getBaseCost(base, addition) + oldResult.getCost();
        boolean affected = false;
        for (Map.Entry<Enchantment, Integer> added : addedEnchants.entrySet()) {
            if (!itemCompat.test(base, added.getKey())) {
                continue;
            }
            if (!baseEnchants.keySet().stream().allMatch(enchant -> enchantCompat.test(enchant, added.getKey()))) {
                continue;
            }
            affected = true;
            int newValue = added.getValue();
            int oldValue = baseEnchants.getOrDefault(added.getKey(), 0);
            newValue = oldValue == newValue ? oldValue + 1 : Math.max(oldValue, newValue);
            baseEnchants.put(added.getKey(), newValue);

            int costMod = EnchantData.of(added.getKey()).getRarity().getAnvilValue();
            if (addition.getType() == Material.ENCHANTED_BOOK) {
                costMod /= 2;
            }

            cost += newValue * Math.max(1, costMod);
        }

        if (!affected) {
            return EMPTY;
        }

        ItemMeta meta = base.getItemMeta();
        baseEnchants.forEach((enchant, level) -> meta.addEnchant(enchant, level, true));
        Repairable repairable = (Repairable) meta;
        repairable.setRepairCost(cost);
        base = base.clone();
        base.setItemMeta((ItemMeta) repairable);

        return new AnvilResult(base, cost);
    }

    private static Map<Enchantment, Integer> getEnchants(ItemMeta meta) {
        if (meta instanceof EnchantmentStorageMeta) {
            return ((EnchantmentStorageMeta) meta).getStoredEnchants();
        }
        return meta.getEnchants();
    }

    private static int getMultiplier(@NotNull Enchantment enchantment, boolean notBook) {
        int value = EnchantData.of(enchantment).getRarity().getAnvilValue();

        if (notBook) {
            return value;
        }

        return value / 2;
    }

    public static void setRepairCount(AnvilInventory inventory, int repairCount) throws ReflectiveOperationException {
        Object containerAnvil = inventory.getClass().getDeclaredMethod("getHandle").invoke(inventory);
        Field fieldRepairCount = containerAnvil.getClass().getDeclaredField("h");
        fieldRepairCount.setAccessible(true);
        fieldRepairCount.set(containerAnvil, repairCount);
    }

    private AnvilUtil() {}

}
