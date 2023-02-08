package com.github.jikoo.enchantableblocks.mock.inventory;

import java.util.Map;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.NotNull;

/**
 * An interface used to help perform corresponding get/set operations on ItemMetas while "cloning"
 * via reflection. Also helps simplify boilerplate in initial mocking because no ItemMeta
 * implementation exists that does not implement Damageable, Repairable, and BlockDataMeta.
 * See CraftMetaItem.
 */
public interface ItemMetaHelper extends ItemMeta, Repairable, Damageable {

  void setEnchants(Map<Enchantment, Integer> enchantments);

  void setStoredEnchants(Map<Enchantment, Integer> enchantments);

  @NotNull ItemMetaHelper clone();

}
