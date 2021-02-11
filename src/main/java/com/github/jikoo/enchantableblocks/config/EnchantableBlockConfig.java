package com.github.jikoo.enchantableblocks.config;

import com.github.jikoo.enchantableblocks.block.EnchantableBlock;
import com.github.jikoo.enchantableblocks.config.data.BooleanWorldSetting;
import com.github.jikoo.enchantableblocks.config.data.EnumWorldSetting;
import com.github.jikoo.enchantableblocks.config.data.ParsedWorldMapping;
import com.github.jikoo.enchantableblocks.config.data.SimpleMultimapWorldSetting;
import com.github.jikoo.enchantableblocks.config.data.SimpleSetWorldSetting;
import com.github.jikoo.enchantableblocks.config.data.ValueConverters;
import com.github.jikoo.enchantableblocks.config.data.WorldMapping;
import com.github.jikoo.enchantableblocks.config.data.WorldSetting;
import com.github.jikoo.enchantableblocks.util.enchant.Enchantability;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collections;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;

public abstract class EnchantableBlockConfig {

    protected final ConfigurationSection section;
    public final WorldSetting<Boolean> enabled;
    public final WorldSetting<Enchantability> tableEnchantability;
    public final WorldSetting<Set<Enchantment>> tableDisabledEnchants;
    public final WorldSetting<Multimap<Enchantment, Enchantment>> tableEnchantmentConflicts;
    public final WorldSetting<Set<Enchantment>> anvilDisabledEnchants;
    public final WorldSetting<Multimap<Enchantment, Enchantment>> anvilEnchantmentConflicts;
    public final WorldMapping<Enchantment, Integer> anvilEnchantmentMax;

    protected EnchantableBlockConfig(FileConfiguration configuration, Class<? extends EnchantableBlock> clazz) {
        String path = "blocks." + clazz.getSimpleName();
        ConfigurationSection configurationSection = configuration.getConfigurationSection(path);
        if (configurationSection == null) {
            configurationSection = configuration.createSection(path);
        }

        this.section = configurationSection;

        this.enabled = new BooleanWorldSetting(section, "enabled", true);
        this.tableEnchantability = new EnumWorldSetting<>(section, "tableEnchantability", Enchantability.STONE);
        this.tableDisabledEnchants = new SimpleSetWorldSetting<>(
                section,
                "tableDisabledEnchants",
                ValueConverters::toEnchant,
                Collections.emptySet());
        Multimap<Enchantment, Enchantment> enchantIncompatibilities = HashMultimap.create();
        enchantIncompatibilities.put(Enchantment.SILK_TOUCH, Enchantment.LOOT_BONUS_BLOCKS);
        this.tableEnchantmentConflicts = new SimpleMultimapWorldSetting<>(
                section,
                "tableEnchantmentConflicts",
                ValueConverters::toEnchant,
                ValueConverters::toEnchant,
                enchantIncompatibilities);
        this.anvilDisabledEnchants = new SimpleSetWorldSetting<>(
                section,
                "anvilDisabledEnchants",
                key -> ValueConverters.toKeyed(Enchantment::getByKey, key),
                Collections.emptySet());
        this.anvilEnchantmentConflicts = new SimpleMultimapWorldSetting<>(
                section,
                "anvilEnchantmentConflicts",
                ValueConverters::toEnchant,
                ValueConverters::toEnchant,
                enchantIncompatibilities);
        this.anvilEnchantmentMax = new ParsedWorldMapping<>(
                section,
                "anvilEnchantmentMax",
                ValueConverters::toEnchant,
                ConfigurationSection::isInt,
                ConfigurationSection::getInt,
                Enchantment::getMaxLevel);
    }

}
