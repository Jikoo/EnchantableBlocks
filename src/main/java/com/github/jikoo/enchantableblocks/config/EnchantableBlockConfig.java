package com.github.jikoo.enchantableblocks.config;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.config.data.Mapping;
import com.github.jikoo.enchantableblocks.config.data.ParsedMapping;
import com.github.jikoo.enchantableblocks.config.data.Setting;
import com.github.jikoo.enchantableblocks.config.data.ValueConverters;
import com.github.jikoo.enchantableblocks.config.data.impl.BooleanSetting;
import com.github.jikoo.enchantableblocks.config.data.impl.EnumSetting;
import com.github.jikoo.enchantableblocks.config.data.impl.MultimapEnchantEnchantSetting;
import com.github.jikoo.enchantableblocks.config.data.impl.SetEnchantSetting;
import com.github.jikoo.enchantableblocks.util.enchant.Enchantability;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EnchantableBlockConfig {

    protected final ConfigurationSection section;
    public final Setting<Boolean> enabled;
    public final Setting<Enchantability> tableEnchantability;
    public final Setting<Set<Enchantment>> tableDisabledEnchants;
    public final Setting<Multimap<Enchantment, Enchantment>> tableEnchantmentConflicts;
    public final Setting<Set<Enchantment>> anvilDisabledEnchants;
    public final Setting<Multimap<Enchantment, Enchantment>> anvilEnchantmentConflicts;
    public final Mapping<Enchantment, Integer> anvilEnchantmentMax;

    protected EnchantableBlockConfig(FileConfiguration configuration, Class<? extends EnchantableBlock> clazz) {
        String path = "blocks." + clazz.getSimpleName();
        ConfigurationSection configurationSection = configuration.getConfigurationSection(path);
        if (configurationSection == null) {
            configurationSection = configuration.createSection(path);
        }

        this.section = configurationSection;

        this.enabled = new BooleanSetting(section, "enabled", true);
        this.tableEnchantability = new EnumSetting<>(section, "tableEnchantability", Enchantability.STONE);
        this.tableDisabledEnchants = new SetEnchantSetting(
                section,
                "tableDisabledEnchantments",
                Collections.emptySet());
        Multimap<Enchantment, Enchantment> enchantIncompatibilities = HashMultimap.create();
        enchantIncompatibilities.put(Enchantment.SILK_TOUCH, Enchantment.LOOT_BONUS_BLOCKS);
        this.tableEnchantmentConflicts = new MultimapEnchantEnchantSetting(
                section,
                "tableEnchantmentConflicts",
                enchantIncompatibilities);
        this.anvilDisabledEnchants = new SetEnchantSetting(
                section,
                "anvilDisabledEnchantments",
                Collections.emptySet());
        this.anvilEnchantmentConflicts = new MultimapEnchantEnchantSetting(
                section,
                "anvilEnchantmentConflicts",
                enchantIncompatibilities);
        this.anvilEnchantmentMax = new ParsedMapping<Enchantment, Integer>(
                section,
                "anvilEnchantmentMax",
                Enchantment::getMaxLevel) {

            @Override
            protected @Nullable Enchantment convertKey(@NotNull String key) {
                return ValueConverters.toEnchant(key);
            }

            @Override
            protected boolean testValue(@NotNull ConfigurationSection localSection, @NotNull String path) {
                return localSection.isInt(path);
            }

            @Override
            protected @NotNull Integer convertValue(@NotNull ConfigurationSection localSection, @NotNull String path) {
                return localSection.getInt(path);
            }
        };
    }

}
