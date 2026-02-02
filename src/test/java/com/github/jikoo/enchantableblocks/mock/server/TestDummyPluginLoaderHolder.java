package com.github.jikoo.enchantableblocks.mock.server;

import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class TestDummyPluginLoaderHolder implements PluginLoader {

  @Override
  public @NotNull Plugin loadPlugin(@NotNull File file) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public @NotNull PluginDescriptionFile getPluginDescription(@NotNull File file) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public @NonNull @NotNull Pattern[] getPluginFileFilters() {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public @NotNull Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(@NotNull Listener listener, @NotNull Plugin plugin) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void enablePlugin(@NotNull Plugin plugin) {
    throw new IllegalStateException("Not implemented");
  }

  @Override
  public void disablePlugin(@NotNull Plugin plugin) {
    throw new IllegalStateException("Not implemented");
  }

}
