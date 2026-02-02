package com.github.jikoo.enchantableblocks.mock;

import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.UnsafeValues;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// These suppressions are for internals we have to mock to get a usable server for testing.
@SuppressWarnings({"deprecation", "UnstableApiUsage"})
public final class ServerMocks {

  public static @NotNull Server mockServer() {
    Server mock = mock(Server.class);

    doReturn(ServerMocks.class.getName()).when(mock).getName();
    doReturn("1.2.3").when(mock).getVersion();
    doReturn("1.2.3-SAMPLETEXT").when(mock).getBukkitVersion();

    Logger noOp = mock(Logger.class);
    when(mock.getLogger()).thenReturn(noOp);
    when(mock.isPrimaryThread()).thenReturn(true);

    ItemFactory itemFactory = ItemFactoryMocks.mockFactory();
    when(mock.getItemFactory()).thenReturn(itemFactory);
    doAnswer(invocation -> {
       UnsafeValues unsafe = mock();

      ItemStack empty = mock();
      doReturn(Material.AIR).when(empty).getType();
      when(unsafe.createEmptyStack()).thenReturn(empty);

      return unsafe;
    }).when(mock).getUnsafe();

    // Server must be available before tags can be mocked.
    Bukkit.setServer(mock);

    // Tags are dependent on registries, but use a different method.
    // This will set up blank tags for each constant; all that needs to be done to render them
    // functional is to re-mock Tag#getValues.
    doAnswer(invocationGetTag -> {
      Tag<?> tag = mock();
      doReturn(invocationGetTag.getArgument(1)).when(tag).getKey();
      doReturn(Set.of()).when(tag).getValues();
      doAnswer(invocationIsTagged -> {
        Keyed keyed = invocationIsTagged.getArgument(0);
        Class<?> type = invocationGetTag.getArgument(2);
        if (!type.isAssignableFrom(keyed.getClass())) {
          return null;
        }
        // Since these are mocks, the exact instance might not be equal. Consider equal keys equal.
        return tag.getValues().contains(keyed) || tag.getValues().stream().anyMatch(value -> value.getKey().equals(keyed.getKey()));
      }).when(tag).isTagged(notNull());
      return tag;
    }).when(mock).getTag(notNull(), notNull(), notNull());

    // Once the server is all set up, touch BlockType and ItemType to initialize.
    // This prevents issues when trying to access dependent methods from a Material constant.
    try {
      Class.forName("org.bukkit.inventory.ItemType");
      Class.forName("org.bukkit.block.BlockType");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    return mock;
  }

  public static void unsetBukkitServer() {
    try
    {
      Field server = Bukkit.class.getDeclaredField("server");
      server.setAccessible(true);
      server.set(null, null);
    }
    catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }
  }

  private ServerMocks() {}

}
