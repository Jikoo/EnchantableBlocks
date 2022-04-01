package com.github.jikoo.enchantableblocks.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import be.seeseemelk.mockbukkit.entity.ItemEntityMock;
import be.seeseemelk.mockbukkit.scheduler.BukkitSchedulerMock;
import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.github.jikoo.enchantableblocks.listener.WorldListener.DropReplacement;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
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
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Feature: Listen for world events.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorldListenerTest {

  private Player player;
  private BukkitSchedulerMock scheduler;
  private EnchantableBlockManager manager;
  private WorldListener listener;
  private Block block;
  private ItemStack itemStack;

  @BeforeAll
  void setUpAll() {
    ServerMock serverMock = MockBukkit.mock();
    var world = new WorldMock() {
      @Override
      public @NotNull Collection<Entity> getNearbyEntities(@NotNull BoundingBox boundingBox) {
        return getEntities().stream().filter(
            entity -> boundingBox.contains(entity.getLocation().toVector()))
            .collect(Collectors.toList());
      }
    };
    world.setName("world");
    serverMock.addWorld(world);

    // Set up a fake block that implements getDrops
    block = new BlockMock(Material.DIRT, new Location(world, 0, 0, 0)) {
      @Override
      public @NotNull BoundingBox getBoundingBox() {
        return BoundingBox.of(this);
      }
      @Override
      public @NotNull Collection<ItemStack> getDrops(@Nullable ItemStack tool) {
        return Set.of(new ItemStack(this.getType()));
      }
    };
  }

  @AfterAll
  void tearDownAll() {
    ServerMock server = MockBukkit.getMock();
    for (Plugin plugin : server.getPluginManager().getPlugins()) {
      server.getScheduler().cancelTasks(plugin);
    }
    MockBukkit.unmock();
  }

  @BeforeEach
  void setUp() {
    var server = MockBukkit.getMock();
    player = server.addPlayer("sampletext");
    scheduler = server.getScheduler();
    var plugin = MockBukkit.createMockPlugin("EnchantableBlocks");
    manager = new EnchantableBlockManager(plugin);

    // Register dummy with manager
    var registration = new DummyEnchantableRegistration(
        plugin,
        Set.of(Enchantment.DIG_SPEED),
        EnumSet.of(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE)
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
    assertDoesNotThrow(() -> scheduler.performTicks(2L));
  }

  @DisplayName("Chunk unloads unload blocks from storage.")
  @Test
  void testChunkUnload() {
    var event = new ChunkUnloadEvent(block.getChunk(), false);
    assertDoesNotThrow(() -> listener.onChunkUnload(event));
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
    assertThat("Block must not be in pending drops", !listener.pendingDrops.containsKey(block));
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
    assertThat("Block must not be in pending drops", !listener.pendingDrops.containsKey(block));
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
    assertThat("Block must not be in pending drops", !listener.pendingDrops.containsKey(block));
  }

  @DisplayName("Breaking valid blocks prepares to drop items.")
  @Test
  void testValidBlockBreak() {
    block.setType(itemStack.getType());
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));
    assertThat("Block must be in pending drops", listener.pendingDrops.containsKey(block));
    assertDoesNotThrow(scheduler::performOneTick);
    assertThat("Block must not be in pending drops", !listener.pendingDrops.containsKey(block));
  }

  @DisplayName("Dropped items for invalid blocks does nothing.")
  @Test
  void testNoPendingBlockDropItem() {
    var postBreakType = block.getType();
    var stackType = itemStack.getType();
    block.setType(stackType);
    var preBreakState = block.getState();
    block.setType(postBreakType);
    var event = new BlockDropItemEvent(block, preBreakState, player, List.of());
    assertDoesNotThrow(() -> listener.onBlockDropItem(event));
  }

  @DisplayName("Invalid data for blocks does nothing.")
  @Test
  void testAirPendingBlockDropItem() {
    var postBreakType = block.getType();
    var stackType = itemStack.getType();
    block.setType(stackType);
    var preBreakState = block.getState();
    block.setType(postBreakType);
    var event = new BlockDropItemEvent(block, preBreakState, player, List.of());
    listener.pendingDrops.put(block, new DropReplacement(new ItemStack(Material.AIR), itemStack));
    assertDoesNotThrow(() -> listener.onBlockDropItem(event));
  }

  @DisplayName("Items for valid block must drop.")
  @Test
  void testPendingEmptyListBlockDropItem() {
    var postBreakType = block.getType();
    var stackType = itemStack.getType();
    block.setType(stackType);
    var preBreakState = block.getState();
    block.setType(postBreakType);
    var event = new BlockDropItemEvent(block, preBreakState, player, List.of());
    listener.pendingDrops.put(block, new DropReplacement(new ItemStack(stackType), itemStack));
    assertDoesNotThrow(() -> listener.onBlockDropItem(event));
    var nearbyEntities = block.getWorld().getNearbyEntities(block.getBoundingBox());
    assertThat("Item must be added to world", nearbyEntities.size(), is(greaterThan(0)));
  }

  @DisplayName("Items for valid block must replace existing drops.")
  @Test
  void testPendingBlockDropItem() {
    var postBreakType = block.getType();
    var stackType = itemStack.getType();
    block.setType(stackType);
    var preBreakState = block.getState();
    block.setType(postBreakType);
    var event = new BlockDropItemEvent(block, preBreakState, player,
        new ArrayList<>(List.of(
            new ItemEntityMock(MockBukkit.getMock(), UUID.randomUUID(), new ItemStack(Material.REDSTONE_ORE)),
            new ItemEntityMock(MockBukkit.getMock(), UUID.randomUUID(), new ItemStack(stackType)))));
    listener.pendingDrops.put(block, new DropReplacement(new ItemStack(stackType), itemStack));
    assertDoesNotThrow(() -> listener.onBlockDropItem(event));
    assertThat("Target drop must be removed", event.getItems().size(), is(1));
    var nearbyEntities = block.getWorld().getNearbyEntities(block.getBoundingBox());
    assertThat("Item must be added to world", nearbyEntities.size(), is(greaterThan(0)));
  }

}