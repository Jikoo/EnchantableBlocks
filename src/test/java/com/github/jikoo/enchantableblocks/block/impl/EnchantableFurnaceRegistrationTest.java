package com.github.jikoo.enchantableblocks.block.impl;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import com.github.jikoo.enchantableblocks.config.impl.EnchantableFurnaceConfig;
import com.github.jikoo.enchantableblocks.util.mock.BlastFurnaceMock;
import com.github.jikoo.enchantableblocks.util.mock.FurnaceMock;
import com.github.jikoo.enchantableblocks.util.mock.SmokerMock;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Feature: Registration for EnchantableFurnace.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableFurnaceRegistrationTest {

  private Plugin plugin;
  private EnchantableFurnaceRegistration registration;
  private Furnace[] furnaces;

  @BeforeAll
  void beforeAll() {
    ServerMock server = MockBukkit.mock();
    plugin = MockBukkit.createMockPlugin("EnchantableBlocks");
    registration = new EnchantableFurnaceRegistration(plugin);

    WorldMock world = server.addSimpleWorld("world");

    Block block = world.getBlockAt(0, 0, 0);
    block.setType(Material.FURNACE);
    Furnace furnace = new FurnaceMock(block);

    block = block.getRelative(BlockFace.UP);
    block.setType(Material.BLAST_FURNACE);
    Furnace blastFurnace = new BlastFurnaceMock(block);

    block = block.getRelative(BlockFace.UP);
    block.setType(Material.SMOKER);
    Furnace smoker = new SmokerMock(block);

    furnaces = new Furnace[] { furnace, blastFurnace, smoker };

    // Add some sample recipes to ensure tests actually cover code
    server.addRecipe(new FurnaceRecipe(new NamespacedKey(plugin, "furnace1"), new ItemStack(Material.DIRT), Material.DIAMOND, 0f, 0));
    server.addRecipe(new FurnaceRecipe(new NamespacedKey(plugin, "furnace2"), new ItemStack(Material.OAK_LOG), Material.COAL, 0f, 0));
    server.addRecipe(new FurnaceRecipe(new NamespacedKey(plugin, "furnace3"), new ItemStack(Material.COAL_ORE), Material.COAL_BLOCK, 0f, 0));

    server.addRecipe(new BlastingRecipe(new NamespacedKey(plugin, "blast1"), new ItemStack(Material.DIRT), Material.DIAMOND, 0f, 0));
    server.addRecipe(new BlastingRecipe(new NamespacedKey(plugin, "blast2"), new ItemStack(Material.OAK_LOG), Material.COAL, 0f, 0));
    server.addRecipe(new BlastingRecipe(new NamespacedKey(plugin, "blast3"), new ItemStack(Material.COAL_ORE), Material.COAL_BLOCK, 0f, 0));

    server.addRecipe(new SmokingRecipe(new NamespacedKey(plugin, "smoke1"), new ItemStack(Material.DIRT), Material.DIAMOND, 0f, 0));
    server.addRecipe(new SmokingRecipe(new NamespacedKey(plugin, "smoke2"), new ItemStack(Material.OAK_LOG), Material.COAL, 0f, 0));
    server.addRecipe(new SmokingRecipe(new NamespacedKey(plugin, "smoke3"), new ItemStack(Material.COAL_ORE), Material.COAL_BLOCK, 0f, 0));

    server.addRecipe(new CampfireRecipe(new NamespacedKey(plugin, "smores1"), new ItemStack(Material.DIRT), Material.DIAMOND, 0f, 0));
    server.addRecipe(new CampfireRecipe(new NamespacedKey(plugin, "hotdog2"), new ItemStack(Material.OAK_LOG), Material.COAL, 0f, 0));
    server.addRecipe(new CampfireRecipe(new NamespacedKey(plugin, "beancan3"), new ItemStack(Material.COAL_ORE), Material.COAL_BLOCK, 0f, 0));
  }

  @AfterAll
  void afterAll() {
    MockBukkit.unmock();
  }

  @DisplayName("Registration must create EnchantableFurnace instances.")
  @Test
  void testNewBlock() {
    WorldMock world = MockBukkit.getMock().addSimpleWorld("world");
    Block block = world.getBlockAt(0, 0, 0);
    ItemStack itemStack = new ItemStack(Material.FURNACE);
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 5);
    block.setType(itemStack.getType());
    ConfigurationSection section = plugin.getConfig().createSection("new.block.section");

    EnchantableFurnace enchantableFurnace = registration.newBlock(block, itemStack, section);

    assertThat("New block must create EnchantableFurnace",
        enchantableFurnace, is(instanceOf(EnchantableFurnace.class)));
    assertThat("New block must create new instance",
        registration.newBlock(block, itemStack, section),
        is(both(instanceOf(EnchantableFurnace.class)).and(not(enchantableFurnace))));

  }

  @DisplayName("Registration creates EnchantableFurnaceConfig instances.")
  @Test
  void testLoadConfig() {
    EnchantableFurnaceConfig config = registration.getConfig();
    assertThat("Get config must return EnchantableFurnaceConfig",
        config, is(instanceOf(EnchantableFurnaceConfig.class)));
    ConfigurationSection section = plugin.getConfig().createSection("blocks.EnchantableFurnace");

    assertThat("Load must create new instance",
        registration.loadConfig(section),
        is(both(instanceOf(EnchantableFurnaceConfig.class)).and(not(config))));
  }

  @DisplayName("Registration provides enchantments.")
  @Test
  void testGetEnchants() {
    assertThat("Registration must provide enchants",
        registration.getEnchants(), is(not(anyOf(nullValue(), empty()))));
  }

  @DisplayName("Registration provides valid materials.")
  @Test
  void testGetMaterials() {
    assertThat("Registration must provide materials",
        registration.getMaterials(), is(not(anyOf(nullValue(), empty()))));
  }

  @DisplayName("Reload does not error.")
  @Test
  void testReload() {
    assertDoesNotThrow(() -> registration.reload());
  }

  @DisplayName("Recipe lookup functions.")
  @ParameterizedTest
  @MethodSource("getModernMaterials")
  void testGetFurnaceRecipe(ItemStack item) {
    // Try each furnace type
    for (Furnace furnace : furnaces) {
      FurnaceInventory inventory = furnace.getInventory();
      inventory.setSmelting(item);

      assertDoesNotThrow(() -> registration.getFurnaceRecipe(inventory));
      // Again to hit cache
      assertDoesNotThrow(() -> registration.getFurnaceRecipe(inventory));
    }
  }

  static Stream<ItemStack> getModernMaterials() {
    return Stream.of(Material.values())
        .filter(material -> !material.name().startsWith("LEGACY_"))
        .map(ItemStack::new)
        // Filter MockBukkit's missing meta implementations
        .filter(itemStack -> {
          try {
            itemStack.getItemMeta();
          } catch (UnsupportedOperationException e) {
            return false;
          }
          return true;
        });
  }

  @Test
  void testGetFurnaceRecipeNull() {
    // Try each furnace type
    for (Furnace furnace : furnaces) {
      FurnaceInventory inventory = furnace.getInventory();
      inventory.setSmelting(null);

      assertDoesNotThrow(() -> registration.getFurnaceRecipe(inventory));
    }
  }

}