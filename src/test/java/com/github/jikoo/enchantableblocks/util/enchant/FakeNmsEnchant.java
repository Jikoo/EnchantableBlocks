package com.github.jikoo.enchantableblocks.util.enchant;

import be.seeseemelk.mockbukkit.enchantments.EnchantmentMock;
import java.util.function.IntUnaryOperator;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

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

      // NMSREF \nnet\.minecraft\.world\.item\.enchantment\.Enchantment(.|\n)*?int getMinCost\(int\)
      public int a(int value) {
        return minQuality.applyAsInt(value);
      }

      // NMSREF \nnet\.minecraft\.world\.item\.enchantment\.Enchantment(.|\n)*?int getMaxCost\(int\)
      public int b(int value) {
        return maxQuality.applyAsInt(value);
      }

      // NMSREF \nnet\.minecraft\.world\.item\.enchantment\.Enchantment(.|\n)*?net\.minecraft\.world\.item\.enchantment\.Enchantment\$Rarity getRarity\(\)
      public Object d() {
        // NMSREF \nnet\.minecraft\.world\.item\.enchantment\.Enchantment\$Rarity(.|\n)*?int getWeight\(\)
        return new Object() {

          public int a() {
            return rarityWeight;
          }

        };
      }
    };
  }

}
