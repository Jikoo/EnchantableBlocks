package com.github.jikoo.enchantableblocks.mock.server;

import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

public class TestServerBuildInfo implements ServerBuildInfo {

  @Override
  public @NotNull Key brandId() {
    return Key.key("dummyserver:dummy");
  }

  @Override
  public boolean isBrandCompatible(@NotNull Key brandId) {
    return true;
  }

  @Override
  public @NotNull String brandName() {
    return "dummyserver";
  }

  @Override
  public @NotNull String minecraftVersionId() {
    return "1";
  }

  @Override
  public @NotNull String minecraftVersionName() {
    return "1.2.3";
  }

  @Override
  public @NotNull OptionalInt buildNumber() {
    return OptionalInt.empty();
  }

  @Override
  public @NotNull Instant buildTime() {
    return Instant.now();
  }

  @Override
  public @NotNull Optional<String> gitBranch() {
    return Optional.empty();
  }

  @Override
  public @NotNull Optional<String> gitCommit() {
    return Optional.empty();
  }

  @Override
  public @NotNull String asString(@NotNull StringRepresentation representation) {
    return "";
  }
}
