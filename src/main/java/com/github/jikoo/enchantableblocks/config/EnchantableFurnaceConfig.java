package com.github.jikoo.enchantableblocks.config;

import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import com.github.jikoo.enchantableblocks.config.data.BooleanWorldSetting;
import com.github.jikoo.enchantableblocks.config.data.SimpleSetWorldSetting;
import com.github.jikoo.enchantableblocks.config.data.ValueConverters;
import com.github.jikoo.enchantableblocks.config.data.WorldSetting;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class EnchantableFurnaceConfig extends EnchantableBlockConfig {

    public final WorldSetting<Boolean> fortuneListIsBlacklist;
    public final WorldSetting<Set<Material>> fortuneList;

    public EnchantableFurnaceConfig(FileConfiguration configuration) {
        super(configuration, EnchantableFurnace.class);
        fortuneListIsBlacklist = new BooleanWorldSetting(section, "fortuneListIsBlacklist", true);
        fortuneList = new SimpleSetWorldSetting<>(
                section,
                "fortuneList",
                ValueConverters::toMaterial,
                EnumSet.of(Material.WET_SPONGE, Material.STONE_BRICKS));
    }

}
