package com.github.jikoo.enchantableblocks.block;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.github.jikoo.enchantableblocks.mock.BukkitServer;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import com.jparams.verifier.tostring.ToStringVerifier;
import com.jparams.verifier.tostring.preset.Presets;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Enchantable blocks.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableBlockTest {

  @DisplayName("Enchantable blocks should clone item and enforce quantity of 1")
  @Test
  void testSingleItemInConstruction() {
    var plugin = mock(Plugin.class);
    var registration = new DummyEnchantableRegistration(
        plugin, Set.of(Enchantment.DIG_SPEED), Set.of(Material.DEEPSLATE));
    var block = WorldMocks.newWorld("world").getBlockAt(0, 0, 0);
    var itemStack = new ItemStack(Material.DEEPSLATE, 10);
    var enchantableBlock = registration.newBlock(block, itemStack, new YamlConfiguration());
    assertThat("Amount of item must be 1", enchantableBlock.getItemStack().getAmount(), is(1));
  }

  @DisplayName("Enchantable blocks should provide a descriptive toString")
  @Test
  void testToString() {
    var server = BukkitServer.newServer();
    Bukkit.setServer(server);
    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);

    ToStringVerifier.forPackage(
        "com.github.jikoo.enchantableblocks.block",
            true,
            EnchantableBlock.class::isAssignableFrom)
        .withPreset(Presets.INTELLI_J)
        .withIgnoredFields("registration", "storage", "dirty", "updating")
        .withFailOnExcludedFields(true).verify();
  }

}