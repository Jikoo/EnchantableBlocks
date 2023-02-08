package com.github.jikoo.enchantableblocks.registry;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Register and retrieve information about enchantable blocks.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableBlockRegistryTest {

  @DisplayName("Registration stores by material and allows overrides.")
  @Test
  void testRegisterAndGet() {
    var plugin = mock(Plugin.class);

    var config = new YamlConfiguration();
    var configFile = Path.of(".", "src", "test", "resources", "EnchantableBlocks", "config.yml").toFile();
    assertDoesNotThrow(() -> config.load(configFile));
    when(plugin.getConfig()).thenReturn(config);

    var logger = mock(Logger.class);
    when(plugin.getLogger()).thenReturn(logger);

    EnchantableBlockRegistry registry = new EnchantableBlockRegistry(plugin);
    EnchantableRegistration registration = new DummyEnchantableRegistration(
        plugin,
        Set.of(Enchantment.DIG_SPEED, Enchantment.LOOT_BONUS_BLOCKS),
        EnumSet.of(Material.FURNACE, Material.ACACIA_WOOD)
    );

    assertDoesNotThrow(
        () -> registry.register(registration),
        "Valid registration must not throw errors.");
    registration.getMaterials().forEach(material ->
        assertThat(
            "Registration for material must match",
            registry.get(material),
            is(registration)));

    DummyEnchantableRegistration dummyReg = new DummyEnchantableRegistration(
        plugin,
        registration.getEnchants().stream().limit(1).collect(Collectors.toSet()),
        registration.getMaterials().stream().limit(1).collect(Collectors.toSet()));

    registry.register(dummyReg);

    // Ensure override count is correct
    verify(logger, times(dummyReg.getMaterials().size())).info(any(Supplier.class));

    dummyReg.getMaterials().forEach(material ->
        assertThat(
            "Registration for material must match",
            registry.get(material),
            is(dummyReg)));

    long count = registration.getMaterials().stream()
        .filter(material -> !registration.equals(registry.get(material))).count();

    assertThat("Override counts must match", count, equalTo((long) dummyReg.getMaterials().size()));
  }

  @DisplayName("Configuration load handles nonexistent sections gracefully.")
  @Test
  void testMissingConfig() {
    var noConfig = mock(Plugin.class);
    when(noConfig.getConfig()).thenReturn(new YamlConfiguration());
    EnchantableBlockRegistry registry = new EnchantableBlockRegistry(noConfig);
    var registration = new DummyEnchantableRegistration(noConfig, Set.of(), Set.of());
    registry.register(registration);
    noConfig.getConfig().set("blocks.DummyEnchantableBlock", null);
    assertDoesNotThrow(registration::getConfig, "Nonexistent sections must be handled gracefully");
  }

}