package com.github.jikoo.enchantableblocks.mock.matcher;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ItemMatcher {

  /**
   * Construct a new {@code ItemEqualMatcher} for the given {@link ItemStack}.
   *
   * @param other the matchable item
   * @return the resulting matcher
   */
  public static BaseMatcher<ItemStack> isItem(@NotNull ItemStack other) {
    return new ItemEqualMatcher(other);
  }

  private static class ItemEqualMatcher extends BaseMatcher<ItemStack> {

    private final @NotNull ItemStack other;

    public ItemEqualMatcher(@NotNull ItemStack other) {
      this.other = other;
    }

    @Override
    public boolean matches(Object actual) {
      // Cannot use .equals because the backing ItemStack impl is a mock.
      // Instead, leverage our isSimilar implementation and compare the one remaining value.
      return actual instanceof ItemStack actualItem
          && other.isSimilar(actualItem)
          && other.getAmount() == actualItem.getAmount();
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("item ").appendValue(other.toString());
    }
  }

  /**
   * Construct a new {@code IsSimilarMatcher} for the given {@link ItemStack}.
   *
   * @param other the matchable item
   * @return the resulting matcher
   */
  public static BaseMatcher<ItemStack> isSimilar(@NotNull ItemStack other) {
    return new IsSimilarMatcher(other);
  }

  private static class IsSimilarMatcher extends BaseMatcher<ItemStack> {

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

  }

  /**
   * Construct a new {@code MetaIsEqualMatcher} for the given {@link ItemMeta}.
   *
   * @param other the matchable meta
   * @return the resulting matcher
   */
  public static BaseMatcher<ItemMeta> isMetaEqual(@NotNull ItemMeta other) {
    return new MetaIsEqualMatcher(other);
  }

  private static class MetaIsEqualMatcher extends BaseMatcher<ItemMeta> {

    private final @NotNull ItemMeta other;

    private MetaIsEqualMatcher(@NotNull ItemMeta other) {
      this.other = other;
    }

    @Override
    public boolean matches(@Nullable Object actual) {
      return actual instanceof ItemMeta actualMeta && Bukkit.getItemFactory().equals(other, actualMeta);
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(other.toString());
    }

  }

}
