package com.github.jikoo.enchantableblocks.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;

/**
 * A placeholder for an invalid recipe.
 */
public class EmptyCookingRecipe extends CookingRecipe<EmptyCookingRecipe> {

    public EmptyCookingRecipe(@NotNull NamespacedKey key) {
        super(
                key,
                new ItemStack(Material.DIRT),
                new RecipeChoice() {
                    @NotNull
                    @Override
                    public ItemStack getItemStack() {
                        return new ItemStack(Material.AIR);
                    }

                    @NotNull
                    @Override
                    public RecipeChoice clone() {
                        try {
                            return (RecipeChoice) super.clone();
                        } catch (CloneNotSupportedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public boolean test(@NotNull ItemStack itemStack) {
                        return false;
                    }
                },
                0,
                0);
    }

    @Override
    public @NotNull EmptyCookingRecipe setInput(@NotNull Material input) {
        return this;
    }

    @Override
    public @NotNull EmptyCookingRecipe setInputChoice(@NotNull RecipeChoice input) {
        return this;
    }

    @Override
    public void setCookingTime(int cookingTime) {
    }

    @Override
    public void setExperience(float experience) {
    }

}
