package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.mock.inventory.InventoryMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Feature: Enchant and combine blocks in anvils.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnvilEnchanterTest {

  private MockedStatic<Bukkit> bukkit;
  private Enchantment enchantment;
  private Material goodType;
  private Material badType;

  @BeforeAll
  void setUp() {
    bukkit = mockStatic();

    ItemFactory itemFactory = ItemFactoryMocks.mockFactory();
    bukkit.when(Bukkit::getItemFactory).thenReturn(itemFactory);

    bukkit.when(() -> Bukkit.getRegistry(any())).thenAnswer(invocation -> {
      Registry<?> registry = mock(Registry.class);
      if (Enchantment.class.isAssignableFrom(invocation.getArgument(0))) {
        doAnswer(invocation1 -> mock(Enchantment.class)).when(registry).getOrThrow(any());
      }
      return registry;
    });

    enchantment = Enchantment.EFFICIENCY;
    doReturn(5).when(enchantment).getMaxLevel();

    goodType = Material.COAL_ORE;
    badType = Material.REDSTONE_ORE;
  }

  @AfterAll
  void tearDown() {
    bukkit.close();
  }

  private AnvilEnchanter enchanter;

  @Nested
  class ItemsInvalidTest {

    @BeforeEach
    void beforeEach() {
      var plugin = mock(Plugin.class);
      var registry = mock(EnchantableBlockRegistry.class);
      enchanter = new AnvilEnchanter(plugin, registry);
    }

    @DisplayName("Items are invalid if base is empty.")
    @Test
    void testNullBaseInvalid() {
      assertThat("Items are invalid", enchanter.areItemsInvalid(null, null));
    }

    @DisplayName("Items are invalid if addition is empty.")
    @Test
    void testNullAdditionInvalid() {
      var base = new ItemStack(goodType);
      assertThat("Items are invalid", enchanter.areItemsInvalid(base, null));
    }

    @DisplayName("Items are invalid if base is stacked.")
    @Test
    void testStackedBaseInvalid() {
      var base = new ItemStack(goodType);
      base.setAmount(64);
      var addition = new ItemStack(goodType);
      assertThat("Items are invalid", enchanter.areItemsInvalid(base, addition));
    }

    @DisplayName("Items are invalid base and addition do not match.")
    @Test
    void testDifferentAddition() {
      var base = new ItemStack(goodType);
      var addition = new ItemStack(badType);
      assertThat("Items are valid", enchanter.areItemsInvalid(base, addition));
    }

    @DisplayName("Items are valid if base and addition match.")
    @Test
    void testSame() {
      var base = new ItemStack(goodType);
      var addition = new ItemStack(goodType);
      assertThat("Items are valid", enchanter.areItemsInvalid(base, addition), is(false));
      addition.setAmount(64);
      assertThat("Items are valid", enchanter.areItemsInvalid(base, addition), is(false));
    }

    @DisplayName("Items are valid if addition is enchanted book.")
    @Test
    void testEnchantedBookAddition() {
      var base = new ItemStack(goodType);
      var addition = new ItemStack(Material.ENCHANTED_BOOK);
      assertThat("Items are valid", enchanter.areItemsInvalid(base, addition), is(false));
    }

  }

  @Nested
  class PrepareAnvilTest {

    private EnchantableBlockRegistry registry;
    private EnchantableRegistration registration;
    private ItemStack itemStack;
    private ArgumentCaptor<Runnable> runnableCaptor;
    private AnvilView view;

    @BeforeEach
    void beforeEach() {
      Server server = mock();

      var scheduler = mock(BukkitScheduler.class);
      runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
      when(scheduler.runTask(any(Plugin.class), runnableCaptor.capture())).thenReturn(null);
      when(server.getScheduler()).thenReturn(scheduler);

      var plugin = mock(Plugin.class);
      when(plugin.getServer()).thenReturn(server);

      itemStack = new ItemStack(goodType);

      registry = mock(EnchantableBlockRegistry.class);
      registration = mock(EnchantableRegistration.class);
      Material goodMat = itemStack.getType();
      doAnswer(invocation -> registration).when(registry).get(goodMat);
      doReturn(Set.of(enchantment)).when(registration).getEnchants();
      doReturn(true).when(registration).hasEnchantPermission(notNull(), anyString());
      // Not worth the hassle of mocking.
      var config = new EnchantableBlockConfig(new YamlConfiguration()) {};
      doReturn(config).when(registration).getConfig();

      enchanter = new AnvilEnchanter(plugin, registry);

      view = prepareView();
    }

    private @NotNull AnvilView prepareView() {
      Player player = mock();
      World world = mock();
      doReturn("world").when(world).getName();
      doReturn(world).when(player).getWorld();

      var inventory = InventoryMocks.newAnvilMock();
      inventory.setItem(0, itemStack.clone());
      var additionItem = new ItemStack(Material.ENCHANTED_BOOK);
      var additionMeta = additionItem.getItemMeta();
      if (additionMeta instanceof EnchantmentStorageMeta storageMeta) {
        storageMeta.addStoredEnchant(enchantment, enchantment.getMaxLevel(), true);
      }
      additionItem.setItemMeta(additionMeta);
      inventory.setItem(1, additionItem);

      AnvilView anvilView = mock(AnvilView.class);
      when(anvilView.getTopInventory()).thenReturn(inventory);
      when(anvilView.getPlayer()).thenReturn(player);
      doAnswer(invocation -> inventory.getItem(invocation.getArgument(0))).when(anvilView).getItem(anyInt());
      doAnswer(invocation -> {
        inventory.setItem(invocation.getArgument(0), invocation.getArgument(1));
        return null;
      }).when(anvilView).setItem(anyInt(), any());

      when(player.getOpenInventory()).thenReturn(anvilView);

      return anvilView;
    }

    @Test
    void testInvalidItem() {
      view.getTopInventory().setItem(0, null);
      var event = spy(new PrepareAnvilEvent(view, null));
      assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
      verify(registry, times(0)).get(any());
      verify(event, times(0)).setResult(any());
    }

    @Test
    void testUnregisteredMaterial() {
      ItemStack badStack = new ItemStack(badType);
      view.getTopInventory().setItem(0, badStack);
      var event = spy(new PrepareAnvilEvent(view, null));
      assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
      verify(registry).get(badStack.getType());
      verify(event, times(0)).setResult(any());
    }

    @Test
    void testNoPermission() {
      doReturn(false).when(registration).hasEnchantPermission(notNull(), anyString());
      var event = spy(new PrepareAnvilEvent(view, null));
      assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
      verify(registration).hasEnchantPermission(notNull(), anyString());
      verify(event, times(0)).setResult(any());
    }

    @Test
    void testNoChange() {
      view.getTopInventory().setItem(1, itemStack.clone());
      var event = spy(new PrepareAnvilEvent(view, null));

      assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
      verify(event, times(0)).setResult(any());
    }

    @ParameterizedTest
    @MethodSource("getSlots")
    void testChangePostCalculate(int... slots) {
      var event = spy(new PrepareAnvilEvent(view, null));

      assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
      verify(event).setResult(notNull());
      assertThat("Result is set", event.getResult(), is(notNullValue()));

      var inventory = view.getTopInventory();
      for (int slot : slots) {
        inventory.setItem(slot, null);
      }
      Runnable task = runnableCaptor.getValue();

      assertDoesNotThrow(task::run);
      verify(inventory, times(0)).setItem(eq(2), any());
      assertThat("Inventory result is unset", inventory.getItem(2), is(nullValue()));
    }

    static Collection<Arguments> getSlots() {
      return List.of(
          Arguments.of((Object) new int[] { 0 }),
          Arguments.of((Object) new int[] { 1 }),
          Arguments.of((Object) new int[] { 0, 1 })
      );
    }

    @Test
    void testSuccess() {
      var event = spy(new PrepareAnvilEvent(view, null));

      assertDoesNotThrow(() -> enchanter.onPrepareAnvil(event));
      verify(event).setResult(notNull());
      assertThat("Result is set", event.getResult(), is(notNullValue()));

      var result = event.getResult();

      Runnable task = runnableCaptor.getValue();

      assertDoesNotThrow(task::run);
      var inventory = view.getTopInventory();
      verify(inventory).setItem(
          eq(2),
          argThat(item -> result.isSimilar(item) && result.getAmount() == item.getAmount())
      );
    }

  }

}