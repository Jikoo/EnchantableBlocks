package com.github.jikoo.enchantableblocks.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import be.seeseemelk.mockbukkit.entity.ItemEntityMock;
import be.seeseemelk.mockbukkit.plugin.PluginManagerMock;
import be.seeseemelk.mockbukkit.scheduler.BukkitSchedulerMock;
import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Listen for world events.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorldListenerTest {

  private Plugin plugin;
  private PluginManagerMock pluginManager;
  private WorldMock worldMock;
  private Player player;
  private BukkitSchedulerMock scheduler;
  private EnchantableBlockManager manager;
  private Block block;
  private ItemStack itemStack;

  @BeforeAll
  void setUpAll() {
    ServerMock serverMock = MockBukkit.mock();
    worldMock = new WorldMock() {
      @Override
      public @NotNull Collection<Entity> getNearbyEntities(@NotNull BoundingBox boundingBox) {
        return getEntities().stream().filter(
            entity -> entity.isValid() && boundingBox.contains(entity.getLocation().toVector()))
            .collect(Collectors.toList());
      }

      @Override
      public @NotNull ItemEntityMock dropItem(@NotNull Location loc, @NotNull ItemStack item, @Nullable Consumer<Item> function) {
        Preconditions.checkNotNull(loc, "The provided location must not be null.");
        Preconditions.checkNotNull(item, "Cannot drop items that are null.");
        Preconditions.checkArgument(!item.getType().isAir(), "Cannot drop air.");
        ItemEntityMock entity = new ItemEntityMock(serverMock, UUID.randomUUID(), item);
        entity.setLocation(loc);
        if (function != null) {
          function.accept(entity);
        }

        serverMock.registerEntity(entity);
        return entity;
      }
    };
    worldMock.setName("world");
    serverMock.addWorld(worldMock);

    // Set up a fake block that implements getDrops
    block = new BlockMock(Material.DIRT, new Location(worldMock, 0, 0, 0)) {
      @Override
      public @NotNull BoundingBox getBoundingBox() {
        return BoundingBox.of(this);
      }

      @Override
      public @NotNull Collection<ItemStack> getDrops() {
        return getDrops(null);
      }

      @Override
      public @NotNull Collection<ItemStack> getDrops(@Nullable ItemStack tool) {
        if (tool == null) {
          return Set.of();
        }

        return Set.of(new ItemStack(this.getType()));
      }
    };
  }

  @AfterAll
  void tearDownAll() {
    MockBukkit.unmock();
  }

  @BeforeEach
  void setUp() {
    var server = MockBukkit.getMock();
    PlayerInventory inventory = mock(PlayerInventory.class);
    when(inventory.getItemInMainHand()).thenReturn(new ItemStack(Material.DIAMOND_PICKAXE));
    player = mock(Player.class);
    when(player.getInventory()).thenReturn(inventory);
    scheduler = server.getScheduler();
    plugin = MockBukkit.createMockPlugin("EnchantableBlocks");
    manager = new EnchantableBlockManager(plugin);

    // Register dummy with manager
    var registration = new DummyEnchantableRegistration(
        plugin,
        Set.of(Enchantment.DIG_SPEED),
        EnumSet.of(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE)
    );
    manager.getRegistry().register(registration);

    pluginManager = server.getPluginManager();
    pluginManager.clearEvents();
    Listener listener = new WorldListener(plugin, manager);
    pluginManager.registerEvents(listener, plugin);

    // Reset block type
    block.setType(Material.DIRT);

    // Create default item
    itemStack = new ItemStack(Material.COAL_ORE);
  }

  @AfterEach
  void tearDown() {
    worldMock.getEntities().forEach(Entity::remove);
    plugin.getServer().getScheduler().cancelTasks(plugin);
    pluginManager.clearPlugins();
  }

  @DisplayName("Chunk loading loads blocks from storage.")
  @Test
  void testChunkLoad() {
    var event = new ChunkLoadEvent(block.getChunk(), false);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertDoesNotThrow(() -> scheduler.performTicks(2L));
  }

  @DisplayName("Chunk unloads unload blocks from storage.")
  @Test
  void testChunkUnload() {
    var event = new ChunkUnloadEvent(block.getChunk(), false);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
  }

  @DisplayName("Placing invalid blocks does nothing.")
  @Test
  void testInvalidBlockPlace() {
    BlockState replacedState = block.getState();
    block.setType(itemStack.getType());
    var event = new BlockPlaceEvent(block, replacedState, block.getRelative(BlockFace.NORTH),
        itemStack, player, true, EquipmentSlot.HAND);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
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
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertThat("Block must be created", manager.getBlock(block), is(notNullValue()));
  }

  @DisplayName("Breaking invalid blocks does nothing.")
  @Test
  void testInvalidBlockBreak() {
    assertThat("Block must be null", manager.getBlock(block), is(nullValue()));
    var event = new BlockBreakEvent(block, player);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));

  }

  @DisplayName("Breaking bad blocks does nothing.")
  @Test
  void testBadBlockBreak() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    enchantableBlock.getItemStack().setType(Material.AIR);
    var event = new BlockBreakEvent(block, player);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    pluginManager.assertEventNotFired(BlockDropItemEvent.class, "Block must not be in pending drops");
  }

  @DisplayName("Breaking blocks without drops does not drop items.")
  @Test
  void testNoDropBlockBreak() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    event.setDropItems(false);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    pluginManager.assertEventNotFired(BlockDropItemEvent.class, "Block must not be in pending drops");
  }

  @DisplayName("Breaking blocks in creative does not drop items.")
  @Test
  void testCreativeBlockBreak() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    player.setGameMode(GameMode.CREATIVE);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    pluginManager.assertEventNotFired(BlockDropItemEvent.class, "Block must not be in pending drops");
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
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    pluginManager.assertEventNotFired(BlockDropItemEvent.class, "Block must not be in pending drops");
  }

  @DisplayName("Breaking valid blocks drops items.")
  @Test
  void testValidBlockBreak() {
    block.setType(itemStack.getType());
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertDoesNotThrow(scheduler::performOneTick);
    var nearbyEntities = block.getWorld().getNearbyEntities(block.getBoundingBox());
    assertThat("Item must be added to world", nearbyEntities.size(), is(greaterThan(0)));
    pluginManager.assertEventFired("Event must be fired for item", BlockDropItemEvent.class, ignored -> true);
  }

  @DisplayName("Items removed from BlockDropEvent must not be in world")
  @Test
  void testRemoveBlockDropItem() {
    pluginManager.registerEvents(new Listener() {
      @EventHandler
      public void onBlockDropItem(BlockDropItemEvent event) {
        event.getItems().clear();
      }
    }, plugin);

    block.setType(itemStack.getType());
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    assertDoesNotThrow(() -> pluginManager.callEvent(event));
    assertDoesNotThrow(scheduler::performOneTick);
    var nearbyEntities = block.getWorld().getNearbyEntities(block.getBoundingBox());
    assertThat("Item must not be added to world", nearbyEntities.size(), is(0));
    pluginManager.assertEventFired("Event must be fired for item", BlockDropItemEvent.class, ignored -> true);
  }

}