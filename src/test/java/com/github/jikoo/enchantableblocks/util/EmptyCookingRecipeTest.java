package com.github.jikoo.enchantableblocks.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import com.github.jikoo.enchantableblocks.mock.BukkitServer;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;
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
    var server = BukkitServer.newServer();
    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);
    Bukkit.setServer(server);
  }

  @BeforeEach
  void beforeEach() {
    recipe = new EmptyCookingRecipe(NamespacedKey.minecraft("key"));
  }

  @Test
  @DisplayName("Empty recipe does not match any item.")
  void testAlwaysInvalid() {
    for (Material material : Material.values()) {
      ItemStack item = new ItemStack(material);
      assertThat("Empty recipe must not match anything", !recipe.getInputChoice().test(item));
    }
  }

  @DisplayName("Empty recipe choices are immutable and therefore always equal.")
  @Test
  void testEquality() {
    var choice = recipe.getInputChoice();
    assertThat("Choice is not equal to null value", null, not(choice));
    assertThat("Choice is not equal to objects of other classes", "cool beans", not(choice));
    var choice2 = new EmptyCookingRecipe(NamespacedKey.minecraft("other_key")).getInputChoice();
    assertThat("Choice is equal to other EmptyCookingRecipe choice", choice2, is(choice));
  }

  @DisplayName("Empty recipe is not modified by setters.")
  @ParameterizedTest
  @MethodSource("getMethods")
  void testMethods(
      @NotNull Function<EmptyCookingRecipe, Object> getter,
      @NotNull BiConsumer<EmptyCookingRecipe, Object> setter,
      Object setValue) {
    Object value = getter.apply(recipe);
    boolean useSetterReturn = false;
    Object setterReturn = null;
    if (setter instanceof BiFunction biFunction) {
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