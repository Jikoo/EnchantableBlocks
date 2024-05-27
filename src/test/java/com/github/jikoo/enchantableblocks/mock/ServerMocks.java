package com.github.jikoo.enchantableblocks.mock;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.mockito.Answers;

public final class ServerMocks {

  public static @NotNull Server mockServer() {
    Server mock = mock(Server.class);

    Logger noOp = mock(Logger.class);
    when(mock.getLogger()).thenReturn(noOp);

    // Server must be available before tags can be mocked.
    Bukkit.setServer(mock);

    // Bukkit has a lot of static constants referencing registry values. To initialize those, the
    // registries must be able to be fetched before the classes are touched.
    // The mock registry can later be more specifically modified as necessary.
    doAnswer(invocationGetRegistry -> {
      // This must be mocked here or else Registry will be initialized when mocking it.
      Registry<?> registry = mock();
      doAnswer(invocationGetEntry -> {
        NamespacedKey key = invocationGetEntry.getArgument(0);
        // Set registries to always return a new value so that any constants are initialized.
        Class<? extends Keyed> arg = invocationGetRegistry.getArgument(0);
        // Deep stubs aren't great, but Bukkit has a lot of nullity checks on new constants.
        Keyed keyed = mock(arg, withSettings().defaultAnswer(Answers.RETURNS_DEEP_STUBS));
        doReturn(key).when(keyed).getKey();
        // It may eventually be necessary to stub BlockType#typed() here, but deep stubs work for now.
        return keyed;
      }).when(registry).get(notNull());
      return registry;
    }).when(mock).getRegistry(notNull());

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
