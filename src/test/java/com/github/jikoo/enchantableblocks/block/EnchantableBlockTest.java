package com.github.jikoo.enchantableblocks.block;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.jparams.verifier.tostring.ToStringVerifier;
import com.jparams.verifier.tostring.preset.Presets;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Enchantable blocks.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableBlockTest {

  @BeforeAll
  void setUpAll() {
    MockBukkit.mock();
  }

  @AfterAll
  void tearDownAll() {
    MockBukkit.unmock();
  }

  @DisplayName("Enchantable blocks should clone item and enforce quantity of 1")
  @Test
  void testSingleItemInConstruction() {
    var plugin = MockBukkit.createMockPlugin("EnchantableBlocks");
    var registration = new DummyEnchantableRegistration(
        plugin, Set.of(Enchantment.DIG_SPEED), Set.of(Material.DEEPSLATE));
    var block = MockBukkit.getMock().addSimpleWorld("world").getBlockAt(0, 0, 0);
    var itemStack = new ItemStack(Material.DEEPSLATE, 10);
    var storage = plugin.getConfig().createSection("going.to.the.store");
    var enchantableBlock = registration.newBlock(block, itemStack, storage);
    assertThat("Amount of item must be 1", enchantableBlock.getItemStack().getAmount(), is(1));
  }

  @DisplayName("Enchantable blocks should provide a descriptive toString")
  @Test
  void testToString() {
    ToStringVerifier.forPackage(
        "com.github.jikoo.enchantableblocks.block",
            true,
            EnchantableBlock.class::isAssignableFrom)
        .withPreset(Presets.INTELLI_J)
        .withIgnoredFields("registration", "storage", "dirty", "updating")
        .withFailOnExcludedFields(true).verify();
  }

}