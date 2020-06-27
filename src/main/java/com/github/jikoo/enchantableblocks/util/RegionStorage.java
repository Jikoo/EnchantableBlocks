package com.github.jikoo.enchantableblocks.util;

import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class RegionStorage extends YamlConfiguration {

	private final Plugin plugin;
	private final World world;
	private final int regionX, regionZ;

	public RegionStorage(Plugin plugin, World world, int regionX, int regionZ) {
		this.plugin = plugin;
		this.world = world;
		this.regionX = regionX;
		this.regionZ = regionZ;
	}

	public void load() throws IOException, InvalidConfigurationException {
		File dataFile = getDataFile();
		if (dataFile.exists()){
			load(dataFile);
		}
	}

	public void save() throws IOException {
		save(getDataFile());
	}

	public File getDataFile() {
		return new File(plugin.getDataFolder(), String.format("data%1$s%2$s%1$s%3$s_%4$s.yml", File.separatorChar, world.getName(), regionX, regionZ));
	}

	public World getWorld() {
		return world;
	}

	public int getRegionX() {
		return regionX;
	}

	public int getRegionZ() {
		return regionZ;
	}

}
