package com.github.jikoo.enchantableblocks.block.impl.furnace;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.mock.BukkitServer;
import com.github.jikoo.enchantableblocks.mock.inventory.InventoryMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
  private FurnaceInventory[] furnaces;

  @BeforeAll
  void beforeAll() {
    var server = BukkitServer.newServer();
    Bukkit.setServer(server);

    var pluginManager = mock(PluginManager.class);
    when(server.getPluginManager()).thenReturn(pluginManager);

    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);

    plugin = mock(Plugin.class);
    when(plugin.getName()).thenReturn(getClass().getSimpleName());
    when(plugin.getConfig()).thenReturn(new YamlConfiguration());
    when(plugin.getServer()).thenReturn(server);
    var logger = mock(Logger.class);
    when(plugin.getLogger()).thenReturn(logger);

    furnaces = new FurnaceInventory[] {
        InventoryMocks.newFurnaceMock(),
        InventoryMocks.newFurnaceMock(InventoryType.BLAST_FURNACE),
        InventoryMocks.newFurnaceMock(InventoryType.SMOKER)
    };

    // Add some sample recipes to ensure tests actually cover code
    List<Recipe> recipes = new ArrayList<>();
    recipes.add(new FurnaceRecipe(new NamespacedKey(plugin, "furnace1"), new ItemStack(Material.DIRT), Material.DIAMOND, 0f, 0));
    recipes.add(new FurnaceRecipe(new NamespacedKey(plugin, "furnace2"), new ItemStack(Material.OAK_LOG), Material.COAL, 0f, 0));
    recipes.add(new FurnaceRecipe(new NamespacedKey(plugin, "furnace3"), new ItemStack(Material.COAL_ORE), Material.COAL_BLOCK, 0f, 0));

    recipes.add(new BlastingRecipe(new NamespacedKey(plugin, "blast1"), new ItemStack(Material.DIRT), Material.DIAMOND, 0f, 0));
    recipes.add(new BlastingRecipe(new NamespacedKey(plugin, "blast2"), new ItemStack(Material.OAK_LOG), Material.COAL, 0f, 0));
    recipes.add(new BlastingRecipe(new NamespacedKey(plugin, "blast3"), new ItemStack(Material.COAL_ORE), Material.COAL_BLOCK, 0f, 0));

    recipes.add(new SmokingRecipe(new NamespacedKey(plugin, "smoke1"), new ItemStack(Material.DIRT), Material.DIAMOND, 0f, 0));
    recipes.add(new SmokingRecipe(new NamespacedKey(plugin, "smoke2"), new ItemStack(Material.OAK_LOG), Material.COAL, 0f, 0));
    recipes.add(new SmokingRecipe(new NamespacedKey(plugin, "smoke3"), new ItemStack(Material.COAL_ORE), Material.COAL_BLOCK, 0f, 0));

    recipes.add(new CampfireRecipe(new NamespacedKey(plugin, "smores1"), new ItemStack(Material.DIRT), Material.DIAMOND, 0f, 0));
    recipes.add(new CampfireRecipe(new NamespacedKey(plugin, "hotdog2"), new ItemStack(Material.OAK_LOG), Material.COAL, 0f, 0));
    recipes.add(new CampfireRecipe(new NamespacedKey(plugin, "beancan3"), new ItemStack(Material.COAL_ORE), Material.COAL_BLOCK, 0f, 0));
    when(server.recipeIterator()).thenAnswer(invocation -> recipes.iterator());
  }

  @BeforeEach
  void beforeEach() {
    var manager = new EnchantableBlockManager(plugin);
    registration = new EnchantableFurnaceRegistration(plugin, manager);
  }

  @DisplayName("Registration must create EnchantableFurnace instances.")
  @Test
  void testNewBlock() {
    World world = WorldMocks.newWorld("world");
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
    for (FurnaceInventory furnace : furnaces) {
      furnace.setSmelting(item);

      assertDoesNotThrow(() -> registration.getFurnaceRecipe(furnace));
      // Again to hit cache
      // TODO use verify
      assertDoesNotThrow(() -> registration.getFurnaceRecipe(furnace));
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

  @DisplayName("Recipe lookup functions for null input.")
  @Test
  void testGetFurnaceRecipeNull() {
    // Try each furnace type
    for (FurnaceInventory furnace : furnaces) {
      furnace.setSmelting(null);

      assertDoesNotThrow(() -> registration.getFurnaceRecipe(furnace));
    }
  }

}