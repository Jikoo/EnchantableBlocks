package com.github.jikoo.enchantableblocks.mock.inventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.stubbing.Answer;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public enum ItemStackMocks {
  ;

  public static ItemStack newItemMock(@NotNull ItemType type, int amount) {
    ItemStack stack = mock();

    Material material = Material.getMaterial(type.getKey().getKey().toUpperCase(Locale.ROOT));
    if (material == null) {
      throw new IllegalArgumentException("Unable to locate Material for ItemType " + type.getKey().getKey());
    }
    doReturn(material).when(stack).getType();

    // Amount get/set.
    AtomicInteger amt = new AtomicInteger(amount);
    doAnswer(invocation -> amt.get()).when(stack).getAmount();
    doAnswer(invocation -> {
      amt.set(invocation.getArgument(0));
      return null;
    }).when(stack).setAmount(anyInt());

    // Item meta.
    AtomicReference<ItemMeta> meta = new AtomicReference<>();
    doAnswer(invocation -> {
      ItemMeta existing = meta.get();
      if (existing != null) {
        return existing.clone();
      }
      return Bukkit.getItemFactory().getItemMeta(stack.getType());
    }).when(stack).getItemMeta();
    doAnswer(invocation -> {
      ItemMeta newMeta = invocation.getArgument(0);
      meta.set(newMeta != null ? newMeta.clone() : null);
      return null;
    }).when(stack).setItemMeta(any());
    doAnswer(invocation -> meta.get() != null).when(stack).hasItemMeta();

    // Item cloning.
    doAnswer(invocation -> {
      ItemStack clone = newItemMock(type, amount);
      // Must also clone the meta or methods that manipulate the meta directly will mutate both.
      clone.setItemMeta(get(meta, null, false).map(ItemMeta::clone).orElse(null));
      return clone;
    }).when(stack).clone();

    // Item similarity. Note that the only thing we track other the meta is the count.
    doAnswer(invocation -> {
      ItemStack other = invocation.getArgument(0);
      if (other == null || other.getType() != stack.getType()) {
        return false;
      }
      boolean haveMeta = stack.hasItemMeta();
      if (haveMeta != other.hasItemMeta()) {
        return false;
      }
      if (!haveMeta) {
        return true;
      }
      return Bukkit.getItemFactory().equals(meta.get(), other.getItemMeta());
    }).when(stack).isSimilar(any());

    // Enchantments.
    doAnswer(invocation -> {
      ItemMeta existing = meta.get();
      return existing != null ? existing.getEnchants() : Map.of();
    }).when(stack).getEnchantments();
    doAnswer(invocation -> {
      ItemMeta existing = meta.get();
      return existing == null ? 0 : existing.getEnchantLevel(invocation.getArgument(0));
    }).when(stack).getEnchantmentLevel(any());
    doAnswer(invocation -> {
      ItemMeta existing = meta.get();
      return existing != null && existing.hasEnchant(invocation.getArgument(0));
    }).when(stack).containsEnchantment(any(Enchantment.class));
    Answer<Void> addEnchant = invocation -> {
      get(meta, stack.getType(), true).ifPresent(itemMeta ->
          itemMeta.addEnchant(
            invocation.getArgument(0),
            invocation.getArgument(1),
            // We aren't winning any performance prizes here, a beautiful DRY hack.
            invocation.getMethod().getName().contains("Unsafe")
        )
      );
      return null;
    };
    doAnswer(addEnchant).when(stack).addEnchantment(any(Enchantment.class), anyInt());
    doAnswer(addEnchant).when(stack).addUnsafeEnchantment(any(Enchantment.class), anyInt());
    Answer<Void> addEnchants = invocation -> {
      get(meta, stack.getType(), true).ifPresent(itemMeta -> {
        // DRY hack again
        boolean unsafe = invocation.getMethod().getName().contains("Unsafe");
        Map<Enchantment, Integer> enchants = invocation.getArgument(0);
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
          itemMeta.addEnchant(entry.getKey(), entry.getValue(), unsafe);
        }
      });
      return null;
    };
    doAnswer(addEnchants).when(stack).addEnchantments(any());
    doAnswer(addEnchants).when(stack).addUnsafeEnchantments(any());
    doAnswer(invocation -> {
      ItemMeta existing = meta.get();
      if (existing != null) {
        existing.removeEnchantments();
      }
      return null;
    }).when(stack).removeEnchantments();
    doAnswer(invocation -> {
      ItemMeta existing = meta.get();
      if (existing != null) {
        existing.removeEnchant(invocation.getArgument(0));
      }
      return null;
    }).when(stack).removeEnchantment(any());

    return stack;
  }

  private static @NotNull Optional<ItemMeta> get(
      @NotNull AtomicReference<ItemMeta> container,
      @Nullable Material createFor,
      boolean store
  ) {
    ItemMeta itemMeta = container.get();
    if (itemMeta == null && createFor != null) {
      itemMeta = Bukkit.getItemFactory().getItemMeta(createFor);
      if (!store) {
        return Optional.ofNullable(itemMeta);
      }
      container.set(itemMeta);
    }
    return Optional.ofNullable(itemMeta);
  }

}
