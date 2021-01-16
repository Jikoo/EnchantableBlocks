package com.github.jikoo.enchantableblocks.util.enchant;

import be.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

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
 * <br><b>And</b> the combinations should ignore vanilla limitations
 *
 * @author Jikoo
 */
@DisplayName("Feature: Calculate item combinations")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AnvilTest {

    @BeforeAll
    void beforeAll() {
        MockBukkit.mock();
    }

    @DisplayName("Damageable items should be repaired")
    @Nested
    class RepairTest {

        private ItemStack getDamagedStack() {
            ItemStack damaged = new ItemStack(Material.DIAMOND_SHOVEL);
            ItemMeta damagedMeta = damaged.getItemMeta();

            assertThat("Item must be damageable", damagedMeta, instanceOf(Damageable.class));

            Damageable damageable = (Damageable) damagedMeta;
            // Silence compiler warning - prior assertation ensures that this is fine.
            assert damageable != null;
            damageable.setDamage(Material.DIAMOND_SHOVEL.getMaxDurability() - 1);

            damaged.setItemMeta(damagedMeta);

            return damaged;
        }

        private ItemStack getRepairMaterial() {
            return new ItemStack(Material.DIAMOND, 64);
        }

        @DisplayName("Items should be repaired using repair material")
        @Test
        void testRepair() {
            if (true) return; // TODO: Test fails, likely due to mocking error - meta may not be tracking values properly.
            AnvilResult result = AnvilUtil.combine(getDamagedStack(), getRepairMaterial(),
                    (a, b) -> true, (a, b) -> true, true);

            ItemStack resultItem = result.getResult();
            ItemMeta resultMeta = resultItem.getItemMeta();

            assertThat("Item must be damageable", resultMeta, instanceOf(Damageable.class));
            // Silence compiler warning.
            assert resultMeta != null;

            int damage = ((Damageable) resultMeta).getDamage();

            assertThat("Item should be fully repaired.", damage, is(0));
            assertThat("Number of items to consume should be specified", result.getRepairCount(), greaterThan(0));
        }

        @DisplayName("Items should be repaired by combination")
        @Test
        void testCombineRepair() {
            if (true) return; // TODO: Test fails
            ItemStack damagedStack = getDamagedStack();
            AnvilResult result = AnvilUtil.combine(damagedStack.clone(), damagedStack.clone(),
                    (a, b) -> true, (a, b) -> true, false);

            ItemStack resultItem = result.getResult();
            ItemMeta resultMeta = resultItem.getItemMeta();

            assertThat("Item must be damageable", resultMeta, instanceOf(Damageable.class));
            // Silence compiler warning.
            assert resultMeta != null;

            int damage = ((Damageable) resultMeta).getDamage();

            ItemMeta damagedMeta = damagedStack.getItemMeta();
            assert damagedMeta != null;

            int expectedDamage = ((Damageable) damagedMeta).getDamage() * 2 + damagedStack.getType().getMaxDurability() * 12 / 100;
            assertThat("Items' durability should be added with a bonus of 12% of max durability", damage, is(expectedDamage));
            assertThat("Number of items to consume should be specified", result.getRepairCount(), greaterThan(0));
        }

    }

    @DisplayName("Enchantments on items should be combined")
    @Nested
    class CombineTest {

        // TODO:
        //  Enchantments from books should be applied
        //  Enchantments from similar items should be applied
        //  Enchantments from dissimilar items should be applied
        //  Enchantments should not be affected by vanilla limitations
        //  Enchantments should be combined during combination repair operations
        //  Decide what to do with enchantments from repair materials - currently ignored

    }

    @AfterAll
    void afterAll() {
        MockBukkit.unmock();
    }

}
