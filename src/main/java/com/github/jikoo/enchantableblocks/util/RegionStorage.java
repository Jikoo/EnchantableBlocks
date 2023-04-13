package com.github.jikoo.enchantableblocks.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * A simplified way of managing a {@link YamlConfiguration} per Minecraft region.
 */
public class RegionStorage extends YamlConfiguration {

  private final @NotNull Path dataDir;
  private final @NotNull Region region;

  /**
   * Construct a new {@code RegionStorage}.
   *
   * @param dataDir the path to the data storage
   * @param region the representation of the Minecraft region
   */
  public RegionStorage(@NotNull Path dataDir, @NotNull Region region) {
    this.dataDir = dataDir;
    this.region = region;
  }

  /**
   * Construct a new {@code RegionStorage}.
   *
   * @param plugin the plugin for which data is being stored
   * @param region the representation of the Minecraft region
   */
  @Deprecated
  public RegionStorage(@NotNull Plugin plugin, @NotNull Region region) {
    this.dataDir = plugin.getDataFolder().toPath().resolve("data");
    this.region = region;
  }

  /**
   * Load the configuration from the default location on disk.
   *
   * <p>Note that if the file is not present, an empty configuration will be returned instead.
   *
   * @throws IOException if there is an issue reading from disk
   * @throws InvalidConfigurationException if the configuration is not valid
   * @see YamlConfiguration#load(File)
   */
  public void load() throws IOException, InvalidConfigurationException {
    File dataFile = getDataFile();
    if (dataFile.exists()) {
      load(dataFile);
    }
  }

  /**
   * Save the configuration to the default location on disk.
   *
   * @throws IOException if there is an issue writing the file to disk
   */
  public void save() throws IOException {
    save(getDataFile());
  }

  /**
   * Save the configuration to disk.
   *
   * <p>Very similar to the overriden method, however, leverages {@link java.nio} features instead
   * of beta Google APIs.
   *
   * @param file the file to save to on disk
   * @throws IOException if there is an issue writing to disk
   * @see org.bukkit.configuration.file.FileConfiguration#save(File)
   */
  @Override
  public void save(@NotNull File file) throws IOException {
    Files.createDirectories(file.toPath().normalize().getParent());

    String yamlData = saveToString();

    try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file),
        StandardCharsets.UTF_8)) {
      writer.write(yamlData);
    }
  }

  /**
   * Get the default storage location on disk.
   *
   * @return the location on disk
   */
  public File getDataFile() {
    return dataDir
        .resolve(Path.of(
            region.worldName(),
            String.format("%1$s_%2$s.yml", region.x(), region.z())
        )).toFile();
  }

  /**
   * Get the {@link Region} that this configuration represents.
   *
   * @return the Minecraft region representation
   */
  public @NotNull Region getRegion() {
    return this.region;
  }

}
