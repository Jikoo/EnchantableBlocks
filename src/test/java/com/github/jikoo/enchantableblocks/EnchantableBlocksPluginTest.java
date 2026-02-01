package com.github.jikoo.enchantableblocks;

import com.github.jikoo.enchantableblocks.mock.ServerMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@DisplayName("Feature: Plugin should load and enable features.")
class EnchantableBlocksPluginTest {

  private EnchantableBlocksPlugin plugin;

  @BeforeEach
  void beforeEach() throws FileNotFoundException, InvalidDescriptionException {
    var server = ServerMocks.mockServer();
    var pluginManager = mock(PluginManager.class);
    when(server.getPluginManager()).thenReturn(pluginManager);
    var scheduler = mock(BukkitScheduler.class);
    when(server.getScheduler()).thenReturn(scheduler);
    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);

    var description = new PluginDescriptionFile(new BufferedReader(new FileReader(
        Path.of(".", "src", "main", "resources", "plugin.yml").toFile())));
    var dataFolder = Path.of(".", "src", "test", "resources", description.getName()).toFile();
    plugin = mock(EnchantableBlocksPlugin.class, withSettings().defaultAnswer(InvocationOnMock::callRealMethod));

    plugin.init(
        mock(),
        server,
        description,
        dataFolder,
        mock(),
        EnchantableBlocksPlugin.class.getClassLoader()
    );
  }

  @AfterEach
  void afterEach() {
    ServerMocks.unsetBukkitServer();
  }

  @DisplayName("Plugin has no-arg constructor.")
  @Test
  void testNoArgConstructor() {
    assertThrows(IllegalStateException.class, EnchantableBlocksPlugin::new);
  }

  @DisplayName("Plugin registers events.")
  @Test
  void testEventRegistration() {
    plugin.onLoad();
    verify(plugin.getServer().getPluginManager(), times(0))
        .registerEvents(any(Listener.class), any(Plugin.class));
    plugin.onEnable();
    // 3 base listeners + 1 furnace listener
    verify(plugin.getServer().getPluginManager(), times(4))
        .registerEvents(any(Listener.class), any(Plugin.class));
  }

  @DisplayName("Plugin loads active blocks.")
  @Test
  void testPluginLoad() {
    // Set up loaded chunks
    var world = WorldMocks.newWorld("world");
    var server = plugin.getServer();
    when(server.getWorlds()).thenReturn(List.of(world));
    when(world.getLoadedChunks()).thenAnswer(invocation -> new Chunk[] { world.getChunkAt(0, 0) });

    // Fire scheduled tasks immediately.
    var scheduler = server.getScheduler();
    when(scheduler.runTask(any(Plugin.class), any(Runnable.class))).thenAnswer(invocation -> {
      invocation.getArgument(1, Runnable.class).run();
      return null;
    });

    plugin.onLoad();

    Logger logger = mock();
    doReturn(logger).when(plugin).getLogger();

    plugin.onEnable();

    verify(plugin.getLogger()).info(any(Supplier.class));
  }

  @DisplayName("Reload command functions as expected.")
  @Test
  void testCommandBase() {
    plugin.onLoad();
    plugin.onEnable();
    var command = mock(Command.class);
    var player = mock(Player.class);

    boolean success = plugin.onCommand(player, command, "aliasesarebad", new String[] { "reload" });

    assertThat("Reload execution must succeed", success);
    verify(player).sendMessage(ArgumentMatchers.endsWith("Reloaded config and registry cache."));
  }

  @DisplayName("No argument command displays version and tells server to show help")
  @Test
  void testCommandNoArgs() {
    plugin.onLoad();
    plugin.onEnable();
    var command = mock(Command.class);
    var player = mock(Player.class);

    boolean success = plugin.onCommand(player, command, "aliasesarebad", new String[0]);

    assertThat("Command with no args must not succeed", !success);
    verify(player).sendMessage(ArgumentMatchers.startsWith("EnchantableBlocks v"));
  }

  @DisplayName("Invalid argument command displays version and tells server to show help")
  @Test
  void testCommandInvalidArgs() {
    plugin.onLoad();
    plugin.onEnable();
    var command = mock(Command.class);
    var player = mock(Player.class);

    boolean success = plugin.onCommand(player, command, "aliasesarebad", new String[] { "notreload" });

    assertThat("Command with other args must not succeed", !success);
    verify(player).sendMessage(ArgumentMatchers.startsWith("EnchantableBlocks v"));
  }

}