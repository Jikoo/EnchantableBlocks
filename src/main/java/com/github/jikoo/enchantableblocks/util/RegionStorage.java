package com.github.jikoo.enchantableblocks.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class RegionStorage extends YamlConfiguration {

	private final Plugin plugin;
	private final World world;
	private final int regionX;
	private final int regionZ;

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

	@Override
	public void save(@NotNull File file) throws IOException {
		Files.createDirectories(file.toPath().normalize().getParent());

		String yamlData = saveToString();

		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			writer.write(yamlData);
		}
	}

	public File getDataFile() {
		return new File(getWorldDir(), String.format("%1$s_%2$s.yml", regionX, regionZ));
	}

	private File getWorldDir() {
		return new File(getDataDir(), world.getName());
	}

	private File getDataDir() {
		return new File(plugin.getDataFolder(), "data");
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
