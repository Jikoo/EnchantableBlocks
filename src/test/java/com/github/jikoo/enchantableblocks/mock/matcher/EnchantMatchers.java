package com.github.jikoo.enchantableblocks.mock.matcher;

import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EnchantMatchers {

  @Contract("_ -> new")
  public static @NotNull Matcher<Enchantment> enchant(@NotNull Enchantment expected) {
    return new EnchantMatcher(expected);
  }

  private static class EnchantMatcher extends BaseMatcher<Enchantment> {
    private final @NotNull Enchantment expected;

    private EnchantMatcher(@NotNull Enchantment expected) {
      this.expected = expected;
    }

    @Override
    public boolean matches(@Nullable Object actual) {
      return actual instanceof Enchantment enchantment && enchantment.getKey().equals(expected.getKey());
    }

    @Override
    public void describeTo(@NotNull Description description) {
      description.appendText(expected.toString());
    }
  }

  @Contract("_ -> new")
  public static @NotNull Matcher<Set<Enchantment>> enchantSet(@NotNull Set<Enchantment> expected) {
    return new EnchantSetMatcher(expected);
  }

  private static class EnchantSetMatcher extends BaseMatcher<Set<Enchantment>> {

    private final Set<NamespacedKey> keys;

    private EnchantSetMatcher(@NotNull Set<Enchantment> enchants) {
      keys = enchants.stream().map(Enchantment::getKey).collect(Collectors.toSet());
    }

    @Override
    public boolean matches(@Nullable Object actual) {
      return actual instanceof Set<?> set
          && set.size() == keys.size()
          && set.stream().allMatch(
              value -> value instanceof Enchantment enchantment
                  && keys.contains(enchantment.getKey()));
    }

    @Override
    public void describeTo(@NotNull Description description) {
      description.appendText("EnchantSetMatcher containing ");
      description.appendValueList("Set of enchantments [", ", ", "]", keys);
    }

  }


  private EnchantMatchers() {}

}
