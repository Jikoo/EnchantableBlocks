package com.github.jikoo.enchantableblocks.registry;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import java.util.EnumMap;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A registry for {@link EnchantableBlock} implementations.
 */
public final class EnchantableBlockRegistry {

  private final @NotNull Plugin plugin;
  private final @NotNull EnumMap<Material, EnchantableRegistration> materialRegistry;

  EnchantableBlockRegistry(@NotNull Plugin plugin) {
    this.plugin = plugin;
    materialRegistry = new EnumMap<>(Material.class);
  }

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

  public @Nullable EnchantableRegistration get(@NotNull Material material) {
    return materialRegistry.get(material);
  }

  void reload() {
    materialRegistry.values().stream().distinct().forEach(EnchantableRegistration::reload);
  }

}
