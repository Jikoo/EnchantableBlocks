package com.github.jikoo.enchantableblocks.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.block.BlockMock;
import be.seeseemelk.mockbukkit.scheduler.BukkitSchedulerMock;
import com.github.jikoo.enchantableblocks.block.impl.dummy.DummyEnchantableBlock.DummyEnchantableRegistration;
import com.github.jikoo.enchantableblocks.registry.EnchantableBlockManager;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
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
    var world = serverMock.addSimpleWorld("world");

    // Set up a fake block that implements getDrops
    block = new BlockMock(Material.DIRT, new Location(world, 0, 0, 0)) {
      @Override
      public @NotNull Collection<ItemStack> getDrops(@Nullable ItemStack tool) {
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

  @Test
  void testChunkLoad() {
    var event = new ChunkLoadEvent(block.getChunk(), false);
    assertDoesNotThrow(() -> listener.onChunkLoad(event));
    assertDoesNotThrow(() -> scheduler.performTicks(2L));
  }

  @Test
  void testChunkUnload() {
    var event = new ChunkUnloadEvent(block.getChunk(), false);
    assertDoesNotThrow(() -> listener.onChunkUnload(event));
  }

  @Test
  void testInvalidBlockPlace() {
    BlockState replacedState = block.getState();
    block.setType(itemStack.getType());
    var event = new BlockPlaceEvent(block, replacedState, block.getRelative(BlockFace.NORTH),
        itemStack, player, true, EquipmentSlot.HAND);
    assertDoesNotThrow(() -> listener.onBlockPlace(event));
    assertThat("Block must not be created", manager.getBlock(block), is(nullValue()));
  }

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

  @Test
  void testInvalidBlockBreak() {
    assertThat("Block must be null", manager.getBlock(block), is(nullValue()));
    var event = new BlockBreakEvent(block, player);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));

  }

  @Test
  void testBadBlockBreak() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    enchantableBlock.getItemStack().setType(Material.AIR);
    var event = new BlockBreakEvent(block, player);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));
  }

  @Test
  void testNoDropBlockBreak() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    event.setDropItems(false);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));
  }

  @Test
  void testCreativeBlockBreak() {
    itemStack.addUnsafeEnchantment(Enchantment.DIG_SPEED, 10);
    var enchantableBlock = manager.createBlock(block, itemStack);
    assertThat("Block must not be null", enchantableBlock, is(notNullValue()));
    var event = new BlockBreakEvent(block, player);
    player.setGameMode(GameMode.CREATIVE);
    assertDoesNotThrow(() -> listener.onBlockBreak(event));
  }

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

  @Test
  void onBlockDropItem() {
    // TODO
  }

}