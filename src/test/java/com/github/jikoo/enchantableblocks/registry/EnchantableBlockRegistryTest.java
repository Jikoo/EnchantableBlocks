package com.github.jikoo.enchantableblocks.registry;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.block.impl.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.github.jikoo.enchantableblocks.block.impl.EnchantableFurnaceRegistration;
import com.github.jikoo.enchantableblocks.util.PluginHelper;
import com.github.jikoo.enchantableblocks.util.logging.PatternCountHandler;
import java.util.stream.Collectors;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Register and retrieve information about enchantable blocks.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableBlockRegistryTest {

  private Plugin plugin;

  @BeforeAll
  void setUp() throws NoSuchFieldException, IllegalAccessException {
    MockBukkit.mock();
    EnchantableBlocksPlugin plugin = MockBukkit.load(EnchantableBlocksPlugin.class);
    PluginHelper.setDataDir(plugin);
    plugin.getBlockManager().reload();
    this.plugin = plugin;
  }

  @AfterAll
  void tearDown() {
    MockBukkit.unmock();
  }

  @DisplayName("Registration stores by material and allows overrides.")
  @Test
  void testRegisterAndGet() {
    EnchantableBlockRegistry registry = new EnchantableBlockRegistry(plugin);
    EnchantableFurnaceRegistration registration = new EnchantableFurnaceRegistration(plugin);

    assertDoesNotThrow(
        () -> registry.register(registration),
        "Valid registration must not throw errors.");
    registration.getMaterials().forEach(material ->
        assertThat(
            "Registration for material must match",
            registry.get(material),
            is(registration)));

    // Piggyback logger to count overrides
    PatternCountHandler handler = new PatternCountHandler(".* overrode .* for type .*");
    plugin.getLogger().addHandler(handler);

    DummyEnchantableRegistration dummyReg = new DummyEnchantableRegistration(
        plugin,
        registration.getEnchants().stream().limit(1).collect(Collectors.toSet()),
        registration.getMaterials().stream().limit(1).collect(Collectors.toSet()));

    registry.register(dummyReg);

    // Ensure override count is correct
    assertThat(
        "Registration must have overridden appropriate types",
        handler.getMatches(),
        is(dummyReg.getMaterials().size()));

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
    Plugin noConfig = MockBukkit.createMockPlugin();
    EnchantableBlockRegistry registry = new EnchantableBlockRegistry(noConfig);
    var registration = new EnchantableFurnaceRegistration(noConfig);
    registry.register(registration);
    noConfig.getConfig().set("blocks.EnchantableFurnace", null);
    assertDoesNotThrow(registration::getConfig, "Nonexistent sections must be handled gracefully");
  }

}