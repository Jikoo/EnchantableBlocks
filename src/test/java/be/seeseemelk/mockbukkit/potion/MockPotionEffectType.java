package be.seeseemelk.mockbukkit.potion;

import java.util.Locale;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class MockPotionEffectType extends PotionEffectType {

  private final int id;
  private final @NotNull String name;
  private final boolean instant;
  private final @NotNull Color color;

  public MockPotionEffectType(int id, @NotNull String name, boolean instant, @NotNull Color color) {
    super(id, NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT)));
    this.id = id;
    this.name = name;
    this.instant = instant;
    this.color = color;
  }

  /**
   * @deprecated
   */
  @Deprecated
  public double getDurationModifier() {
    return 1.0D;
  }

  public @NotNull String getName() {
    return this.name;
  }

  public boolean isInstant() {
    return this.instant;
  }

  public @NotNull Color getColor() {
    return this.color;
  }

  public boolean equals(Object obj) {
    if (obj instanceof PotionEffectType) {
      return this.id == ((PotionEffectType) obj).getId();
    } else {
      return false;
    }
  }

  public int hashCode() {
    return this.id;
  }
}
