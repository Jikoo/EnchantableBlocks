package com.github.jikoo.enchantableblocks.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.github.jikoo.planarwrappers.util.StringConverters;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Feature: Placeholder empty unmodifiable cooking recipe")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmptyCookingRecipeTest {

  private EmptyCookingRecipe recipe;

  @BeforeAll
  void beforeAll() {
    MockBukkit.mock();
  }

  @AfterAll
  void afterAll() {
    MockBukkit.unmock();
  }

  @BeforeEach
  void setUp() {
    recipe = new EmptyCookingRecipe(Objects.requireNonNull(StringConverters.toNamespacedKey("key")));
  }

  @Test
  @DisplayName("Empty recipe does not match any item.")
  void testAlwaysInvalid() {
    for (Material material : Material.values()) {
      ItemStack item = new ItemStack(material);
      assertThat("Empty recipe must not match anything", !recipe.getInputChoice().test(item));
    }
  }

  @DisplayName("Empty recipe is not modified by setters.")
  @ParameterizedTest
  @MethodSource("getMethods")
  void testMethods(
      Function<EmptyCookingRecipe, Object> getter,
      BiConsumer<EmptyCookingRecipe, Object> setter,
      Object setValue) {
    Object value = getter.apply(recipe);
    boolean useSetterReturn = false;
    Object setterReturn = null;
    if (setter instanceof BiFunction biFunction) {
      //noinspection unchecked
      setterReturn = biFunction.apply(recipe, setValue);
      useSetterReturn = true;
    } else {
      setter.accept(recipe, setValue);
    }
    assertThat("Value must not change", getter.apply(recipe), is(value));
    if (useSetterReturn) {
      assertThat("Setter must return self", setterReturn, is(recipe));
    }
  }

  static Stream<Arguments> getMethods() {
    return Stream.of(
        Arguments.of(
            (Function<EmptyCookingRecipe, ItemStack>) EmptyCookingRecipe::getInput,
            (BiConsumer<EmptyCookingRecipe, Material>) EmptyCookingRecipe::setInput,
            Material.COAL),
        Arguments.of(
            (Function<EmptyCookingRecipe, RecipeChoice>) EmptyCookingRecipe::getInputChoice,
            (BiConsumer<EmptyCookingRecipe, RecipeChoice>) EmptyCookingRecipe::setInputChoice,
            new RecipeChoice.MaterialChoice(Material.COAL)),
        Arguments.of(
            (Function<EmptyCookingRecipe, Integer>) EmptyCookingRecipe::getCookingTime,
            (BiConsumer<EmptyCookingRecipe, Integer>) EmptyCookingRecipe::setCookingTime,
            500),
        Arguments.of(
            (Function<EmptyCookingRecipe, Float>) EmptyCookingRecipe::getExperience,
            (BiConsumer<EmptyCookingRecipe, Float>) EmptyCookingRecipe::setExperience,
            500F)
    );
  }
}