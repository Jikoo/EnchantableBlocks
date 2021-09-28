package com.github.jikoo.enchantableblocks.util.enchant;

import be.seeseemelk.mockbukkit.enchantments.EnchantmentMock;
import java.util.function.IntUnaryOperator;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused") // Used in reflection tests.
class FakeNmsEnchant extends EnchantmentMock {

  private final int rarityWeight;
  private final IntUnaryOperator minQuality;
  private final IntUnaryOperator maxQuality;

  FakeNmsEnchant(@NotNull Enchantment enchantment, int rarityWeight,
      @NotNull IntUnaryOperator minQuality, @NotNull IntUnaryOperator maxQuality) {
    super(enchantment.getKey(), enchantment.getKey().getNamespace());
    this.rarityWeight = rarityWeight;
    this.minQuality = minQuality;
    this.maxQuality = maxQuality;
  }

  public Object getHandle() {
    return new Object() {

      public int a(int value) {
        return minQuality.applyAsInt(value);
      }

      public int b(int value) {
        return maxQuality.applyAsInt(value);
      }

      public Object d() {
        return new Object() {

          public int a() {
            return rarityWeight;
          }

        };
      }
    };
  }

}
