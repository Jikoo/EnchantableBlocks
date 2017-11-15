package com.github.jikoo.enchantedfurnace;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class ReflectionUtils {

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

		ReflectionUtils.init();
	}

	public static boolean areAnvilsSupported() {
		return ReflectionUtils.ANVIL_SUPPORT;
	}

	public static boolean areFurnacesSupported() {
		return ReflectionUtils.FURNACE_SUPPORT;
	}

	private static String getContainerAnvilNameField() {
		switch (ReflectionUtils.VERSION_STRING) {
		case "craftbukkit":
		case "v1_4_5":
		case "v1_4_6":
		case "v1_4_R1":
		case "v1_5_R1":
		case "v1_5_R2":
		case "v1_5_R3":
		case "v1_6_R1":
		case "v1_6_R2":
		case "v1_6_R3":
			return "m";
		case "v1_7_R1":
		case "v1_7_R2":
		case "v1_7_R3":
		case "v1_7_R4":
			return "n";
		case "v1_8_R1":
		case "v1_8_R2":
		case "v1_8_R3":
		case "v1_9_R1":
		case "v1_9_R2":
		case "v1_10_R1":
		case "v1_11_R1":
		default:
			return "l";
		}
	}

	public static String getNameFromAnvil(final InventoryView view) {
		if (!ReflectionUtils.ANVIL_SUPPORT) {
			throw new IllegalStateException(
					"Cannot get anvil name when anvil support is not enabled!");
		}

		if (!(view.getTopInventory() instanceof AnvilInventory)) {
			return null;
		}

		if (ReflectionUtils.VERSION_MINOR >= 12 || ReflectionUtils.VERSION_MAJOR > 1) {
			return ((AnvilInventory) view.getTopInventory()).getRenameText();
		}

		if (!ReflectionUtils.CRAFTINVENTORYVIEW.isAssignableFrom(view.getClass())) {
			return null;
		}

		try {
			Object containerAnvil = ReflectionUtils.CRAFTINVENTORYVIEW_GETHANDLE.invoke(view);
			if (containerAnvil == null
					|| !ReflectionUtils.CONTAINERANVIL.isAssignableFrom(containerAnvil.getClass())) {
				return null;
			}
			return (String) ReflectionUtils.CONTAINERANVIL_NAME.get(containerAnvil);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static void init() {
		if (ReflectionUtils.VERSION_STRING == null) {
			return;
		}

		Matcher matcher = Pattern.compile("v([0-9]+)_([0-9]+)_R[0-9]+").matcher(ReflectionUtils.VERSION_STRING);

		String packageOBC = "org.bukkit.craftbukkit";
		String packageNMS = "net.minecraft.server";
		if (!matcher.find()) {
			ReflectionUtils.VERSION_MAJOR = 0;
			ReflectionUtils.VERSION_MINOR = 0;
		} else {
			ReflectionUtils.VERSION_MAJOR = Integer.parseInt(matcher.group(2));
			ReflectionUtils.VERSION_MINOR = Integer.parseInt(matcher.group(2));
			packageOBC += '.' + ReflectionUtils.VERSION_STRING;
			packageNMS += '.' + ReflectionUtils.VERSION_STRING;
		}

		ReflectionUtils.initAnvilSupport(packageOBC, packageNMS);

		if (ReflectionUtils.VERSION_MINOR < 8 && ReflectionUtils.VERSION_MAJOR < 2) {
			// Minimum supported version of 1.8 for furnaces - worked much differently prior.
			System.out.println("[EnchantedFurnace] " + ReflectionUtils.VERSION_STRING + " detected, furnaces will fall back to runnables.");
			return;
		}

		try {
			ReflectionUtils.CRAFTFURNACE = Class.forName(packageOBC + ".block.CraftFurnace");

			Class<?> clazz = ReflectionUtils.CRAFTFURNACE;
			nextSuper: while (clazz != null) {
				for (Method method : clazz.getDeclaredMethods()) {
					if (method.getName().equals("getTileEntity") && method.getParameterTypes().length == 0) {
						ReflectionUtils.CRAFTFURNACE_GETTILEENTITY = method;
						method.setAccessible(true);
						break nextSuper;
					}
				}
				clazz = clazz.getSuperclass();
			}

			ReflectionUtils.TILEENTITYFURNACE = Class.forName(packageNMS + ".TileEntityFurnace");
			ReflectionUtils.TILEENTITYFURNACE_COOK_TIME_TOTAL = ReflectionUtils.TILEENTITYFURNACE.getDeclaredField("cookTimeTotal");
			ReflectionUtils.TILEENTITYFURNACE_COOK_TIME_TOTAL.setAccessible(true);

			// Verify types before giving the all clear
			if (ReflectionUtils.CRAFTFURNACE_GETTILEENTITY != null
					&& ReflectionUtils.CRAFTFURNACE_GETTILEENTITY.getReturnType().isAssignableFrom(ReflectionUtils.TILEENTITYFURNACE)
					&& int.class.isAssignableFrom(ReflectionUtils.TILEENTITYFURNACE_COOK_TIME_TOTAL.getType())) {
				ReflectionUtils.FURNACE_SUPPORT = true;
			} else {
				System.out.println("[EnchantedFurnace] NMS/OBC field types are not assignable, furnaces will fall back to runnables.");
			}
		} catch (ClassNotFoundException | SecurityException | NoSuchFieldException e) {
			// Furnaces not supported
			System.err.println("[EnchantedFurnace] Error enabling furnace support, will fall back to runnables:");
			e.printStackTrace();
			System.err.println("[EnchantedFurnace] You can safely ignore this error, but it won't get fixed if you don't report it.");
		}
	}

	private static void initAnvilSupport(final String packageOBC, final String packageNMS) {
		try {
			ReflectionUtils.CRAFTINVENTORYVIEW = Class.forName(packageOBC + ".inventory.CraftInventoryView");
			ReflectionUtils.CRAFTINVENTORYVIEW_GETHANDLE = ReflectionUtils.CRAFTINVENTORYVIEW.getMethod("getHandle");

			ReflectionUtils.CONTAINERANVIL = Class.forName(packageNMS + ".ContainerAnvil");

			boolean under1_12 = ReflectionUtils.VERSION_MAJOR < 2 && ReflectionUtils.VERSION_MINOR < 12;

			if (under1_12) {
				ReflectionUtils.CONTAINERANVIL_NAME = ReflectionUtils.CONTAINERANVIL.getDeclaredField(ReflectionUtils.getContainerAnvilNameField());
				ReflectionUtils.CONTAINERANVIL_NAME.setAccessible(true);
				ReflectionUtils.CONTAINERANVIL_EXP_COST = ReflectionUtils.CONTAINERANVIL.getDeclaredField("a");
			}

			ReflectionUtils.CRAFTPLAYER = Class.forName(packageOBC + ".entity.CraftPlayer");
			ReflectionUtils.CRAFTPLAYER_GETHANDLE = ReflectionUtils.CRAFTPLAYER.getMethod("getHandle");

			ReflectionUtils.ENTITYPLAYER = Class.forName(packageNMS + ".EntityPlayer");
			ReflectionUtils.ENTITYPLAYER_SETCONTAINERDATA = ReflectionUtils.ENTITYPLAYER.getMethod("setContainerData",
					ReflectionUtils.CONTAINERANVIL.getSuperclass(), int.class, int.class);

			if (ReflectionUtils.CRAFTINVENTORYVIEW_GETHANDLE.getReturnType().isAssignableFrom(ReflectionUtils.CONTAINERANVIL)
					&& (!under1_12 || String.class.isAssignableFrom(ReflectionUtils.CONTAINERANVIL_NAME.getType())
					&& int.class.isAssignableFrom(ReflectionUtils.CONTAINERANVIL_EXP_COST.getType()))
					&& ReflectionUtils.CRAFTPLAYER_GETHANDLE.getReturnType().isAssignableFrom(ReflectionUtils.ENTITYPLAYER)) {
				ReflectionUtils.ANVIL_SUPPORT = true;
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

	public static void setAnvilExpCost(final InventoryView view, final int cost) {
		if (!ReflectionUtils.ANVIL_SUPPORT) {
			throw new IllegalStateException("Cannot set anvil cost when anvil support is not enabled!");
		}

		if (!(view.getTopInventory() instanceof AnvilInventory)) {
			return;
		}

		// Set repair cost
		if (ReflectionUtils.VERSION_MAJOR > 1 || ReflectionUtils.VERSION_MINOR >= 12) {
			((AnvilInventory) view.getTopInventory()).setRepairCost(cost);
		} else {
			if (!ReflectionUtils.CRAFTINVENTORYVIEW.isAssignableFrom(view.getClass())) {
				return;
			}

			try {
				Object containerAnvil = ReflectionUtils.CRAFTINVENTORYVIEW_GETHANDLE.invoke(view);
				if (!ReflectionUtils.CONTAINERANVIL.isAssignableFrom(containerAnvil.getClass())) {
					return;
				}
				ReflectionUtils.CONTAINERANVIL_EXP_COST.set(containerAnvil, cost);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		// Update repair cost for client
		try {
			HumanEntity player = view.getPlayer();
			if (player == null || !ReflectionUtils.CRAFTPLAYER.isAssignableFrom(player.getClass())) {
				return;
			}
			Object entityPlayer = ReflectionUtils.CRAFTPLAYER_GETHANDLE.invoke(player);
			if (entityPlayer == null || !ReflectionUtils.ENTITYPLAYER.isAssignableFrom(entityPlayer.getClass())) {
				return;
			}
			Object containerAnvil = ReflectionUtils.CRAFTINVENTORYVIEW_GETHANDLE.invoke(view);
			if (containerAnvil == null
					|| !ReflectionUtils.CONTAINERANVIL.isAssignableFrom(containerAnvil.getClass())) {
				return;
			}
			ReflectionUtils.ENTITYPLAYER_SETCONTAINERDATA.invoke(entityPlayer, containerAnvil, 0, cost);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setFurnaceCookTime(final Block block, final int duration) {
		if (!ReflectionUtils.FURNACE_SUPPORT) {
			throw new IllegalStateException(
					"Cannot set furnace cook time when furnace support is not enabled!");
		}
		BlockState state = block.getState();
		if (!ReflectionUtils.CRAFTFURNACE.isAssignableFrom(state.getClass())) {
			return;
		}
		try {
			Object tileEntityFurnace = ReflectionUtils.CRAFTFURNACE_GETTILEENTITY.invoke(state);
			if (tileEntityFurnace == null
					|| !ReflectionUtils.TILEENTITYFURNACE.isAssignableFrom(tileEntityFurnace.getClass())) {
				return;
			}
			ReflectionUtils.TILEENTITYFURNACE_COOK_TIME_TOTAL.set(tileEntityFurnace, Math.max(0, duration));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private ReflectionUtils() {}

}
