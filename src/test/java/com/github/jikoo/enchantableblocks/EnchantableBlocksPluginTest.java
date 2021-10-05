package com.github.jikoo.enchantableblocks;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.github.jikoo.enchantableblocks.util.logging.PatternCountHandler;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.command.Command;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Feature: Plugin should load and supply commands.")
class EnchantableBlocksPluginTest {

  private EnchantableBlocksPlugin plugin;

  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private void loadPlugin() {
    // This is specifically a separate method and not a beforeEach to test onEnable.
    plugin = MockBukkit.load(EnchantableBlocksPlugin.class);
  }

  @DisplayName("Plugin has no-arg constructor.")
  @Test
  void testNoArgConstructor() {
    assertThrows(IllegalStateException.class, EnchantableBlocksPlugin::new);
  }

  @DisplayName("Plugin loads.")
  @Test
  void testPluginLoad() {
    assertDoesNotThrow(this::loadPlugin);

    // Load chunk
    WorldMock world = MockBukkit.getMock().addSimpleWorld("test");
    world.getChunkAt(0, 0);
    PatternCountHandler loadCount = new PatternCountHandler("Loaded all active blocks");
    plugin.getLogger().addHandler(loadCount);

    assertDoesNotThrow(() -> MockBukkit.getMock().getScheduler().performOneTick());

    assertThat("Load must be performed once", loadCount.getMatches(), is(1));
  }

  @DisplayName("Plugin commands exist and perform as expected.")
  @Test
  void testCommandBase() {
    loadPlugin();

    ServerMock server = MockBukkit.getMock();
    AtomicInteger count = new AtomicInteger();
    PlayerMock player = new PlayerMock(server, "sample_text") {
      @Override
      public void sendMessage(String message) {
        count.incrementAndGet();
      }
    };

    Command command = server.getCommandMap().getCommand("enchantableblocks");
    assertThat("Command must be registered", command, is(notNullValue()));
    int expectedCount = 0;

    boolean success = plugin.onCommand(player, command, "aliasesarebad", new String[0]);
    ++expectedCount;

    assertThat("Command with no args must not succeed", !success);
    assertThat("Sender must have recieved message", count.get(), is(expectedCount));

    success = plugin.onCommand(player, command, "aliasesarebad", new String[]{"reload"});
    ++expectedCount;

    assertThat("Reload execution must succeed", success);
    assertThat("Sender must have recieved message", count.get(), is(expectedCount));
  }

}