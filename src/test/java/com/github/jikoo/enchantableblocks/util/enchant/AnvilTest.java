package com.github.jikoo.enchantableblocks.util.enchant;

import be.seeseemelk.mockbukkit.MockBukkit;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * Unit tests for item combination.
 *
 * <p>As a developer, I want to be able to combine items and
 * enchanted books in the same fashion as an anvil.
 *
 * <p><b>Feature:</b> Calculate item combination results
 * <br><b>Given</b> I am a user
 * <br><b>When</b> I attempt to combine two items
 * <br><b>Then</b> the items should be combined in a vanilla fashion
 * <br><b>And</b> the combinations should ignore vanilla limitations where specified
 *
 * @author Jikoo
 */
@DisplayName("Feature: Calculate item combinations")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnvilTest {

    private static final Material BASE_MAT = Material.DIAMOND_SHOVEL;
    private static final Material REPAIR_MAT = Material.DIAMOND;

    @BeforeAll
    void beforeAll() {
        MockBukkit.mock();
        EnchantmentHelper.wrapCanEnchant();
        EnchantmentHelper.setupToolEnchants();
    }

    private static ItemStack maxDamageItem() {
        return prepareItem(new ItemStack(BASE_MAT), Material.DIAMOND_SHOVEL.getMaxDurability() - 1, 0);
    }

    private static ItemStack prepareItem(ItemStack itemStack, int damage, int repairCost) {
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (damage != 0) {
            Damageable damageable = requireDamageable(itemMeta);
            damageable.setDamage(damage);
        }

        if (repairCost != 0) {
            Repairable repairable = requireRepairable(itemMeta);
            repairable.setRepairCost(repairCost);
        }

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    private static Damageable requireDamageable(@Nullable ItemMeta itemMeta) {
        assertThat("Meta may not be null", itemMeta, notNullValue());
        assertThat("Item must be damageable", itemMeta, instanceOf(Damageable.class));

        return (Damageable) itemMeta;
    }

    private static Repairable requireRepairable(@Nullable ItemMeta itemMeta) {
        assertThat("Meta may not be null", itemMeta, notNullValue());
        assertThat("Item must be repairable", itemMeta, instanceOf(Repairable.class));

        return (Repairable) itemMeta;
    }

    private static void applyEnchantments(ItemStack itemStack, @NotNull Map<Enchantment, Integer> enchantments) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        assertThat("Meta may not be null", itemMeta, notNullValue());
        BiConsumer<Enchantment, Integer> metaAddEnchant = addEnchant(itemMeta);
        enchantments.forEach(metaAddEnchant);
        itemStack.setItemMeta(itemMeta);
    }

    private static BiConsumer<Enchantment, Integer> addEnchant(@NotNull ItemMeta meta) {
        if (meta instanceof EnchantmentStorageMeta) {
            return (enchantment, level) -> ((EnchantmentStorageMeta) meta).addStoredEnchant(enchantment, level, true);
        }

        return (enchantment, integer) -> meta.addEnchant(enchantment, integer, true);
    }

    @DisplayName("Damageable items should be repaired")
    @Nested
    class RepairTest {

        @DisplayName("Items should be repaired using repair material")
        @ParameterizedTest
        @ValueSource(ints = { 1, 64 })
        void testRepairWithMaterial(int repairMats) {
            ItemStack damagedStack = maxDamageItem();
            AnvilResult result = AnvilOperation.VANILLA.apply(damagedStack, new ItemStack(REPAIR_MAT, repairMats));

            ItemStack resultItem = result.getResult();
            int damage = requireDamageable(resultItem.getItemMeta()).getDamage();

            assertThat("Item should be fully repaired.", damage, is(0));
            assertThat("Number of items to consume should be specified",
                    result.getRepairCount(), greaterThan(0));
            assertThat("Number of items to consume should not exceed number of available items",
                    result.getRepairCount(), lessThanOrEqualTo(repairMats));
        }

        @DisplayName("Items should be repaired by combination")
        @Test
        void testRepairWithMerge() {
            ItemStack damagedStack = maxDamageItem();
            AnvilResult result = AnvilOperation.VANILLA.apply(damagedStack.clone(), damagedStack.clone());

            int damage = requireDamageable(damagedStack.getItemMeta()).getDamage();
            int maxDurability = damagedStack.getType().getMaxDurability();
            int remainingDurability = maxDurability - damage;
            int bonusDurability = maxDurability * 12 / 100;
            int expectedDurability = 2 * remainingDurability + bonusDurability;
            int expectedDamage = maxDurability - expectedDurability;

            damage = requireDamageable(result.getResult().getItemMeta()).getDamage();
            assertThat("Items' durability should be added with a bonus of 12% of max durability", damage, is(expectedDamage));
            assertThat("Number of items to consume should not be specified", result.getRepairCount(), is(0));
        }

    }

    @DisplayName("Enchantments on items should be combined")
    @Nested
    class CombineTest {

        @DisplayName("Enchantments from books should be applied")
        @Test
        void testCombineWithBook() {
            Map<Enchantment, Integer> enchantments = new HashMap<>();
            enchantments.put(Enchantment.DIG_SPEED, 5);
            enchantments.put(Enchantment.SILK_TOUCH, 1);

            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            applyEnchantments(book, enchantments);

            AnvilResult result = AnvilOperation.VANILLA.apply(new ItemStack(BASE_MAT), book);

            assertThat("Result must be of original type", result.getResult().getType(), equalTo(BASE_MAT));
            assertThat("Enchantments must be added to result", result.getResult().getEnchantments().entrySet(),
                    both(everyItem(is(in(enchantments.entrySet())))).and(containsInAnyOrder(enchantments.entrySet().toArray())));
            assertThat("Number of items to consume should not be specified", result.getRepairCount(), is(0));
            // TODO: verify values to ensure calculations are correct
            assertThat("Operation cost is correct", result.getCost(), is(9));
        }
        // TODO:
        //  Enchantments from similar items should be applied
        //  Enchantments from dissimilar items should be applied
        //  Enchantments should use the specified limitations

    }

    @DisplayName("Enchanted repair materials should be handled appropriately")
    @Nested
    class RepairAndCombineTest {

        // TODO:
        //  Enchantments should be combined during merge repair operations
        //  Enchantments from repair materials should be combined only if allowed to do so

    }

    @AfterAll
    void afterAll() {
        MockBukkit.unmock();
    }

}
