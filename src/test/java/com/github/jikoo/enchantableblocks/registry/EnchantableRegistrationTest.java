package com.github.jikoo.enchantableblocks.registry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Feature: Registration for enchantable blocks.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableRegistrationTest {

  private EnchantableRegistration registration;

  @BeforeAll
  void setUpAll() {
    var plugin = mock(Plugin.class);
    when(plugin.getName()).thenReturn(getClass().getSimpleName());
    registration = new DummyEnchantableRegistration(plugin, Set.of(), Set.of());
  }

  @DisplayName("Permission or parent permissions grant access.")
  @ParameterizedTest
  @MethodSource("getPermissionsAndParents")
  void testHasPermission(String node) {
    var player = mock(Player.class);
    when(player.hasPermission(anyString())).thenAnswer(invocation -> node.equals(invocation.getArgument(0)));
    when(player.isPermissionSet(anyString())).thenAnswer(invocation -> node.equals(invocation.getArgument(0)));

    assertThat("Permission must be true", registration.hasEnchantPermission(player, "test"));
  }

  Collection<String> getPermissionsAndParents() {
    String[] segments = new String[] {
        getClass().getSimpleName().toLowerCase(),
        "enchant",
        "test",
        registration.getBlockClass().getSimpleName().toLowerCase()
    };

    Collection<String> strings = new ArrayList<>();

    StringBuilder builder = new StringBuilder();
    for (String segment : segments) {
      if (!builder.isEmpty()) {
        builder.append('.');
      }
      builder.append(segment);
      strings.add(builder.toString());
    }

    return strings;
  }

  @DisplayName("Negated permission does not grant access.")
  @Test
  void testNegatedPermission() {
    var player = mock(Player.class);
    // Return false only for first node checked, all others true.
    when(player.hasPermission(anyString())).thenReturn(false, true);
    when(player.isPermissionSet(anyString())).thenReturn(true);

    assertThat("Permission must be false", !registration.hasEnchantPermission(player, "test"));
  }

  @DisplayName("No permission does not grant access.")
  @Test
  void testUnsetPermission() {
    var player = mock(Player.class);
    assertThat("Permission must be false", !registration.hasEnchantPermission(player, "test"));
  }

}