package com.github.jikoo.enchantableblocks.util.enchant;

import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.mock.ServerMocks;
import com.github.jikoo.enchantableblocks.mock.enchantments.EnchantmentMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import com.github.jikoo.planarenchanting.util.MetaCachedStack;
import com.github.jikoo.planarwrappers.config.Mapping;
import com.github.jikoo.planarwrappers.config.Setting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DisplayName("Feature: Configurable AnvilBehavior for EnchantableBlocks.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlockAnvilBehaviorTest {

  private EnchantableRegistration registration;

  @BeforeAll
  void beforeAll() {
    ServerMocks.mockServer();
    EnchantmentMocks.init();
  }

  @BeforeEach
  void beforeEach() {
    registration = mock(EnchantableRegistration.class);
    doReturn(Set.of()).when(registration).getEnchants();
    var config = mock(EnchantableBlockConfig.class);
    doReturn(config).when(registration).getConfig();
    var disabledEnchants = mock(Setting.class);
    doReturn(Set.of()).when(disabledEnchants).get(anyString());
    doReturn(disabledEnchants).when(config).anvilDisabledEnchants();
    var conflicts = mock(Setting.class);
    doReturn(conflicts).when(config).anvilEnchantmentConflicts();
    var levelMax = mock(Mapping.class);
    doReturn(levelMax).when(config).anvilEnchantmentMax();
  }

  @Test
  void enchantApplies() {
    doReturn(Set.of(Enchantment.UNBREAKING, Enchantment.EFFICIENCY)).when(registration).getEnchants();
    var anvilDisabledEnchants = registration.getConfig().anvilDisabledEnchants();
    doReturn(Set.of(Enchantment.UNBREAKING)).when(anvilDisabledEnchants).get(anyString());

    var op = new BlockAnvilBehavior(registration, "sample_text");

    assertThat(
        "Enabled enchantment applies",
        op.enchantApplies(Enchantment.EFFICIENCY, new MetaCachedStack(new ItemStack(Material.AIR))));
    assertThat(
        "Disabled enchantment does not apply",
        op.enchantApplies(Enchantment.UNBREAKING, new MetaCachedStack(new ItemStack(Material.AIR))),
        is(false));
  }

  @Test
  void enchantsConflict() {
    var anvilEnchantmentConflicts = registration.getConfig().anvilEnchantmentConflicts();
    Multimap<Enchantment, Enchantment> conflicts = HashMultimap.create();
    conflicts.put(Enchantment.UNBREAKING, Enchantment.EFFICIENCY);
    doReturn(conflicts).when(anvilEnchantmentConflicts).get(anyString());

    var op = new BlockAnvilBehavior(registration, "sample_text");

    assertThat(
        "Enchantments conflict",
        op.enchantsConflict(Enchantment.UNBREAKING, Enchantment.EFFICIENCY));
    assertThat(
        "Inverted enchantments conflict",
        op.enchantsConflict(Enchantment.EFFICIENCY, Enchantment.UNBREAKING));
    assertThat(
        "Other enchantments do not conflict",
        op.enchantsConflict(Enchantment.EFFICIENCY, Enchantment.SILK_TOUCH),
        is(false));
    assertThat(
        "Inverted other enchantments do not conflict",
        op.enchantsConflict(Enchantment.SILK_TOUCH, Enchantment.EFFICIENCY),
        is(false));
  }

  @Test
  void enchantMaxLevel() {
    var anvilEnchantmentMax = registration.getConfig().anvilEnchantmentMax();
    int genericEnchantLevel = 0;
    doReturn(genericEnchantLevel).when(anvilEnchantmentMax).get(anyString(), any());
    int specificEnchantLevel = 4;
    Enchantment specifiedEnchant = Enchantment.UNBREAKING;
    doReturn(specificEnchantLevel).when(anvilEnchantmentMax).get(anyString(), eq(specifiedEnchant));

    var op = new BlockAnvilBehavior(registration, "sample_text");

    assertThat(
        "Specified enchantment max is provided",
        op.getEnchantMaxLevel(specifiedEnchant),
        is(specificEnchantLevel));
    assertThat(
        "Generic max is used for other enchantments",
        op.getEnchantMaxLevel(Enchantment.EFFICIENCY),
        is(genericEnchantLevel));
  }

  @Test
  void itemsCombineEnchants() {
    var op = new BlockAnvilBehavior(registration, "sample_text");
    assertThat(
        "Items always combine",
        op.itemsCombineEnchants(new MetaCachedStack(new ItemStack(Material.AIR)), new MetaCachedStack(new ItemStack(Material.AIR))));
  }

  @Test
  void itemRepairedBy() {
    var op = new BlockAnvilBehavior(registration, "sample_text");
    assertThat(
        "Item is never repairable",
        !op.itemRepairedBy(new MetaCachedStack(new ItemStack(Material.AIR)), new MetaCachedStack(new ItemStack(Material.AIR))));
  }

}
