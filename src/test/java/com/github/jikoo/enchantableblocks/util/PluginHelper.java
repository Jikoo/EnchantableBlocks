package com.github.jikoo.enchantableblocks.util;

import java.io.File;
import java.lang.reflect.Field;
import org.bukkit.plugin.java.JavaPlugin;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class PluginHelper {

    public static void setDataDir(JavaPlugin plugin) throws NoSuchFieldException, IllegalAccessException {
        Field field = JavaPlugin.class.getDeclaredField("dataFolder");
        field.setAccessible(true);
        // Looks gross, but works around rare path issues with separator char on various OSs in Java 8.
        File dataFolder = new File(new File(new File(new File(".", "src"), "test"), "resources"), plugin.getName());
        field.set(plugin, dataFolder);
        field = JavaPlugin.class.getDeclaredField("configFile");
        field.setAccessible(true);
        field.set(plugin, new File(dataFolder, "config.yml"));

        plugin.reloadConfig();
    }

    public static <T extends JavaPlugin> MockedStatic<JavaPlugin> fixInstance(T t) {
        MockedStatic<JavaPlugin> javaPluginMockedStatic = Mockito.mockStatic(JavaPlugin.class);
        fixInstance(t, javaPluginMockedStatic);
        return javaPluginMockedStatic;
    }

    public static <T extends JavaPlugin> void fixInstance(T t, MockedStatic<JavaPlugin> javaPluginMockedStatic) {
        javaPluginMockedStatic.when(() -> JavaPlugin.getPlugin(t.getClass())).thenReturn(t);
    }

    private PluginHelper() {}
}
