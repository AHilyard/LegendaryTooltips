package com.anthonyhilyard.legendarytooltips;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.anthonyhilyard.iceberg.util.Selectors;

import net.minecraft.world.item.ItemStack;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.JsonPrimitive;

@Config(name = Loader.MODID)
public class LegendaryTooltipsConfig implements ConfigData
{
	@ConfigEntry.Gui.Excluded
	public static LegendaryTooltipsConfig INSTANCE;

	public static void init()
	{
		AutoConfig.register(LegendaryTooltipsConfig.class, JanksonConfigSerializer::new);
		INSTANCE = AutoConfig.getConfigHolder(LegendaryTooltipsConfig.class).getConfig();
	}

	@Comment("Whether item names in tooltips should have a line under them separating them from the rest of the tooltip.")
	public boolean nameSeparator = true;
	@Comment("If enabled, tooltip border colors will match item rarity colors (except for custom borders).")
	public boolean bordersMatchRarity = true;
	@Comment("If enabled, tooltips will display a drop shadow.")
	public boolean tooltipShadow = true;
	@Comment("If enabled, items showing a custom border will have a special shine effect when hovered over.")
	public boolean shineEffect = true;

	@Comment("Custom borders are broken into 16 \"levels\", with level 0 being intended for the \"best\" or \"rarest\" items. Only level 0 has a custom border built-in, but others can be added with resource packs.\n" +
			 "The start border color (color at top of tooltip) of each levels' borders. Note that they can be entered as a lowercase hex code in the format \"0xaarrggbb\" or \"0xrrggbb\" for convenience.")
	public Long[] startColors = Collections.nCopies(LegendaryTooltips.NUM_FRAMES, 0xFF996922L).toArray(new Long[0]);
	@Comment("The end border color (color at bottom of tooltip) of each levels' borders. Note that they can be entered as a lowercase hex code in the format \"0xaarrggbb\" or \"0xrrggbb\" for convenience.")
	public Long[] endColors = Collections.nCopies(LegendaryTooltips.NUM_FRAMES, 0xFF5A3A1DL).toArray(new Long[0]);

	@Comment("Set border priorities here.  This should be a list of numbers that correspond to border levels, with numbers coming first being higher priority.")
	private Integer[] framePriorities = IntStream.rangeClosed(0, LegendaryTooltips.NUM_FRAMES - 1).boxed().collect(Collectors.toList()).toArray(new Integer[0]);
	
	@Comment("Entry types:\n" + 
			 "  Item name - Use item name for vanilla items or include mod name for modded items.  Examples: \"minecraft:stick\", \"iron_ore\"\n" +
			 "  Tag - $ followed by tag name.  Examples: \"$minecraft:stone\" or \"$planks\"\n" +
			 "  Mod name - @ followed by mod identifier.  Examples: \"@spoiledeggs\"\n" +
			 "  Rarity - ! followed by item's rarity.  This is ONLY vanilla rarities.  Examples: \"!common\", \"!uncommon\", \"!rare\", \"!epic\"\n" +
			 "  Item name color - # followed by color hex code, the hex code must match exactly.  Examples: \"#23F632\"\n" +
			 "  Display name - % followed by any text.  Will match any item with this text in its tooltip display name.  Examples: \"%[Uncommon]\"\n" +
			 "  Tooltip text - ^ followed by any text.  Will match any item with this text anywhere in the tooltip text (besides the name).  Examples: \"^Legendary\"" +
			 "  NBT tag - & followed by tag name and optional comparator (=, >, <, or !=) and value, in the format <tag><comparator><value> or just <tag>. Examples: \"&Damage=0\", \"&Tier>1\", \"&Broken\", \"&map!=128\"")
	private List<List<String>> itemSelectors = Stream.of(List.of(List.of("!rare", "!epic")), Collections.nCopies(LegendaryTooltips.NUM_FRAMES - 1, List.of(""))).flatMap(List::stream).collect(Collectors.toList());

	private static Map<ItemStack, Integer> frameLevelCache = new HashMap<ItemStack, Integer>();

	@Override
	public void validatePostLoad()
	{
		// Convert selectors back to strings, since they automatically get converted to json objects.
		List<List<String>> convertedSelectors = new ArrayList<List<String>>();
		for (List<String> selectorList : itemSelectors)
		{
			List<String> convertedList = new ArrayList<String>();
			for (Object val : selectorList)
			{
				if (val instanceof JsonPrimitive)
				{
					String stringVal = ((JsonPrimitive)val).asString();
					convertedList.add(stringVal);
				}
				else if (val instanceof String)
				{
					convertedList.add((String)val);
				}
			}
			convertedSelectors.add(convertedList);
		}
		itemSelectors = convertedSelectors.subList(Math.max(0, itemSelectors.size() - LegendaryTooltips.NUM_FRAMES), itemSelectors.size());

		// Check for 6-digit color codes and convert them to 8-digit.
		for (int i = 0; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			// If alpha is 0 but the color isn't 0x00000000, assume alpha is intended to be 0xFF.
			// Only downside is if users want black borders they'd have to specify "0xFF000000".
			if (startColors[i] > 0 && startColors[i] <= 0xFFFFFF)
			{
				startColors[i] = startColors[i] | (0xFF << 24);
			}
			if (endColors[i] > 0 && endColors[i] <= 0xFFFFFF)
			{
				endColors[i] = endColors[i] | (0xFF << 24);
			}
		}
	}

	public int getFrameLevelForItem(ItemStack item)
	{
		if (frameLevelCache.containsKey(item))
		{
			return frameLevelCache.get(item);
		}

		// Check each level from 0 to 3 for matches for this item, from most specific to least.
		for (int i = 0; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			if (i < framePriorities.length)
			{
				int frameIndex = framePriorities[i];
				for (String entry : itemSelectors.get(frameIndex))
				{
					if (Selectors.itemMatches(item, entry))
					{
						// Add to cache.
						frameLevelCache.put(item, frameIndex);
						return frameIndex;
					}
				}
			}
		}
		
		// Add to cache.
		frameLevelCache.put(item, LegendaryTooltips.STANDARD);
		return LegendaryTooltips.STANDARD;
	}

	public int getCustomBorderStartColor(int level)
	{
		if (level >= 0 && level <= 15 && startColors[level] != null)
		{
			return (int)startColors[level].longValue();
		}
		return -1;
	}

	public int getCustomBorderEndColor(int level)
	{
		if (level >= 0 && level <= 15 && endColors[level] != null)
		{
			return (int)endColors[level].longValue();
		}
		return -1;
	}

}