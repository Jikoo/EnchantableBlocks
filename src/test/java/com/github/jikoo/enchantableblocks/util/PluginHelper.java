package com.github.jikoo.enchantableblocks.util;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class PluginHelper {

  public static void setDataDir(@NotNull JavaPlugin plugin)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = JavaPlugin.class.getDeclaredField("dataFolder");
    field.setAccessible(true);
    File dataFolder = Path.of(".", "src", "test", "resources", plugin.getName()).toFile();
    field.set(plugin, dataFolder);
    field = JavaPlugin.class.getDeclaredField("configFile");
    field.setAccessible(true);
    field.set(plugin, new File(dataFolder, "config.yml"));

    plugin.reloadConfig();
  }

  private PluginHelper() {
  }
}
