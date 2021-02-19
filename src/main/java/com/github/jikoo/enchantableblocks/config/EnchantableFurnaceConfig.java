package com.github.jikoo.enchantableblocks.config;

import com.github.jikoo.enchantableblocks.block.EnchantableFurnace;
import com.github.jikoo.planarwrappers.config.Setting;
import com.github.jikoo.planarwrappers.config.SimpleSetSetting;
import com.github.jikoo.planarwrappers.config.impl.BooleanSetting;
import com.github.jikoo.planarwrappers.util.StringConverters;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnchantableFurnaceConfig extends EnchantableBlockConfig {

    public final Setting<Boolean> fortuneListIsBlacklist;
    public final Setting<Set<Material>> fortuneList;

    public EnchantableFurnaceConfig(FileConfiguration configuration) {
        super(configuration, EnchantableFurnace.class);
        fortuneListIsBlacklist = new BooleanSetting(section, "fortuneListIsBlacklist", true);
        fortuneList = new SimpleSetSetting<Material>(
                section,
                "fortuneList",
                EnumSet.of(Material.WET_SPONGE, Material.STONE_BRICKS)) {

            @Override
            protected @Nullable Material convertValue(@NotNull String value) {
                return StringConverters.toMaterial(value);
            }
        };
    }

}
