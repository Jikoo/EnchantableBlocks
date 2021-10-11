package com.github.jikoo.enchantableblocks.registry;

import static org.hamcrest.MatcherAssert.assertThat;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import java.util.Set;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Feature: Registration for enchantable blocks.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableRegistrationTest {

  private ServerMock server;
  private EnchantableRegistration registration;

  @BeforeAll
  void setUpAll() {
    server = MockBukkit.mock();
    Plugin plugin = MockBukkit.createMockPlugin("EnchantableBlocks");
    registration = new DummyEnchantableRegistration(plugin, Set.of(), Set.of());
  }

  @AfterAll
  void tearDownAll() {
    MockBukkit.unmock();
  }

  @DisplayName("Permission or parent permissions grant access.")
  @ParameterizedTest
  @ValueSource(strings = {
      "enchantableblocks", "enchantableblocks.enchant", "enchantableblocks.enchant.test",
      "enchantableblocks.enchant.test.dummyenchantableblock"
  })
  void testHasPermission(String node) {
    var player = new PlayerMock(server, "sampletext") {
      @Override
      public boolean hasPermission(String name) {
        return node.equals(name);
      }

      @Override
      public boolean isPermissionSet(String name) {
        return node.equals(name);
      }
    };

    assertThat("Permission must be true", registration.hasEnchantPermission(player, "test"));
  }

  @DisplayName("Negated permission does not grant access.")
  @Test
  void testMissingPermission() {
    var player = new PlayerMock(server, "sampletext") {
      int count = 0;
      @Override
      public boolean hasPermission(String name) {
        // Return false only for first node checked, all others true.
        ++count;
        return count > 1;
      }

      @Override
      public boolean isPermissionSet(String name) {
        return true;
      }
    };

    assertThat("Permission must be false", !registration.hasEnchantPermission(player, "test"));
  }

  @DisplayName("No permission does not grant access.")
  @Test
  void testNoPermission() {
    var player = new PlayerMock(server, "sampletext") {
      @Override
      public boolean hasPermission(String name) {
        return false;
      }
    };

    assertThat("Permission must be false", !registration.hasEnchantPermission(player, "test"));
  }

}