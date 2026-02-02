## EnchantableBlocks
[![Build](https://github.com/Jikoo/EnchantableBlocks/actions/workflows/ci.yml/badge.svg)](https://github.com/Jikoo/EnchantableBlocks/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=jikoo_enchantableblocks&metric=alert_status)](https://sonarcloud.io/dashboard?id=jikoo_enchantableblocks)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jikoo_enchantableblocks&metric=coverage)](https://sonarcloud.io/dashboard?id=jikoo_enchantableblocks)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=jikoo_enchantableblocks&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=jikoo_enchantableblocks)

EnchantableBlocks is a Bukkit plugin adding effects for enchantments on blocks.

### Get It Now
Builds are available [on BukkitDev](https://dev.bukkit.org/projects/enchantableblocks/files) or [in the releases](https://github.com/Jikoo/EnchantableBlocks/releases).  
Development builds are available for use at your own risk [on AppVeyor](https://ci.appveyor.com/project/Jikoo/enchantableblocks) in the Artifacts tab.

## Features

### Per-World Focus
All features are configurable per-world.
Want an OP world? Not a problem. Want to disable blocks for a vanilla world? Absolutely.
All block settings can be controlled by per-world overrides, falling through to default values when not specifically configured.

### Enchantment Table Enchanting
EnchantableBlocks offers vanilla-style enchantment table usage for supported blocks.
Disable certain enchantments, determine your own conflicts (i.e. silk touch/fortune), or modify enchantability either globally or for a specific world set.

Permission can be granted or denied per-implementation or as a whole. More specific overrides always take precedence.  
Nodes are available as follows:
 * `<plugin name>.enchant.table.<block name>`
   * Permission to enchant a specific block implementation by a plugin in an enchanting table
   * Ex: `enchantableblocks.enchant.table.enchantablefurnace`
   * Note that this is not per-material! I.e. `enchantablefurnace` covers 3 material types.
 * `<plugin name>.enchant.table`
   * Permission to enchant all block implementations by a plugin in an enchanting table
   * Ex: `enchantableblocks.enchant.table`
* `<plugin name>.enchant`
    * Permission to enchant all block implementations by a plugin in any enchantment source
    * Ex: `enchantableblocks.enchant`

### Anvil Enchanting
EnchantableBlocks offers vanilla-style enchantment and combination for supported blocks in anvils.
Supported blocks can be combined with either a matching block or an enchanted book to increase enchantment levels.
Uses vanilla combination rules - higher level takes precedence, equal levels yield an increase of 1 level up to the level cap.
The enchantment level cap is configurable per-enchantment.
Enchantments can be disabled to prevent transfer, though this won't remove them from the base item.
Conflicts are also determined separately for maximum configurability.

Permission can be granted or denied per-implementation or as a whole. More specific overrides always take precedence.  
Nodes are available as follows:
* `<plugin name>.enchant.anvil.<block name>`
    * Permission to enchant a specific block implementation by a plugin in an anvil
    * Ex: `enchantableblocks.enchant.anvil.enchantablefurnace`
    * Note that this is not per-material! I.e. `enchantablefurnace` covers 3 material types.
* `<plugin name>.enchant.anvil`
    * Permission to enchant all block implementations by a plugin in an anvil
    * Ex: `enchantableblocks.enchant.anvil`
* `<plugin name>.enchant`
    * Permission to enchant all block implementations by a plugin in any enchantment source
    * Ex: `enchantableblocks.enchant`

### Block Functions

The only currently block is the EnchantableFurnace, which supports Furnace, Blast Furnace, and Smoker blocks.  
Please see [the wiki](https://github.com/Jikoo/EnchantableBlocks/wiki/Furnace) for functions.

## For Developers

### Contributing
That's awfully decent of you. Here's a quick list:
 * Follow existing style (GoogleStyle)
 * Include tests
   * I don't care if a test for a class doesn't fully cover everything, however, the tests as a whole should come as close to full coverage as can reasonably be expected.
   * While I'm open to new testing libraries, the reason MockBukkit is used over a more generic mocking library such as Mockito is speed. The right tool for the job.

### Implementing
Want to write your own `EnchantableBlock` implementation? All you need to do is implement the block functionality and register it with the `EnchantableBlockRegistry` (obtainable from the `EnchantableBlocksPlugin` instance) on startup.
EnchantableBlocks will handle creation, destruction, loading, saving, and enchanting for you.  
An example implementation can be found in [this package](https://github.com/Jikoo/EnchantableBlocks/tree/master/src/main/java/com/github/jikoo/enchantableblocks/block/impl/furnace).