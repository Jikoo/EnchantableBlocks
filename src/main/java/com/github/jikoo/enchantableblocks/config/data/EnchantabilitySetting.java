package com.github.jikoo.enchantableblocks.config.data;

import com.github.jikoo.planarenchanting.table.Enchantability;
import com.github.jikoo.planarwrappers.config.ParsedSimpleSetting;
import java.lang.reflect.Field;
import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * A setting for an {@code Enchantability}.
 */
public class EnchantabilitySetting extends ParsedSimpleSetting<Enchantability> {

  public EnchantabilitySetting(
      @NotNull ConfigurationSection section,
      @NotNull String path,
      @NotNull Enchantability defaultValue) {
    super(section, path, defaultValue);
  }

  @Override
  protected @Nullable Enchantability convertString(@Nullable String value) {
    if (value == null) {
      return null;
    }
    try {
      return new Enchantability(Math.max(1, Integer.parseInt(value)));
    } catch (NumberFormatException ignored) {
      // Not a number, may be a field name.
    }
    value = value.toUpperCase(Locale.ROOT);
    try {
      Field field = Enchantability.class.getDeclaredField(value);
      Object fieldValue = field.get(null);
      if (fieldValue instanceof Enchantability enchantability) {
        return enchantability;
      }
      return null;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      return null;
    }
  }

}
