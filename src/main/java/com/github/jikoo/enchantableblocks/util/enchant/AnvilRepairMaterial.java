package com.github.jikoo.enchantableblocks.util.enchant;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

/**
 * Definitions of materials used in anvil repair operations.
 */
public final class AnvilRepairMaterial {

  private static final Map<Material, RecipeChoice> MATERIALS_TO_REPAIRABLE
      = new EnumMap<>(Material.class);

  static {
    String[] armor = new String[] { "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS" };
    String[] tools = new String[] { "_AXE", "_SHOVEL", "_PICKAXE", "_HOE" };
    String[] armortools = new String[armor.length + tools.length];
    System.arraycopy(armor, 0, armortools, 0, armor.length);
    System.arraycopy(tools, 0, armortools, armor.length, tools.length);

    addGear("LEATHER", armor, Material.LEATHER);
    RecipeChoice choiceIronIngot = singleChoice(Material.IRON_INGOT);
    addGear("CHAINMAIL", armor, choiceIronIngot);
    addGear("IRON", armortools, choiceIronIngot);
    MATERIALS_TO_REPAIRABLE.put(Material.SHEARS, choiceIronIngot);
    addGear("GOLDEN", armortools, Material.GOLD_INGOT);
    addGear("DIAMOND", armortools, Material.DIAMOND);
    MATERIALS_TO_REPAIRABLE.put(Material.TURTLE_HELMET, singleChoice(Material.SCUTE));
    addGear("NETHERITE", armortools, Material.NETHERITE_INGOT);
    addGear("WOODEN", tools, new RecipeChoice.MaterialChoice(Tag.PLANKS));
    addGear("STONE", tools, new RecipeChoice.MaterialChoice(Tag.ITEMS_STONE_TOOL_MATERIALS));
  }

  private static void addGear(String type, String[] gearType, RecipeChoice repairChoice) {
    for (String toolType : gearType) {
      Material material = Material.getMaterial(type + toolType);
      if (material != null) {
        MATERIALS_TO_REPAIRABLE.put(material, repairChoice);
      }
    }
  }

  private static void addGear(String type, String[] gearType, Material repairMaterial) {
    addGear(type, gearType, singleChoice(repairMaterial));
  }

  private static RecipeChoice singleChoice(Material material) {
    // RecipeChoice.ExactChoice is a full meta match, which isn't what we want.
    return new RecipeChoice.MaterialChoice(List.of(material));
  }

  /**
   * Get whether an item is repairable by another item.
   *
   * <p>N.B. This is for pure material-based repair operations, not for combination operations.
   *
   * @param base the item to be repaired
   * @param addition the item used to repair
   * @return whether the addition material can repair the base material
   */
  public static boolean repairs(ItemStack base, ItemStack addition) {
    RecipeChoice recipeChoice = MATERIALS_TO_REPAIRABLE.get(base.getType());
    return recipeChoice != null && recipeChoice.test(addition);
  }

  private AnvilRepairMaterial() {}

}
