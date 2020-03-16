package com.github.jikoo.enchantableblocks.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockMap<V> {

	private final Map<String, TreeMap<Integer, TreeMap<Integer, Map<Integer, V>>>> serverMap = new HashMap<>();

	public @Nullable V put(@NotNull Block block, @Nullable V value) {
		TreeMap<Integer, TreeMap<Integer, Map<Integer, V>>> worldMap = serverMap.computeIfAbsent(block.getWorld().getName(), k -> new TreeMap<>());
		TreeMap<Integer, Map<Integer, V>> blockXMap = worldMap.computeIfAbsent(block.getX(), k -> new TreeMap<>());
		Map<Integer, V> blockZMap = blockXMap.computeIfAbsent(block.getZ(), (blockZ) -> new HashMap<>());

		return blockZMap.put(block.getY(), value);
	}

	public @Nullable V get(@NotNull Block block) {
		TreeMap<Integer, TreeMap<Integer, Map<Integer, V>>> worldMap = serverMap.get(block.getWorld().getName());
		if (worldMap == null) {
			return null;
		}

		TreeMap<Integer, Map<Integer, V>> blockXMap = worldMap.get(block.getX());
		if (blockXMap == null) {
			return null;
		}

		Map<Integer, V> blockZMap = blockXMap.get(block.getZ());
		if (blockZMap == null) {
			return null;
		}

		return blockZMap.get(block.getY());
	}

	public @Nullable V remove(@NotNull Block block) {
		TreeMap<Integer, TreeMap<Integer, Map<Integer, V>>> worldMap = serverMap.get(block.getWorld().getName());
		if (worldMap == null) {
			return null;
		}

		TreeMap<Integer, Map<Integer, V>> blockXMap = worldMap.get(block.getX());
		if (blockXMap == null) {
			return null;
		}

		Map<Integer, V> blockZMap = blockXMap.get(block.getZ());
		if (blockZMap == null) {
			return null;
		}

		V value = blockZMap.remove(block.getY());

		if (blockZMap.isEmpty()) {
			blockXMap.remove(block.getZ());
			if (blockXMap.isEmpty()) {
				worldMap.remove(block.getX());
				if (worldMap.isEmpty()) {
					serverMap.remove(block.getWorld().getName());
				}
			}
		}

		return value;
	}

	public @NotNull Collection<V> get(@NotNull Chunk chunk) {
		return get(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}

	public @NotNull Collection<V> get(@NotNull String world, int chunkX, int chunkZ) {
		TreeMap<Integer, TreeMap<Integer, Map<Integer, V>>> worldMap = serverMap.get(world);
		if (worldMap == null) {
			return Collections.emptyList();
		}

		int blockXMin = CoordinateConversions.chunkToBlock(chunkX);
		SortedMap<Integer, TreeMap<Integer, Map<Integer, V>>> chunkXSubMap = worldMap.subMap(blockXMin, blockXMin + 16);
		if (chunkXSubMap.isEmpty()) {
			return Collections.emptyList();
		}

		List<V> values = new ArrayList<>();
		int blockZMin = CoordinateConversions.chunkToBlock(chunkZ);
		for (Map.Entry<Integer, TreeMap<Integer, Map<Integer, V>>> blockXEntry : chunkXSubMap.entrySet()) {
			SortedMap<Integer, Map<Integer, V>> chunkZSubMap = blockXEntry.getValue().subMap(blockZMin, blockZMin + 16);

			for (Map<Integer, V> blockYMap : chunkZSubMap.values()) {
				values.addAll(blockYMap.values());
			}
		}

		return values;
	}

	public @NotNull Collection<V> remove(@NotNull Chunk chunk) {
		return remove(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}

	public @NotNull Collection<V> remove(@NotNull String world, int chunkX, int chunkZ) {
		TreeMap<Integer, TreeMap<Integer, Map<Integer, V>>> worldMap = serverMap.get(world);
		if (worldMap == null) {
			return Collections.emptyList();
		}

		int blockXMin = CoordinateConversions.chunkToBlock(chunkX);
		SortedMap<Integer, TreeMap<Integer, Map<Integer, V>>> chunkXSubMap = worldMap.subMap(blockXMin, blockXMin + 16);
		if (chunkXSubMap.isEmpty()) {
			return Collections.emptyList();
		}

		List<V> values = new ArrayList<>();
		int blockZMin = CoordinateConversions.chunkToBlock(chunkZ);

		for (Iterator<TreeMap<Integer, Map<Integer, V>>> blockXIterator = chunkXSubMap.values().iterator(); blockXIterator.hasNext(); ) {
			TreeMap<Integer, Map<Integer, V>> blockXValue = blockXIterator.next();
			SortedMap<Integer, Map<Integer, V>> chunkZSubMap = blockXValue.subMap(blockZMin, blockZMin + 16);

			for (Iterator<Map<Integer, V>> blockZIterator = chunkZSubMap.values().iterator(); blockZIterator.hasNext(); ) {
				values.addAll(blockZIterator.next().values());
				blockZIterator.remove();
			}

			if (blockXValue.isEmpty()) {
				blockXIterator.remove();
			}
		}

		if (worldMap.isEmpty()) {
			serverMap.remove(world);
		}

		return values;
	}

}
