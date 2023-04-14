package com.github.jikoo.enchantableblocks.registry;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Register and retrieve information about enchantable blocks.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableBlockRegistryTest {

  private Logger logger;
  private EnchantableBlockRegistry registry;

  @BeforeEach
  void beforeEach() {
    logger = mock(Logger.class);
    registry = new EnchantableBlockRegistry(logger);
  }

  @DisplayName("Registration stores by material.")
  @Test
  void testRegisterAndGet() {
    var registration = mock(EnchantableRegistration.class);
    doReturn(Set.of(Material.FURNACE, Material.ACACIA_WOOD)).when(registration).getMaterials();

    assertDoesNotThrow(
        () -> registry.register(registration),
        "Valid registration must not throw errors.");
    registration.getMaterials().forEach(material ->
        assertThat(
            "Registration for material must match",
            registry.get(material),
            is(registration)));
  }

  @DisplayName("Registration allows overrides.")
  @Test
  void testOverride() {
    var registration = mock(EnchantableRegistration.class);
    Material mutualMat = Material.FURNACE;
    doReturn(Set.of(mutualMat, Material.ACACIA_WOOD)).when(registration).getMaterials();
    doReturn(EnchantableBlock.class).when(registration).getBlockClass();
    registry.register(registration);
    var registration2 = mock(EnchantableRegistration.class);
    doReturn(Set.of(mutualMat)).when(registration2).getMaterials();
    doReturn(EnchantableBlock.class).when(registration2).getBlockClass();

    // Check logging content.
    doAnswer(invocation -> {
      Object supplied = invocation.getArgument(0, Supplier.class).get();
      assertThat(
          "Override must be logged as expected",
          supplied,
          is("EnchantableBlock overrode EnchantableBlock for type " + mutualMat.getKey()));
      return null;
    }).when(logger).info(any(Supplier.class));

    registry.register(registration2);

    // Ensure override count is correct.
    verify(logger, times(registration2.getMaterials().size())).info(any(Supplier.class));

    registration2.getMaterials().forEach(material ->
        assertThat(
            "Registration for material must match",
            registry.get(material),
            is(registration2)));

    long count = registration.getMaterials().stream()
        .filter(material -> !registration.equals(registry.get(material))).count();

    assertThat("Override counts must match", count, equalTo((long) registration2.getMaterials().size()));
  }

  @DisplayName("Reloading registry reloads all registrations.")
  @Test
  void testReloadRegistry() {
    var registration = mock(EnchantableRegistration.class);
    when(registration.getMaterials()).thenReturn(Set.of(Material.DIRT));
    registry.register(registration);
    verify(registration, times(0)).reload();
    registry.reload();
    verify(registration).reload();
  }

}