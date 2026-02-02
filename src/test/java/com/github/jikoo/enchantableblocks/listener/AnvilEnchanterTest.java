package com.github.jikoo.enchantableblocks.listener;

import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import com.github.jikoo.enchantableblocks.mock.ServerMocks;
import com.github.jikoo.enchantableblocks.mock.enchantments.EnchantmentMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.InventoryMocks;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockRegistry;
import com.github.jikoo.enchantableblocks.registry.EnchantableRegistration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Feature: Enchant and combine blocks in anvils.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnvilEnchanterTest {

  private Enchantment enchantment;
  private ItemType goodType;
  private ItemType badType;

  private AnvilEnchanter enchanter;

  @BeforeAll
  void setUpAll() {
    ServerMocks.mockServer();
    EnchantmentMocks.init();

    enchantment = Enchantment.EFFICIENCY;
    goodType = ItemType.COAL_ORE;
    badType = ItemType.REDSTONE_ORE;
  }

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
      var base = goodType.createItemStack();
      assertThat("Items are invalid", enchanter.areItemsInvalid(base, null));
    }

    @DisplayName("Items are invalid if base is stacked.")
    @Test
    void testStackedBaseInvalid() {
      var base = goodType.createItemStack();
      base.setAmount(64);
      var addition = goodType.createItemStack();
      assertThat("Items are invalid", enchanter.areItemsInvalid(base, addition));
    }

    @DisplayName("Items are invalid base and addition do not match.")
    @Test
    void testDifferentAddition() {
      var base = goodType.createItemStack();
      var addition = badType.createItemStack();
      assertThat("Items are valid", enchanter.areItemsInvalid(base, addition));
    }

    @DisplayName("Items are valid if base and addition match.")
    @Test
    void testSame() {
      var base = goodType.createItemStack();
      var addition = goodType.createItemStack();
      assertThat("Items are valid", enchanter.areItemsInvalid(base, addition), is(false));
      addition.setAmount(64);
      assertThat("Items are valid", enchanter.areItemsInvalid(base, addition), is(false));
    }

    @DisplayName("Items are valid if addition is enchanted book.")
    @Test
    void testEnchantedBookAddition() {
      var base = goodType.createItemStack();
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
      var server = Bukkit.getServer();

      var scheduler = mock(BukkitScheduler.class);
      runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
      when(scheduler.runTask(any(Plugin.class), runnableCaptor.capture())).thenReturn(null);
      when(server.getScheduler()).thenReturn(scheduler);

      var plugin = mock(Plugin.class);
      when(plugin.getServer()).thenReturn(server);

      itemStack = goodType.createItemStack();

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
      var player = mock(Player.class);
      var world = WorldMocks.newWorld("world");
      when(player.getWorld()).thenReturn(world);

      var inventory = InventoryMocks.newAnvilMock();
      inventory.setItem(0, itemStack.clone());
      var additionItem = ItemType.ENCHANTED_BOOK.createItemStack();
      var additionMeta = additionItem.getItemMeta();
      if (additionMeta instanceof EnchantmentStorageMeta storageMeta) {
        storageMeta.addStoredEnchant(enchantment, enchantment.getMaxLevel(), true);
      }
      additionItem.setItemMeta(additionMeta);
      inventory.setItem(1, additionItem);

      AnvilView view = mock(AnvilView.class);
      when(view.getTopInventory()).thenReturn(inventory);
      when(view.getPlayer()).thenReturn(player);
      doAnswer(invocation -> inventory.getItem(invocation.getArgument(0))).when(view).getItem(anyInt());
      doAnswer(invocation -> {
        inventory.setItem(invocation.getArgument(0), invocation.getArgument(1));
        return null;
      }).when(view).setItem(anyInt(), any());

      when(player.getOpenInventory()).thenReturn(view);

      return view;
    }

    @AfterEach
    void afterEach() {
      var server = Bukkit.getServer();
      when(server.getScheduler()).thenReturn(null);
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
      ItemStack badStack = badType.createItemStack();
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

      // Because we can't override .equals for mocks and we need to verify that items
      // are unchanged before setting the result, we instead want to ensure that "copy"
      // is actually the same object.
      // This does cause the original item to be manipulated as a side effect when producing the result.
      ItemStack base = view.getTopInventory().getItem(0);
      doReturn(base).when(base).clone();
      ItemStack addition = view.getTopInventory().getItem(1);
      doReturn(addition).when(addition).clone();

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