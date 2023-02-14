package com.github.jikoo.enchantableblocks.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.mock.BukkitServer;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;

@DisplayName("Feature: Listen for world events.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorldListenerTest {

  private ArgumentCaptor<Runnable> runnableCaptor;
  private PluginManager pluginManager;
  private Player player;
  private EnchantableBlockManager manager;
  private WorldListener listener;
  private Block block;
  private ItemStack itemStack;

  @BeforeAll
  void setUpAll() {
    var server = BukkitServer.newServer();
    Bukkit.setServer(server);
    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);
  }

  @BeforeEach
  void setUp() {
    var world = createWorld();

    block = world.getBlockAt(0, 0, 0);
    when(block.isPreferredTool(any())).thenAnswer(invocation -> {
        ItemStack tool = invocation.getArgument(0);
        return tool != null;
    });

    player = mock(Player.class);
    when(player.getWorld()).thenReturn(world);

    var inventory = mock(PlayerInventory.class);
    when(inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.DIAMOND_PICKAXE));
    when(player.getInventory()).thenReturn(inventory);

    var server = BukkitServer.newServer();
    var scheduler = mock(BukkitScheduler.class);
    runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    when(scheduler.runTask(any(Plugin.class), runnableCaptor.capture())).thenReturn(null);
    when(server.getScheduler()).thenReturn(scheduler);
    pluginManager = mock(PluginManager.class);
    when(server.getPluginManager()).thenReturn(pluginManager);

    Plugin plugin = mock(Plugin.class);
    when(plugin.getServer()).thenReturn(server);

    manager = mock(EnchantableBlockManager.class);

    listener = new WorldListener(plugin, manager);

    // Reset block type
    block.setType(Material.DIRT);

    // Create default item
    itemStack = new ItemStack(Material.COAL_ORE);
  }

  private @NotNull World createWorld() {
    var world = WorldMocks.newWorld("world");

    List<Entity> entities = new ArrayList<>();
    when(world.getEntities()).thenAnswer(invocation -> List.copyOf(entities));
    when(world.dropItem(any(Location.class), any(ItemStack.class))).thenAnswer(invocation -> {
      Location location = invocation.getArgument(0);
      ItemStack item = invocation.getArgument(1);

      Preconditions.checkNotNull(location, "The provided location must not be null.");
      Preconditions.checkNotNull(item, "Cannot drop items that are null.");
      Preconditions.checkArgument(!item.getType().isAir(), "Cannot drop air.");

      Item entity = mock(Item.class);
      doAnswer(invocation1 -> entities.remove(entity)).when(entity).remove();
      AtomicReference<ItemStack> stack = new AtomicReference<>(item.clone());
      when(entity.getItemStack()).thenAnswer(invocation1 -> stack.get());
      when(entity.getLocation()).thenReturn(location);
      doAnswer(invocation1 -> {
        Preconditions.checkArgument(!item.getType().isAir(), "Cannot set item entity to air.");
        stack.set(invocation1.getArgument(0));
        return null;
      }).when(entity).setItemStack(any(ItemStack.class));

      entities.add(entity);

      return entity;
    });

    return world;
  }

  @DisplayName("Chunk loading loads blocks from storage.")
  @Test
  void testChunkLoad() {
    var event = new ChunkLoadEvent(block.getChunk(), false);
    assertDoesNotThrow(() -> listener.onChunkLoad(event));
    verify(manager, times(0)).loadChunkBlocks(any());
    Runnable task = runnableCaptor.getValue();
    assertDoesNotThrow(task::run);
    verify(manager).loadChunkBlocks(any());
  }

  @DisplayName("Chunk unloads unload blocks from storage.")
  @Test
  void testChunkUnload() {
    var event = new ChunkUnloadEvent(block.getChunk(), false);
    assertDoesNotThrow(() -> listener.onChunkUnload(event));
    verify(manager).unloadChunkBlocks(any());
  }

  @DisplayName("Placing valid blocks creates enchanted blocks.")
  @Test
  void testBlockPlace() {
    var state = mock(BlockState.class);
    var against = mock(Block.class);
    var event = new BlockPlaceEvent(block, state, against, itemStack, player, true, EquipmentSlot.HAND);

    assertDoesNotThrow(() -> listener.onBlockPlace(event));
    assertThat("Block must not be created", manager.getBlock(block), is(nullValue()));
  }

  @DisplayName("Invalid blocks are not handled.")
  @Test
  void testInvalidBlockBreak() {
    var event = spy(new BlockBreakEvent(block, player));

    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(event, times(0)).setDropItems(false);
  }

  @DisplayName("Blocks with invalid drops are not handled.")
  @Test
  void testBadBlockBreak() {
    ItemStack stack = new ItemStack(Material.AIR);
    doReturn(stack).when(manager).destroyBlock(block);
    var event = spy(new BlockBreakEvent(block, player));

    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(event, times(0)).setDropItems(false);
  }

  @DisplayName("Events set to not drop items are not handled.")
  @Test
  void testNoDropBlockBreak() {
    ItemStack stack = new ItemStack(Material.COAL_ORE);
    doReturn(stack).when(manager).destroyBlock(block);
    var event = new BlockBreakEvent(block, player);
    event.setDropItems(false);
    var finalEvent = spy(event);

    assertDoesNotThrow(() -> listener.onBlockBreak(finalEvent));
    verify(finalEvent, times(0)).setDropItems(false);
  }

  @DisplayName("Creative mode breaks are ignored.")
  @Test
  void testCreativeBlockBreak() {
    ItemStack stack = new ItemStack(Material.COAL_ORE);
    doReturn(stack).when(manager).destroyBlock(block);
    doReturn(GameMode.CREATIVE).when(player).getGameMode();
    var event = spy(new BlockBreakEvent(block, player));

    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(event, times(0)).setDropItems(false);
  }

  @DisplayName("Breaking blocks with invalid tool does not drop items.")
  @Test
  void testInvalidToolBlockBreak() {
    when(player.getInventory().getItemInMainHand()).thenReturn(null);
    ItemStack stack = new ItemStack(Material.COAL_ORE);
    doReturn(stack).when(manager).destroyBlock(block);
    var event = spy(new BlockBreakEvent(block, player));

    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(event, times(0)).setDropItems(false);
  }

  @DisplayName("Breaking valid blocks drops items.")
  @Test
  void testValidBlockBreak() {
    ItemStack stack = new ItemStack(Material.COAL_ORE);
    doReturn(stack).when(manager).destroyBlock(block);
    var event = spy(new BlockBreakEvent(block, player));

    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(event).setDropItems(false);

    Runnable task = runnableCaptor.getValue();
    assertDoesNotThrow(task::run);
    var nearbyEntities = block.getWorld().getEntities();
    assertThat("Item must be added to world", nearbyEntities.size(), is(1));
    verify(pluginManager).callEvent(any(BlockDropItemEvent.class));
  }

  @DisplayName("Breaking tile entity drops contents.")
  @Test
  void testTileBlockBreak() {
    ItemStack stack = new ItemStack(Material.COAL_ORE);
    doReturn(stack).when(manager).destroyBlock(block);

    var tile = mock(Container.class);
    doReturn(tile).when(block).getState();
    var inventory = mock(Inventory.class);
    doReturn(inventory).when(tile).getInventory();
    var contents = new ItemStack[] {
        null,
        new ItemStack(Material.AIR),
        new ItemStack(Material.DIRT)
    };
    doReturn(contents).when(inventory).getContents();

    var event = spy(new BlockBreakEvent(block, player));

    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(event).setDropItems(false);

    Runnable task = runnableCaptor.getValue();
    assertDoesNotThrow(task::run);
    var entities = block.getWorld().getEntities();
    assertThat("Item must be added to world", entities.size(), is(2));
    verify(pluginManager).callEvent(any(BlockDropItemEvent.class));
  }

  @DisplayName("Items removed from BlockDropEvent must not be in world")
  @Test
  void testRemoveBlockDropItem() {
    doAnswer(invocation -> {
      BlockDropItemEvent event = invocation.getArgument(0);
      event.getItems().clear();
      return null;
    }).when(pluginManager).callEvent(any(BlockDropItemEvent.class));

    ItemStack stack = new ItemStack(Material.COAL_ORE);
    doReturn(stack).when(manager).destroyBlock(block);
    var event = spy(new BlockBreakEvent(block, player));

    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(event).setDropItems(false);

    Runnable task = runnableCaptor.getValue();
    assertDoesNotThrow(task::run);
    var entities = block.getWorld().getEntities();
    assertThat("Item must not be added to world", entities.size(), is(0));
    verify(pluginManager).callEvent(any(BlockDropItemEvent.class));
  }

}