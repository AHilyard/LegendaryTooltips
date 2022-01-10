package com.anthonyhilyard.legendarytooltips.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;

public abstract class TextColor
{
	public static Integer getColorFromFormatCode(String formatCode)
	{
		Integer color = Minecraft.getMinecraft().fontRenderer.getColorCode(formatCode.charAt(1));
		if (color == -1)
		{
			color = null;
		}
		return color;
	}

	public static Integer parseColor(String string)
	{
		if (string.startsWith("#"))
		{
			try
			{
				return Integer.parseUnsignedInt(string.substring(1), 16);
			}
			catch (NumberFormatException i) { return null; }
		}

		// The color string doesn't start with #, so check for a named color.
		TextFormatting namedColor = TextFormatting.getValueByName(string);
		if (namedColor != null && namedColor.isColor())
		{
			return getColorFromFormatCode(namedColor.toString());
		}
		return null;
	}
}
