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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.mock.BukkitServer;
import com.github.jikoo.enchantableblocks.mock.inventory.InventoryMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlastFurnace;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.Smoker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
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

    // Add some sample recipes to ensure tests actually cover code
    List<Recipe> recipes = new ArrayList<>();
    recipes.add(new ShapelessRecipe(new NamespacedKey(plugin, "amorphous1"), new ItemStack(Material.DIRT)).addIngredient(Material.COAL));

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
    var manager = mock(EnchantableBlockManager.class);
    registration = new EnchantableFurnaceRegistration(plugin, manager);

    var furnaceInventory = InventoryMocks.newFurnaceMock();
    var furnace = mock(Furnace.class);
    when(furnaceInventory.getHolder()).thenReturn(furnace);
    var blastFurnaceInventory = InventoryMocks.newFurnaceMock(InventoryType.BLAST_FURNACE);
    var blastFurnace = mock(BlastFurnace.class);
    when(blastFurnaceInventory.getHolder()).thenReturn(blastFurnace);
    var smokerInventory = InventoryMocks.newFurnaceMock(InventoryType.SMOKER);
    var smoker = mock(Smoker.class);
    when(smokerInventory.getHolder()).thenReturn(smoker);

    furnaces = new FurnaceInventory[] { furnaceInventory, blastFurnaceInventory, smokerInventory };
  }

  @DisplayName("Listeners are registered as required.")
  @Test
  void testRegisterEvents() {
    var server = Bukkit.getServer();
    // Reset PluginManager so we can verify event registrations properly.
    var pluginManager = mock(PluginManager.class);
    when(server.getPluginManager()).thenReturn(pluginManager);

    var manager = mock(EnchantableBlockManager.class);

    verify(pluginManager, times(0)).registerEvents(any(FurnaceListener.class), eq(plugin));
    var reg = new EnchantableFurnaceRegistration(plugin, manager);
    verify(pluginManager).registerEvents(any(FurnaceListener.class), eq(plugin));
    reg.reload();
    verify(pluginManager, times(2)).registerEvents(any(FurnaceListener.class), eq(plugin));
  }

  @DisplayName("Registration must create EnchantableFurnace instances.")
  @Test
  void testNewBlock() {
    var block = mock(Block.class);
    // EnchantableFurnace creation requires several ItemStack methods, easier to not stub.
    var itemStack = new ItemStack(Material.FURNACE);
    var section = mock(ConfigurationSection.class);

    EnchantableFurnace enchantableFurnace = registration.newBlock(block, itemStack, section);

    assertThat("New block must create EnchantableFurnace",
        enchantableFurnace, is(instanceOf(EnchantableFurnace.class)));
    assertThat("New block must create new instance",
        registration.newBlock(block, itemStack, section),
        is(both(instanceOf(EnchantableFurnace.class)).and(not(enchantableFurnace))));
  }

  @DisplayName("Registration creates EnchantableFurnaceConfig instances.")
  @Test
  void testConfig() {
    ConfigurationSection section = mock(ConfigurationSection.class);
    var loaded = registration.loadConfig(section);
    assertThat(
        "Load creates EnchantableFurnaceConfig",
        loaded,
        is(instanceOf(EnchantableFurnaceConfig.class)));
    assertThat(
        "Load does not cache value",
        registration.loadConfig(section),
        is(both(instanceOf(EnchantableFurnaceConfig.class)).and(not(loaded))));

    EnchantableFurnaceConfig config = registration.getConfig();
    assertThat(
        "Getter returns EnchantableFurnaceConfig",
        config,
        is(instanceOf(EnchantableFurnaceConfig.class)));
    assertThat("Getter caches value", registration.getConfig(), is(config));
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

  @DisplayName("Recipe lookup uses cache.")
  @ParameterizedTest
  @MethodSource("getModernItems")
  void testGetFurnaceRecipe(ItemStack item) {
    // Try each furnace type
    for (FurnaceInventory furnace : furnaces) {
      furnace.setSmelting(item);
      var spiedReg = spy(registration);

      assertDoesNotThrow(() -> spiedReg.getFurnaceRecipe(furnace));
      verify(spiedReg).locateRecipe(any(), any());
      // Again to hit cache
      assertDoesNotThrow(() -> spiedReg.getFurnaceRecipe(furnace));
      verify(spiedReg).locateRecipe(any(), any());
    }
  }

  static Stream<ItemStack> getModernItems() {
    return Stream.of(Material.values())
        .filter(material -> !material.name().startsWith("LEGACY_") && material != Material.AIR && material.isItem())
        .map(ItemStack::new);
  }

  @DisplayName("Recipe lookup ignores null tile.")
  @Test
  void testGetFurnaceRecipeNullTile() {
    var inventory = InventoryMocks.newFurnaceMock();
    inventory.setSmelting(new ItemStack(Material.DIAMOND));
    var spiedReg = spy(registration);

    CookingRecipe<?> recipe = spiedReg.getFurnaceRecipe(inventory);
    assertThat("Recipe must be null for null holder", recipe, is(nullValue()));
    verify(spiedReg, times(0)).locateRecipe(any(), any());
  }

  @DisplayName("Recipe lookup functions for null input.")
  @Test
  void testGetFurnaceRecipeNull() {
    // Try each furnace type
    for (FurnaceInventory furnace : furnaces) {
      furnace.setSmelting(null);

      var spiedReg = spy(registration);

      CookingRecipe<?> recipe = spiedReg.getFurnaceRecipe(furnace);
      assertThat("Recipe must be null for null holder", recipe, is(nullValue()));
      verify(spiedReg, times(0)).locateRecipe(any(), any());
    }
  }

  @DisplayName("Recipe lookup functions for air input.")
  @Test
  void testGetFurnaceRecipeAir() {
    // Try each furnace type
    for (FurnaceInventory furnace : furnaces) {
      furnace.setSmelting(new ItemStack(Material.AIR));

      var spiedReg = spy(registration);

      CookingRecipe<?> recipe = spiedReg.getFurnaceRecipe(furnace);
      assertThat("Recipe must be null for null holder", recipe, is(nullValue()));
      verify(spiedReg, times(0)).locateRecipe(any(), any());
    }
  }

}