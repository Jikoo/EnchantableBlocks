package com.github.jikoo.enchantableblocks.util.enchant;

public interface Enchantability {

    enum Armor implements Enchantability {
        LEATHER(15),
        CHAIN(12),
        IRON(9),
        GOLD(25),
        DIAMOND(10),
        TURTLE(9),
        NETHERITE(15);

        private final int enchantability;

        Armor(int enchantability) {
            this.enchantability = enchantability;
        }

        @Override
        public int getEnchantability() {
            return enchantability;
        }
    }

    enum Tool implements Enchantability {
        WOOD(15),
        STONE(5),
        IRON(14),
        GOLD(22),
        DIAMOND(10),
        NETHERITE(15),
        BOOK(1);

        private final int enchantability;

        Tool(int enchantability) {
            this.enchantability = enchantability;
        }

        @Override
        public int getEnchantability() {
            return enchantability;
        }
    }

    int getEnchantability();

}
