package com.anthonyhilyard.legendarytooltips.util;

import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;

import java.util.List;

import com.anthonyhilyard.legendarytooltips.Loader;

import net.minecraft.client.Minecraft;

public abstract class ItemColor
{
	public static Integer findFirstColorCode(String rawTitle)
	{
		// This function finds the first specified color code in the given text component.
		// It is intended to skip non-color formatting codes.
		for (int i = 0; i < rawTitle.length(); i += 2)
		{
			// If we encounter a formatting code, check to see if it's a color.  If so, return it.
			if (rawTitle.charAt(i) == '\u00a7')
			{
				try
				{
					String format = String.valueOf(rawTitle.charAt(i)) + String.valueOf(rawTitle.charAt(i + 1));
					Integer color = TextColor.getColorFromFormatCode(format);
					if (color != null)
					{
						return color;
					}
				}
				catch (StringIndexOutOfBoundsException e)
				{
					return null;
				}
			}
			// Otherwise, if we encounter a non-formatting character, bail.
			else
			{
				return null;
			}
		}

		return null;
	}

	public static Integer getColorForItem(ItemStack item, int defaultColor)
	{
		Integer result = null;

		if (item == null)
		{
			return defaultColor;
		}

		// TextColor based on rarity value.
		if (item.getTextComponent() != null &&
			item.getTextComponent().getStyle() != null &&
			item.getTextComponent().getStyle().getColor() != null)
		{
			result = TextColor.getColorFromFormatCode(item.getTextComponent().getStyle().getColor().toString());
		}

		// Now check for NBT, which should override whatever color we have.
		if (item.hasTagCompound() &&
			item.getTagCompound().hasKey("display", 10) &&
			item.getTagCompound().getCompoundTag("display").hasKey("color", 3))
		{
			result = item.getTagCompound().getCompoundTag("display").getInteger("color");
		}

		// If we still haven't found a color or we're still using the rarity color, check the actual tooltip.
		// This is slow, so it better get cached externally!
		if (result == null)
		{
			Minecraft mc = Minecraft.getMinecraft();
			List<String> lines = item.getTooltip(mc.player, ITooltipFlag.TooltipFlags.ADVANCED);
			if (!lines.isEmpty())
			{
				result = findFirstColorCode(lines.get(0));
			}
		}

		// Fallback to the default TextColor if we somehow haven't found a single valid TextColor.
		if (result == null)
		{
			result = defaultColor;
		}

		return result;
	}
}
