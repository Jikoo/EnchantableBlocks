package com.github.jikoo.enchantableblocks.mock.matcher;

import org.bukkit.inventory.ItemStack;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link org.hamcrest.Matcher Matcher} implementation for comparing {@link ItemStack ItemStacks}
 * with {@link ItemStack#isSimilar(ItemStack)}.
 */
public class IsSimilarMatcher extends BaseMatcher<ItemStack> {

  private final @NotNull ItemStack other;

  private IsSimilarMatcher(@NotNull ItemStack other) {
    this.other = other;
  }

  @Override
  public boolean matches(@Nullable Object actual) {
    return actual instanceof ItemStack actualItem && other.isSimilar(actualItem);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText(other.toString());
  }

  /**
   * Construct a new {@code IsSimilarMatcher} for the given {@link ItemStack}.
   *
   * @param other the matchable item
   * @return the resulting matcher
   */
  public static @NotNull IsSimilarMatcher isSimilar(@NotNull ItemStack other) {
    return new IsSimilarMatcher(other);
  }

}
