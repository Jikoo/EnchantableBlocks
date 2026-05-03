package com.github.jikoo.enchantableblocks;

import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import com.github.jikoo.enchantableblocks.mock.world.WorldMocks;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@DisplayName("Feature: Plugin should load and enable features.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnchantableBlocksPluginTest {

  private MockedStatic<Bukkit> bukkit;
  private EnchantableBlocksPlugin plugin;

  @BeforeAll
  void setUp() {
    bukkit = mockStatic();
    bukkit.when(() -> Bukkit.getRegistry(any())).thenAnswer(invocation -> {
      Registry<?> registry = mock(Registry.class);
      if (Enchantment.class.isAssignableFrom(invocation.getArgument(0))) {
        doAnswer(invocation1 -> mock(Enchantment.class)).when(registry).getOrThrow(any());
      }
      return registry;
    });
    var factory = ItemFactoryMocks.mockFactory();
    bukkit.when(Bukkit::getItemFactory).thenReturn(factory);
  }

  @BeforeEach
  void beforeEach() throws FileNotFoundException, InvalidDescriptionException, ReflectiveOperationException {
    Server server = mock();
    var pluginManager = mock(PluginManager.class);
    when(server.getPluginManager()).thenReturn(pluginManager);
    var scheduler = mock(BukkitScheduler.class);
    when(server.getScheduler()).thenReturn(scheduler);

    plugin = mock(EnchantableBlocksPlugin.class, withSettings().defaultAnswer(InvocationOnMock::callRealMethod));

    doReturn(server).when(plugin).getServer();

    Logger logger = mock();
    doReturn(logger).when(plugin).getLogger();

    var description = new PluginDescriptionFile(new BufferedReader(new FileReader(
        Path.of(".", "src", "main", "resources", "plugin.yml").toFile())));
    doReturn(description).when(plugin).getDescription();

    var dataFolder = Path.of(".", "src", "test", "resources", description.getName()).toFile();
    doReturn(dataFolder).when(plugin).getDataFolder();

    Field configFile = JavaPlugin.class.getDeclaredField("configFile");
    configFile.setAccessible(true);
    configFile.set(plugin, new File(dataFolder, "config.yml"));

    Field classLoader = JavaPlugin.class.getDeclaredField("classLoader");
    classLoader.setAccessible(true);
    classLoader.set(plugin, EnchantableBlocksPlugin.class.getClassLoader());
  }

  @AfterAll
  void tearDown() {
    bukkit.close();
  }

  @DisplayName("Plugin has no-arg constructor.")
  @Test
  void testNoArgConstructor() {
    assertThrows(IllegalStateException.class, EnchantableBlocksPlugin::new);
  }

  @DisplayName("Plugin registers events.")
  @Test
  void testEventRegistration() {
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

    plugin.onEnable();

    verify(plugin.getLogger()).info(any(Supplier.class));
  }

  @DisplayName("Reload command functions as expected.")
  @Test
  void testCommandBase() {
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
    plugin.onEnable();
    var command = mock(Command.class);
    var player = mock(Player.class);

    boolean success = plugin.onCommand(player, command, "aliasesarebad", new String[] { "notreload" });

    assertThat("Command with other args must not succeed", !success);
    verify(player).sendMessage(ArgumentMatchers.startsWith("EnchantableBlocks v"));
  }

}