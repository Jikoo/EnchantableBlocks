package com.github.jikoo.enchantableblocks.util;

import com.github.jikoo.enchantableblocks.mock.ServerMocks;
import com.github.jikoo.enchantableblocks.mock.inventory.ItemFactoryMocks;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("Feature: Placeholder empty unmodifiable cooking recipe")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmptyCookingRecipeTest {

  private EmptyCookingRecipe recipe;

  @BeforeAll
  void beforeAll() {
    var server = ServerMocks.mockServer();
    var factory = ItemFactoryMocks.mockFactory();
    when(server.getItemFactory()).thenReturn(factory);
  }

  @BeforeEach
  void beforeEach() {
    recipe = new EmptyCookingRecipe(NamespacedKey.minecraft("key"));
  }

  @ParameterizedTest
  @DisplayName("Empty recipe does not match any item.")
  @MethodSource("getModernItems")
  void testAlwaysInvalid(ItemStack item) {
    assertThat("Empty recipe must not match anything", !recipe.getInputChoice().test(item));
  }

  static Stream<ItemStack> getModernItems() {
    return Stream.of(Material.values())
        .filter(material -> !material.name().startsWith("LEGACY_") && material != Material.AIR && material.isItem())
        .map(ItemStack::new);
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
  @Test
  void testMethods() {
    RecipeChoice inputChoice = recipe.getInputChoice();
    recipe.setInput(Material.COAL);
    assertThat("Input modification has no effect", recipe.getInputChoice(), is(inputChoice));
    assertThat("Modified input is not accepted", !inputChoice.test(new ItemStack(Material.COAL)));

    recipe.setInputChoice(new RecipeChoice.MaterialChoice(Material.COAL));
    assertThat("InputChoice modification has no effect", recipe.getInputChoice(), is(inputChoice));

    int cookingTime = recipe.getCookingTime();
    recipe.setCookingTime(cookingTime + 1);
    assertThat("CookingTime modification has no effect", recipe.getCookingTime(), is(cookingTime));

    float experience = recipe.getExperience();
    recipe.setExperience(experience + 1);
    assertThat("Experience modification has no effect", recipe.getExperience(), is(experience));
  }
}