package com.github.jikoo.enchantableblocks;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.config.EnchantableBlockConfig;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EnchantableBlockRegistry {

    private final EnumMap<Material, Class<? extends EnchantableBlock>> materialRegistry;
    private final Map<Class<? extends EnchantableBlock>, Collection<Enchantment>> enchantmentRegistry;
    private final Map<Class<? extends EnchantableBlock>, Supplier<? extends EnchantableBlockConfig>> configRegistry;
    private final Collection<Runnable> reloads;

    EnchantableBlockRegistry() {
        materialRegistry = new EnumMap<>(Material.class);
        enchantmentRegistry = new HashMap<>();
        configRegistry = new HashMap<>();
        reloads = new HashSet<>();
    }

    public void register(
            @NotNull Collection<Material> materials,
            @NotNull Class<? extends EnchantableBlock> blockClass,
            @NotNull Collection<Enchantment> enchants,
            @NotNull Supplier<@NotNull ? extends EnchantableBlockConfig> getConfig,
            @NotNull Runnable reload
    ) {
        materials.forEach(material -> materialRegistry.put(material, blockClass));
        this.enchantmentRegistry.put(blockClass, enchants);
        this.configRegistry.put(blockClass, getConfig);
        this.reloads.add(reload);
    }

    public @Nullable Class<? extends EnchantableBlock> get(Material material) {
        return materialRegistry.get(material);
    }

    public Collection<Enchantment> getEnchants(Class<? extends EnchantableBlock> blockClass) {
        return enchantmentRegistry.get(blockClass);
    }

    public EnchantableBlockConfig getConfig(Class<? extends EnchantableBlock> blockClass) {
        return configRegistry.get(blockClass).get();
    }

    public void reload() {
        reloads.forEach(Runnable::run);
    }

}
