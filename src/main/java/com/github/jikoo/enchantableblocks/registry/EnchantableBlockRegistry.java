package com.github.jikoo.enchantableblocks.registry;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * A registry for {@link EnchantableBlock} implementations.
 */
public final class EnchantableBlockRegistry {

  private final @NotNull Plugin plugin;
  private final @NotNull Map<Material, EnchantableRegistration> materialRegistry;

  /**
   * Create a new {@code EnchantableBlockRegistry} for the given {@link Plugin}.
   *
   * @param plugin the {@code Plugin}
   */
  @VisibleForTesting
  public EnchantableBlockRegistry(@NotNull Plugin plugin) {
    this.plugin = plugin;
    materialRegistry = new HashMap<>();
  }

  /**
   * Register an {@link EnchantableBlock} implementation.
   *
   * <p>If an implementation is registered for the same {@link Material} as another registration,
   * the later registration will take precedence and the override will be logged.
   *
   * @param registration the {@link EnchantableRegistration} for the block
   */
  public void register(@NotNull EnchantableRegistration registration) {
    registration.getMaterials()
        .forEach(material -> {
          var replaced = this.materialRegistry.put(material, registration);
          if (replaced != null) {
            plugin.getLogger().info(() ->
                String.format(
                    "%s overrode %s for type %s",
                    registration.getBlockClass().getSimpleName(),
                    replaced.getBlockClass().getSimpleName(),
                    material.getKey()));
          }
        });
  }

  /**
   * Get an {@link EnchantableRegistration} by {@link Material}.
   *
   * @param material the {@code Material}
   * @return the {@code EnchantableRegistration} or {@code null} if none is registered
   */
  public @Nullable EnchantableRegistration get(@NotNull Material material) {
    return materialRegistry.get(material);
  }

  /**
   * Reload all registered {@link EnchantableRegistration EnchantableRegistrations}.
   */
  public void reload() {
    materialRegistry.values().stream().distinct().forEach(EnchantableRegistration::reload);
  }

}
