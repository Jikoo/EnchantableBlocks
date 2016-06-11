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

	private static final String VERSION;

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
	private static Class<?> CRAFTBLOCKSTATE;
	private static Method CRAFTBLOCKSTATE_GETTILEENTITY;

	// NMS TileEntityFurnace
	private static Class<?> TILEENTITYFURNACE;
	private static Field TILEENTITYFURNACE_COOK_TIME_TOTAL;

	static {
		String packageName = Bukkit.getServer().getClass().getPackage().getName();
		VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);

		init();
	}

	private ReflectionUtils() {}

	private static void init() {
		if (VERSION == null) {
			return;
		}

		Matcher matcher = Pattern.compile("v([0-9]+)_([0-9]+)_R[0-9]+").matcher(VERSION);
		if (!matcher.find()) {
			return;
		}

		int major = Integer.parseInt(matcher.group(1));
		int minor = Integer.parseInt(matcher.group(2));

		String packageOBC = "org.bukkit.craftbukkit." + VERSION;
		String packageNMS = "net.minecraft.server." + VERSION;

		try {
			CRAFTINVENTORYVIEW = Class.forName(packageOBC + ".inventory.CraftInventoryView");
			CRAFTINVENTORYVIEW_GETHANDLE = CRAFTINVENTORYVIEW.getMethod("getHandle");

			CONTAINERANVIL = Class.forName(packageNMS + ".ContainerAnvil");
			CONTAINERANVIL_NAME = CONTAINERANVIL.getDeclaredField(getContainerAnvilNameField());
			CONTAINERANVIL_NAME.setAccessible(true);
			CONTAINERANVIL_EXP_COST = CONTAINERANVIL.getDeclaredField("a");

			CRAFTPLAYER = Class.forName(packageOBC + ".entity.CraftPlayer");
			CRAFTPLAYER_GETHANDLE = CRAFTPLAYER.getMethod("getHandle");

			ENTITYPLAYER = Class.forName(packageNMS + ".EntityPlayer");
			ENTITYPLAYER_SETCONTAINERDATA = ENTITYPLAYER.getMethod("setContainerData",
					CONTAINERANVIL.getSuperclass(), int.class, int.class);

			if (CRAFTINVENTORYVIEW_GETHANDLE.getReturnType().isAssignableFrom(CONTAINERANVIL)
					&& String.class.isAssignableFrom(CONTAINERANVIL_NAME.getType())
					&& int.class.isAssignableFrom(CONTAINERANVIL_EXP_COST.getType())
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
		}

		if (minor < 8 && major < 2) {
			// Minimum supported version of 1.8 for furnaces - worked much differently prior.
			System.out.println("[EnchantedFurnace] " + VERSION + " detected, furnaces will fall back to runnables.");
			return;
		}

		try {
			CRAFTBLOCKSTATE = Class.forName(packageOBC + ".block.CraftBlockState");
			CRAFTBLOCKSTATE_GETTILEENTITY = CRAFTBLOCKSTATE.getMethod("getTileEntity");

			TILEENTITYFURNACE = Class.forName(packageNMS + ".TileEntityFurnace");
			TILEENTITYFURNACE_COOK_TIME_TOTAL = TILEENTITYFURNACE.getDeclaredField("cookTimeTotal");
			TILEENTITYFURNACE_COOK_TIME_TOTAL.setAccessible(true);

			// Verify types before giving the all clear
			if (CRAFTBLOCKSTATE_GETTILEENTITY.getReturnType().isAssignableFrom(TILEENTITYFURNACE)
					&& int.class.isAssignableFrom(TILEENTITYFURNACE_COOK_TIME_TOTAL.getType())) {
				FURNACE_SUPPORT = true;
			} else {
				System.out.println("[EnchantedFurnace] NMS/OBC field types are not assignable, furnaces will fall back to runnables.");
			}
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException
				| NoSuchFieldException e) {
			// Furnaces not supported
			System.err.println("[EnchantedFurnace] Error enabling furnace support, will fall back to runnables:");
			e.printStackTrace();
		}
	}

	private static String getContainerAnvilNameField() {
		switch (VERSION) {
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
		default:
			return "l";
		}
	}

	public static boolean areAnvilsSupported() {
		return ANVIL_SUPPORT;
	}

	public static boolean areFurnacesSupported() {
		return FURNACE_SUPPORT;
	}

	public static String getNameFromAnvil(InventoryView view) {
		if (!ANVIL_SUPPORT) {
			throw new IllegalStateException(
					"Cannot get anvil name when anvil support is not enabled!");
		}
		if (!(view.getTopInventory() instanceof AnvilInventory)
				|| !CRAFTINVENTORYVIEW.isAssignableFrom(view.getClass())) {
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

	public static void setAnvilExpCost(InventoryView view, int cost) {
		if (!ANVIL_SUPPORT) {
			throw new IllegalStateException(
					"Cannot set anvil cost when anvil support is not enabled!");
		}
		if (!(view.getTopInventory() instanceof AnvilInventory)
				|| !CRAFTINVENTORYVIEW.isAssignableFrom(view.getClass())) {
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

	public static void updateAnvilExpCost(InventoryView view) {
		if (!ANVIL_SUPPORT) {
			throw new IllegalStateException(
					"Cannot update anvil cost when anvil support is not enabled!");
		}
		if (!(view.getTopInventory() instanceof AnvilInventory)
				|| !CRAFTINVENTORYVIEW.isAssignableFrom(view.getClass())) {
			return;
		}
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
			ENTITYPLAYER_SETCONTAINERDATA.invoke(entityPlayer, containerAnvil, 0,
					CONTAINERANVIL_EXP_COST.get(containerAnvil));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void setFurnaceCookTime(Block block, int duration) {
		if (!FURNACE_SUPPORT) {
			throw new IllegalStateException(
					"Cannot set furnace cook time when furnace support is not enabled!");
		}
		BlockState state = block.getState();
		if (!CRAFTBLOCKSTATE.isAssignableFrom(state.getClass())) {
			return;
		}
		try {
			Object tileEntityFurnace = CRAFTBLOCKSTATE_GETTILEENTITY.invoke(state);
			if (tileEntityFurnace == null
					|| !TILEENTITYFURNACE.isAssignableFrom(tileEntityFurnace.getClass())) {
				return;
			}
			TILEENTITYFURNACE_COOK_TIME_TOTAL.set(tileEntityFurnace, Math.max(0, duration));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

}
