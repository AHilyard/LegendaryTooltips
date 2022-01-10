package com.anthonyhilyard.legendarytooltips.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.item.EnumRarity;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

public class Selectors
{
	private static final int TAG_COMPOUND = 10;
	private static final int TAG_LIST = 9;

	private static Map<String, EnumRarity> rarities = new HashMap<String, EnumRarity>() {{
		put("common", EnumRarity.COMMON);
		put("uncommon", EnumRarity.UNCOMMON);
		put("rare", EnumRarity.RARE);
		put("epic", EnumRarity.EPIC);
	}};

	private static Map<String, BiPredicate<NBTBase, String>> nbtComparators = new HashMap<String, BiPredicate<NBTBase, String>>() {{
		put("=",  (tag, value) -> tag.toString().contentEquals(value));

		put("!=", (tag, value) -> !tag.toString().contentEquals(value));

		put(">",  (tag, value) -> {
			try
			{
				double parsedValue = Double.valueOf(value);
				if (tag instanceof NBTPrimitive)
				{
					return ((NBTPrimitive)tag).getDouble() > parsedValue;
				}
				else
				{
					return false;
				}
			}
			catch (Exception e)
			{
				return false;
			}
		});

		put("<",  (tag, value) -> {
			try
			{
				double parsedValue = Double.valueOf(value);
				if (tag instanceof NBTPrimitive)
				{
					return ((NBTPrimitive)tag).getDouble() < parsedValue;
				}
				else
				{
					return false;
				}
			}
			catch (Exception e)
			{
				return false;
			}
		});
	}};

	/**
	 * Returns true if the given item is matched by the given selector.
	 * @param item An ItemStack instance of an item to check.
	 * @param selector A selector string to check against.
	 * @return True if the item matches, false otherwise.
	 */
	public static boolean itemMatches(ItemStack item, String selector)
	{
		String itemResourceLocation = ForgeRegistries.ITEMS.getKey(item.getItem()).toString();

		// Item ID
		if (selector.equals(itemResourceLocation) || selector.equals(itemResourceLocation.replace("minecraft:", "")))
		{
			return true;
		}
		// Item name color
		else if (selector.startsWith("#"))
		{
			Integer entryColor = TextColor.parseColor(selector);
			if (entryColor != null && entryColor.equals(ItemColor.getColorForItem(item, 0xFFFFFF)))
			{
				return true;
			}
		}
		// Vanilla rarity
		else if (selector.startsWith("!"))
		{
			if (item.getItem().getForgeRarity(item) == rarities.get(selector.substring(1)))
			{
				return true;
			}
		}
		// Mod ID
		else if (selector.startsWith("@"))
		{
			if (itemResourceLocation.startsWith(selector.substring(1) + ":"))
			{
				return true;
			}
		}
		// Item tag
		else if (selector.startsWith("$"))
		{
			if (OreDictionary.containsMatch(false, OreDictionary.getOres(selector.substring(1)), item))
			{
				return true;
			}
		}
		// Item display name
		else if (selector.startsWith("%"))
		{
			if (item.getDisplayName().contains(selector.substring(1)))
			{
				return true;
			}
		}
		// Tooltip text
		else if (selector.startsWith("^"))
		{
			Minecraft mc = Minecraft.getMinecraft();
			List<String> lines = item.getTooltip(mc.player, ITooltipFlag.TooltipFlags.ADVANCED);
			String tooltipText = "";
			
			// Skip title line.
			for (int n = 1; n < lines.size(); n++)
			{
				tooltipText += lines.get(n) + '\n';
			}
			if (tooltipText.contains(selector.substring(1)))
			{
				return true;
			}
		}
		// NBT tag
		else if (selector.startsWith("&"))
		{
			String tagName = selector.substring(1);
			String tagValue = null;
			BiPredicate<NBTBase, String> valueChecker = null;

			// This implementation means tag names containing and comparator strings can't be compared.
			// Hopefully this isn't common.
			for (String comparator : nbtComparators.keySet())
			{
				if (tagName.contains(comparator))
				{
					valueChecker = nbtComparators.get(comparator);
					String[] components = tagName.split(comparator);
					tagName = components[0];
					if (components.length > 1)
					{
						tagValue = components[1];
					}
					break;
				}
			}

			// Look for a tag matching the given name and value.
			return findMatchingSubtag(item.getTagCompound(), tagName, tagValue, valueChecker);
		}

		return false;
	}

	private static boolean findMatchingSubtag(NBTBase tag, String key, String value, BiPredicate<NBTBase, String> valueChecker)
	{
		if (tag == null)
		{
			return false;
		}

		if (tag.getId() == TAG_COMPOUND)
		{
			NBTTagCompound compoundTag = (NBTTagCompound)tag;

			if (compoundTag.hasKey(key))
			{
				// Just checking presence.
				if (value == null && valueChecker == null)
				{
					return true;
				}
				// Otherwise, we will use the provided comparator.
				else
				{
					return valueChecker.test(compoundTag.getTag(key), value);
				}
			}
			else
			{
				for (String innerKey : compoundTag.getKeySet())
				{
					if (compoundTag.getTagId(innerKey) == TAG_LIST || compoundTag.getTagId(innerKey) == TAG_COMPOUND)
					{
						if (findMatchingSubtag(compoundTag.getTag(innerKey), key, value, valueChecker))
						{
							return true;
						}
					}
				}
				return false;
			}
		}
		else if (tag.getId() == TAG_LIST)
		{
			NBTTagList NBTTagList = (NBTTagList)tag;
			for (NBTBase innerTag : NBTTagList)
			{
				if (innerTag.getId() == TAG_LIST || innerTag.getId() == TAG_COMPOUND)
				{
					if (findMatchingSubtag(innerTag, key, value, valueChecker))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
}