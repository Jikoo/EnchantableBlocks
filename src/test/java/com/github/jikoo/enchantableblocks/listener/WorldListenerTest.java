package com.github.jikoo.enchantableblocks.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.github.jikoo.enchantableblocks.mock.BukkitServer;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import com.google.common.base.Preconditions;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Consumer;
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
    var world = WorldMocks.newWorld("world");

    List<Entity> entities = new ArrayList<>();
    when(world.getNearbyEntities(any(BoundingBox.class))).thenAnswer(invocation -> {
      BoundingBox box = invocation.getArgument(0);
      return entities.stream().filter(entity -> entity.isValid() && box.contains(entity.getLocation().toVector())).toList();
    });
    when(world.dropItem(any(Location.class), any(ItemStack.class)))
        .thenAnswer(invocation -> world.dropItem(invocation.getArgument(0), invocation.getArgument(1), null));
    when(world.dropItem(any(Location.class), any(ItemStack.class), any())).thenAnswer(invocation -> {
      Location location = invocation.getArgument(0);
      ItemStack item = invocation.getArgument(1);

      Preconditions.checkNotNull(location, "The provided location must not be null.");
      Preconditions.checkNotNull(item, "Cannot drop items that are null.");
      Preconditions.checkArgument(!item.getType().isAir(), "Cannot drop air.");

      Item entity = mock(Item.class);
      doAnswer(invocation1 -> entities.remove(entity)).when(entity).remove();
      when(entity.isValid()).thenAnswer(invocation1 -> entities.contains(entity));
      AtomicReference<ItemStack> stack = new AtomicReference<>(item.clone());
      when(entity.getItemStack()).thenAnswer(invocation1 -> stack.get());
      when(entity.getLocation()).thenReturn(location);
      doAnswer(invocation1 -> {
        Preconditions.checkArgument(!item.getType().isAir(), "Cannot set item entity to air.");
        stack.set(invocation1.getArgument(0));
        return null;
      }).when(entity).setItemStack(any(ItemStack.class));

      entities.add(entity);

      Consumer<Item> consumer = invocation.getArgument(2);
      if (consumer != null) {
        consumer.accept(entity);
      }
      return entity;
    });

    block = world.getBlockAt(0, 0, 0);
    block.setType(Material.DIRT);
    BoundingBox box = BoundingBox.of(block);
    when(block.getBoundingBox()).thenReturn(box);
    when(block.getDrops()).thenAnswer(invocation -> block.getDrops(null));
    when(block.getDrops(any())).thenAnswer(invocation -> {
      ItemStack tool = invocation.getArgument(0);
      return tool == null ? Set.of() : Set.of(new ItemStack(block.getType()));
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
    when(plugin.getName()).thenReturn(getClass().getSimpleName());
    File dataFolder = Path.of(".", "src", "test", "resources", plugin.getName()).toFile();
    when(plugin.getDataFolder()).thenReturn(dataFolder);
    when(plugin.getConfig()).thenReturn(new YamlConfiguration());
    when(plugin.getServer()).thenReturn(server);

    manager = spy(new EnchantableBlockManager(plugin));

    // Register dummy with manager
    var registration = new DummyEnchantableRegistration(
        plugin,
        Set.of(Enchantment.DIG_SPEED),
        Set.of(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE)
    );
    manager.getRegistry().register(registration);

    listener = new WorldListener(plugin, manager);

    // Reset block type
    block.setType(Material.DIRT);

    // Create default item
    itemStack = new ItemStack(Material.COAL_ORE);
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

  @DisplayName("Placing invalid blocks does nothing.")
  @Test
  void testInvalidBlockPlace() {
    BlockState replacedState = block.getState();
    block.setType(itemStack.getType());
    var event = new BlockPlaceEvent(block, replacedState, block.getRelative(BlockFace.NORTH),
        itemStack, player, true, EquipmentSlot.HAND);
    assertDoesNotThrow(() -> listener.onBlockPlace(event));
    assertThat("Block must not be created", manager.getBlock(block), is(nullValue()));
  }

  @DisplayName("Placing valid blocks creates enchanted blocks.")
  @Test
  void testValidBlockPlace() {
    BlockState replacedState = block.getState();
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    block.setType(itemStack.getType());
    var event = new BlockPlaceEvent(block, replacedState, block.getRelative(BlockFace.NORTH),
        itemStack, player, true, EquipmentSlot.HAND);
    assertDoesNotThrow(() -> listener.onBlockPlace(event));
    assertThat("Block must be created", manager.getBlock(block), is(notNullValue()));
  }

  @DisplayName("Breaking invalid blocks does nothing.")
  @Test
  void testInvalidBlockBreak() {
    assertThat("Block must be null", manager.getBlock(block), is(nullValue()));
    var event = new BlockBreakEvent(block, player);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(pluginManager, times(0)).callEvent(any(BlockDropItemEvent.class));
  }

  @DisplayName("Breaking bad blocks does nothing.")
  @Test
  void testBadBlockBreak() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    enchantableBlock.getItemStack().setType(Material.AIR);
    var event = new BlockBreakEvent(block, player);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(pluginManager, times(0)).callEvent(any(BlockDropItemEvent.class));
  }

  @DisplayName("Breaking blocks without drops does not drop items.")
  @Test
  void testNoDropBlockBreak() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    event.setDropItems(false);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(pluginManager, times(0)).callEvent(any(BlockDropItemEvent.class));
  }

  @DisplayName("Breaking blocks in creative does not drop items.")
  @Test
  void testCreativeBlockBreak() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    player.setGameMode(GameMode.CREATIVE);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(pluginManager, times(0)).callEvent(any(BlockDropItemEvent.class));
  }

  @DisplayName("Breaking blocks with invalid tool does not drop items.")
  @Test
  void testInvalidToolBlockBreak() {
    when(player.getInventory().getItemInMainHand()).thenReturn(null);
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    player.setGameMode(GameMode.CREATIVE);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    verify(pluginManager, times(0)).callEvent(any(BlockDropItemEvent.class));
  }

  @DisplayName("Breaking valid blocks drops items.")
  @Test
  void testValidBlockBreak() {
    block.setType(itemStack.getType());
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    Runnable task = runnableCaptor.getValue();
    assertDoesNotThrow(task::run);
    var nearbyEntities = block.getWorld().getNearbyEntities(block.getBoundingBox());
    assertThat("Item must be added to world", nearbyEntities.size(), is(greaterThan(0)));
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

    block.setType(itemStack.getType());
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    Runnable task = runnableCaptor.getValue();
    assertDoesNotThrow(task::run);
    var nearbyEntities = block.getWorld().getNearbyEntities(block.getBoundingBox());
    assertThat("Item must not be added to world", nearbyEntities.size(), is(0));
    verify(pluginManager).callEvent(any(BlockDropItemEvent.class));
  }

}