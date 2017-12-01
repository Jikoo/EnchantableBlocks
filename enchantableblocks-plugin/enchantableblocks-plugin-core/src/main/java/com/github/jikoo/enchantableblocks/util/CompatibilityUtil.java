package com.github.jikoo.enchantableblocks.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.jikoo.enchantableblocks.EnchantableBlocksPlugin;
import com.github.jikoo.enchantableblocks.enchanting.TableEnchantmentHandler;
import org.apache.commons.lang.IllegalClassException;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.InventoryView;

/**
 * A very basic utility for using reflection to attempt the NMS access EnchantedFurnace requires for
 * full functionality.
 *
 * @author Jikoo
 */
public class CompatibilityUtil {

	private static final String VERSION_STRING;

	private static int VERSION_MAJOR;
	private static int VERSION_MINOR;

	private static boolean ANVIL_SUPPORT = false;
	private static boolean FURNACE_SUPPORT = false;

	// CraftInventoryView
	private static Class<?> CRAFTINVENTORYVIEW;
	private static Method CRAFTINVENTORYVIEW_GETHANDLE;

	// NMS ContainerAnvil
	private static Class<?> CONTAINERANVIL;
	private static Field CONTAINERANVIL_NAME;
	private static Field CONTAINERANVIL_EXP_COST;

	// CraftPlayer
	private static Class<?> CRAFTPLAYER;
	private static Method CRAFTPLAYER_GETHANDLE;

	// NMS EntityPlayer
	private static Class<?> ENTITYPLAYER;
	private static Method ENTITYPLAYER_SETCONTAINERDATA;

	// CraftBlockState
	private static Class<?> CRAFTFURNACE;
	private static Method CRAFTFURNACE_GETTILEENTITY;

	// NMS TileEntityFurnace
	private static Class<?> TILEENTITYFURNACE;
	private static Field TILEENTITYFURNACE_COOK_TIME_TOTAL;

	static {
		String packageName = Bukkit.getServer().getClass().getPackage().getName();
		VERSION_STRING = packageName.substring(packageName.lastIndexOf('.') + 1);

		init();
	}

	public static boolean areAnvilsSupported() {
		return ANVIL_SUPPORT;
	}

	public static boolean areFurnacesSupported() {
		return FURNACE_SUPPORT;
	}

	public static String getNameFromAnvil(final InventoryView view) {
		if (!ANVIL_SUPPORT) {
			throw new IllegalStateException(
					"Cannot get anvil name when anvil support is not enabled!");
		}

		if (!(view.getTopInventory() instanceof AnvilInventory)) {
			return null;
		}

		if (VERSION_MAJOR == 1 && VERSION_MINOR >= 12 || VERSION_MAJOR > 1) {
			return ((AnvilInventory) view.getTopInventory()).getRenameText();
		}

		if (!CRAFTINVENTORYVIEW.isAssignableFrom(view.getClass())) {
			return null;
		}

		try {
			Object containerAnvil = CRAFTINVENTORYVIEW_GETHANDLE.invoke(view);
			if (containerAnvil == null
					|| !CONTAINERANVIL.isAssignableFrom(containerAnvil.getClass())) {
				return null;
			}
			return (String) CONTAINERANVIL_NAME.get(containerAnvil);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void init() {
		if (VERSION_STRING == null) {
			return;
		}

		Matcher matcher = Pattern.compile("v([0-9]+)_([0-9]+)_R[0-9]+").matcher(VERSION_STRING);

		String packageOBC = "org.bukkit.craftbukkit";
		String packageNMS = "net.minecraft.server";
		if (!matcher.find()) {
			VERSION_MAJOR = 0;
			VERSION_MINOR = 0;
		} else {
			VERSION_MAJOR = Integer.parseInt(matcher.group(1));
			VERSION_MINOR = Integer.parseInt(matcher.group(2));
			packageOBC += '.' + VERSION_STRING;
			packageNMS += '.' + VERSION_STRING;
		}

		initAnvilSupport(packageOBC, packageNMS);
		initFurnaceSupport(packageOBC, packageNMS);
		// TODO: Enable version-specific  block listener
	}

	private static void initAnvilSupport(final String packageOBC, final String packageNMS) {
		try {
			CRAFTINVENTORYVIEW = Class.forName(packageOBC + ".inventory.CraftInventoryView");
			CRAFTINVENTORYVIEW_GETHANDLE = CRAFTINVENTORYVIEW.getMethod("getHandle");

			CONTAINERANVIL = Class.forName(packageNMS + ".ContainerAnvil");

			boolean under1_12 = VERSION_MAJOR < 1 || VERSION_MAJOR == 1 && VERSION_MINOR <= 11;

			if (under1_12) {
				String containerAnvilNameField;
				if (VERSION_MAJOR < 1 || VERSION_MINOR < 7) {
					containerAnvilNameField = "m";
				} else if (VERSION_MINOR > 7) {
					containerAnvilNameField = "l";
				} else {
					containerAnvilNameField = "n";
				}

				CONTAINERANVIL_NAME = CONTAINERANVIL.getDeclaredField(containerAnvilNameField);
				CONTAINERANVIL_NAME.setAccessible(true);
				CONTAINERANVIL_EXP_COST = CONTAINERANVIL.getDeclaredField("a");
			}

			CRAFTPLAYER = Class.forName(packageOBC + ".entity.CraftPlayer");
			CRAFTPLAYER_GETHANDLE = CRAFTPLAYER.getMethod("getHandle");

			ENTITYPLAYER = Class.forName(packageNMS + ".EntityPlayer");
			ENTITYPLAYER_SETCONTAINERDATA = ENTITYPLAYER.getMethod("setContainerData",
					CONTAINERANVIL.getSuperclass(), int.class, int.class);

			if (CRAFTINVENTORYVIEW_GETHANDLE.getReturnType().isAssignableFrom(CONTAINERANVIL)
					&& (!under1_12 || String.class.isAssignableFrom(CONTAINERANVIL_NAME.getType())
					&& int.class.isAssignableFrom(CONTAINERANVIL_EXP_COST.getType()))
					&& CRAFTPLAYER_GETHANDLE.getReturnType().isAssignableFrom(ENTITYPLAYER)) {
				ANVIL_SUPPORT = true;
			} else {
				System.err.println("[EnchantedFurnace] NMS/OBC field types are not assignable, anvils are unsupported!");
			}
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException
				| NoSuchFieldException e) {
			// Anvils not supported
			System.err.println("[EnchantedFurnace] Error enabling anvil support:");
			e.printStackTrace();
			System.err.println("[EnchantedFurnace] Anvils will not be able to be used to enchant furnaces.");
		}
	}

	private static void initFurnaceSupport(String packageOBC, String packageNMS) {

		if (VERSION_MINOR < 8 && VERSION_MAJOR < 2) {
			// Minimum supported version of 1.8 for furnaces - worked much differently prior.
			System.out.println("[EnchantedFurnace] " + VERSION_STRING + " detected, furnaces will fall back to runnables.");
			return;
		}

		try {
			CRAFTFURNACE = Class.forName(packageOBC + ".block.CraftFurnace");

			Class<?> clazz = CRAFTFURNACE;
			nextSuper: while (clazz != null) {
				for (Method method : clazz.getDeclaredMethods()) {
					if (method.getName().equals("getTileEntity") && method.getParameterTypes().length == 0) {
						CRAFTFURNACE_GETTILEENTITY = method;
						method.setAccessible(true);
						break nextSuper;
					}
				}
				clazz = clazz.getSuperclass();
			}

			TILEENTITYFURNACE = Class.forName(packageNMS + ".TileEntityFurnace");
			TILEENTITYFURNACE_COOK_TIME_TOTAL = TILEENTITYFURNACE.getDeclaredField("cookTimeTotal");
			TILEENTITYFURNACE_COOK_TIME_TOTAL.setAccessible(true);

			// Verify types before giving the all clear
			if (CRAFTFURNACE_GETTILEENTITY != null
					&& CRAFTFURNACE_GETTILEENTITY.getReturnType().isAssignableFrom(TILEENTITYFURNACE)
					&& int.class.isAssignableFrom(TILEENTITYFURNACE_COOK_TIME_TOTAL.getType())) {
				FURNACE_SUPPORT = true;
			} else {
				System.out.println("[EnchantedFurnace] NMS/OBC field types are not assignable, furnaces will fall back to runnables.");
			}
		} catch (ClassNotFoundException | SecurityException | NoSuchFieldException e) {
			// Furnaces not supported
			System.err.println("[EnchantedFurnace] Error enabling furnace support, will fall back to runnables:");
			e.printStackTrace();
		}
	}

	public static TableEnchantmentHandler newTableEnchantmentHandler(EnchantableBlocksPlugin plugin)
			throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

		Class<?> tableEnchantmentHandlerClazz;
		try {
			Class.forName("org.bukkit.enchantments.EnchantmentOffer");
			tableEnchantmentHandlerClazz = Class.forName("com.github.jikoo.enchantableblocks.enchanting.TablePreviewEnchantmentHandler");

		} catch (ClassNotFoundException e) {
			tableEnchantmentHandlerClazz = Class.forName("com.github.jikoo.enchantableblocks.enchanting.LegacyTableEnchantmentHandler");
		}

		if (!TableEnchantmentHandler.class.isAssignableFrom(tableEnchantmentHandlerClazz)) {
			throw new IllegalClassException(TableEnchantmentHandler.class, tableEnchantmentHandlerClazz);
		}

		Constructor<?> tableEnchantmentHandlerConstructor = tableEnchantmentHandlerClazz.getConstructor(EnchantableBlocksPlugin.class);
		return TableEnchantmentHandler.class.cast(tableEnchantmentHandlerConstructor.newInstance(plugin));

	}

	public static void setAnvilExpCost(final InventoryView view, final int cost) {
		if (!ANVIL_SUPPORT) {
			throw new IllegalStateException("Cannot set anvil cost when anvil support is not enabled!");
		}

		if (!(view.getTopInventory() instanceof AnvilInventory)) {
			return;
		}

		// Set repair cost
		if (VERSION_MAJOR > 1 || VERSION_MAJOR == 1 && VERSION_MINOR >= 12) {
			((AnvilInventory) view.getTopInventory()).setRepairCost(cost);
		} else {
			if (!CRAFTINVENTORYVIEW.isAssignableFrom(view.getClass())) {
				return;
			}

			try {
				Object containerAnvil = CRAFTINVENTORYVIEW_GETHANDLE.invoke(view);
				if (!CONTAINERANVIL.isAssignableFrom(containerAnvil.getClass())) {
					return;
				}
				CONTAINERANVIL_EXP_COST.set(containerAnvil, cost);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		// Update repair cost for client
		try {
			HumanEntity player = view.getPlayer();
			if (player == null || !CRAFTPLAYER.isAssignableFrom(player.getClass())) {
				return;
			}
			Object entityPlayer = CRAFTPLAYER_GETHANDLE.invoke(player);
			if (entityPlayer == null || !ENTITYPLAYER.isAssignableFrom(entityPlayer.getClass())) {
				return;
			}
			Object containerAnvil = CRAFTINVENTORYVIEW_GETHANDLE.invoke(view);
			if (containerAnvil == null
					|| !CONTAINERANVIL.isAssignableFrom(containerAnvil.getClass())) {
				return;
			}
			ENTITYPLAYER_SETCONTAINERDATA.invoke(entityPlayer, containerAnvil, 0, cost);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setFurnaceCookTime(final Block block, final int duration) {
		if (!FURNACE_SUPPORT) {
			throw new IllegalStateException(
					"Cannot set furnace cook time when furnace support is not enabled!");
		}
		BlockState state = block.getState();
		if (!CRAFTFURNACE.isAssignableFrom(state.getClass())) {
			return;
		}
		try {
			Object tileEntityFurnace = CRAFTFURNACE_GETTILEENTITY.invoke(state);
			if (tileEntityFurnace == null
					|| !TILEENTITYFURNACE.isAssignableFrom(tileEntityFurnace.getClass())) {
				return;
			}
			TILEENTITYFURNACE_COOK_TIME_TOTAL.set(tileEntityFurnace, Math.max(0, duration));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private CompatibilityUtil() {}

}
