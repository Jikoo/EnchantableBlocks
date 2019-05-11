package com.github.jikoo.enchantableblocks.util;

import org.bukkit.Chunk;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BlockMap<V> {

	private final Map<String, TreeMap<Integer, TreeMap<Integer, Map<Integer, V>>>> serverMap = new HashMap<>();

	public V put(Block block, V value) {
		Wrapper<V> previousValue = new Wrapper<>();

		this.serverMap.compute(block.getWorld().getName(), (worldName, worldMap) -> {
			if (worldMap == null) {
				worldMap = new TreeMap<>();
			}

			worldMap.compute(block.getX(), (blockX, blockXMap) -> {
				if (blockXMap == null) {
					blockXMap = new TreeMap<>();
				}

				blockXMap.compute(block.getZ(), (blockZ, blockZMap) -> {
					if (blockZMap == null) {
						blockZMap = new HashMap<>();
					}

					previousValue.set(blockZMap.put(block.getY(), value));

					return blockZMap.isEmpty() ? null : blockZMap;
				});

				return blockXMap.isEmpty() ? null : blockXMap;
			});

			return worldMap.isEmpty() ? null : worldMap;
		});

		return previousValue.get();
	}

	public V get(Block block) {
		Wrapper<V> wrapper = new Wrapper<>();

		this.serverMap.computeIfPresent(block.getWorld().getName(), (worldName, worldMap) -> {
			worldMap.computeIfPresent(block.getX(), (blockX, blockXMap) -> {
				blockXMap.computeIfPresent(block.getZ(), (blockZ, blockZMap) -> {
					wrapper.set(blockZMap.remove(block.getY()));
					return blockZMap.isEmpty() ? null : blockZMap;
				});
				return blockXMap.isEmpty() ? null : blockXMap;
			});
			return worldMap.isEmpty() ? null : worldMap;
		});

		return wrapper.get();
	}

	public V remove(Block block) {
		Wrapper<V> previousValue = new Wrapper<>();

		this.serverMap.compute(block.getWorld().getName(), (worldName, worldMap) -> {
			if (worldMap == null) {
				worldMap = new TreeMap<>();
			}

			worldMap.compute(block.getX(), (blockX, blockXMap) -> {
				if (blockXMap == null) {
					blockXMap = new TreeMap<>();
				}

				blockXMap.compute(block.getZ(), (blockZ, blockZMap) -> {
					if (blockZMap == null) {
						blockZMap = new HashMap<>();
					}

					previousValue.set(blockZMap.remove(block.getY()));

					return blockZMap.isEmpty() ? null : blockZMap;
				});

				return blockXMap.isEmpty() ? null : blockXMap;
			});

			return worldMap.isEmpty() ? null : worldMap;
		});

		return previousValue.get();
	}

	public Collection<V> get(Chunk chunk) {
		return get(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}

	public Collection<V> get(String world, int chunkX, int chunkZ) {
		List<V> values = new ArrayList<>();
		this.serverMap.computeIfPresent(world, (worldName, worldMap) -> {
			int blockXMin = CoordinateConversions.chunkToBlock(chunkX);
			Map<Integer, TreeMap<Integer, Map<Integer, V>>> blockXMap = worldMap.subMap(blockXMin, blockXMin + 16);
			blockXMap.entrySet().removeIf(blockXMapping -> {
				int blockZMin = CoordinateConversions.chunkToBlock(chunkZ);
				Map<Integer, Map<Integer, V>> blockZMap = blockXMapping.getValue().subMap(blockZMin, blockZMin + 16);
				blockZMap.entrySet().removeIf(blockZMapping -> {
					values.addAll(blockZMapping.getValue().values());
					return blockZMapping.getValue().isEmpty();
				});
				return blockXMapping.getValue().isEmpty();
			});
			return worldMap.isEmpty() ? null : worldMap;
		});
		return values;
	}

	public Collection<V> remove(Chunk chunk) {
		return remove(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
	}

	public Collection<V> remove(String world, int chunkX, int chunkZ) {
		List<V> values = new ArrayList<>();
		this.serverMap.computeIfPresent(world, (worldName, worldMap) -> {
			int blockXMin = CoordinateConversions.chunkToBlock(chunkX);
			Map<Integer, TreeMap<Integer, Map<Integer, V>>> blockXMap = worldMap.subMap(blockXMin, blockXMin + 16);
			blockXMap.entrySet().removeIf(blockXMapping -> {
				int blockZMin = CoordinateConversions.chunkToBlock(chunkZ);
				Map<Integer, Map<Integer, V>> blockZMap = blockXMapping.getValue().subMap(blockZMin, blockZMin + 16);
				blockZMap.entrySet().removeIf(blockZMapping -> {
					values.addAll(blockZMapping.getValue().values());
					blockZMapping.getValue().clear();
					return true;
				});
				return blockXMapping.getValue().isEmpty();
			});
			return worldMap.isEmpty() ? null : worldMap;
		});
		return values;

	}

}
