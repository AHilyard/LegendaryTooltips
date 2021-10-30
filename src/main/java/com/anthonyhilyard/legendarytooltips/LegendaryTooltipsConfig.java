package com.anthonyhilyard.legendarytooltips;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.anthonyhilyard.iceberg.util.ItemColor;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

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
			 "The start border color (color at top of tooltip) of each levels' borders. Note that they can be entered as a lowercase 8-digit hex code in the format \"0xaarrggbb\" for convenience.")
	public Long[] startColors = Collections.nCopies(LegendaryTooltips.NUM_FRAMES, 0xFF996922L).toArray(new Long[0]);
	@Comment("The end border color (color at bottom of tooltip) of each levels' borders. Note that they can be entered as a lowercase 8-digit hex code in the format \"0xaarrggbb\" for convenience.")
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
			 "  Tooltip text - ^ followed by any text.  Will match any item with this text anywhere in the tooltip text (besides the name).  Examples: \"^Legendary\"")
	private List<List<String>> itemSelectors = Stream.of(List.of(List.of("!rare", "!epic")), Collections.nCopies(LegendaryTooltips.NUM_FRAMES - 1, List.of(""))).flatMap(List::stream).collect(Collectors.toList());

	private static Map<String, Rarity> rarities = new HashMap<String, Rarity>() {{
		put("common", Rarity.COMMON);
		put("uncommon", Rarity.UNCOMMON);
		put("rare", Rarity.RARE);
		put("epic", Rarity.EPIC);
	}};

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
	}

	public int getFrameLevelForItem(ItemStack item)
	{
		if (frameLevelCache.containsKey(item))
		{
			return frameLevelCache.get(item);
		}

		// Check each level from 0 to 3 for matches for this item, from most specific to least.
		String itemResourceLocation = Registry.ITEM.getKey(item.getItem()).toString();

		for (int i = 0; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			if (i < framePriorities.length)
			{
				int frameIndex = framePriorities[i];
				for (String entry : itemSelectors.get(frameIndex))
				{
					boolean found = false;
					if (entry.equals(itemResourceLocation) || entry.equals(itemResourceLocation.replace("minecraft:", "")))
					{
						found = true;
					}
					else if (entry.startsWith("#"))
					{
						TextColor entryColor = TextColor.parseColor(entry);
						if (entryColor.equals(ItemColor.getColorForItem(item, TextColor.fromRgb(0xFFFFFF))))
						{
							found = true;
						}
					}
					else if (entry.startsWith("!"))
					{
						if (item.getRarity() == rarities.get(entry.substring(1)))
						{
							found = true;
						}
					}
					else if (entry.startsWith("@"))
					{
						if (itemResourceLocation.startsWith(entry.substring(1) + ":"))
						{
							found = true;
						}
					}
					else if (entry.startsWith("$"))
					{
						if (ItemTags.getAllTags().getTagOrEmpty(new ResourceLocation(entry.substring(1))).getValues().contains(item.getItem()))
						{
							found = true;
						}
					}
					else if (entry.startsWith("%"))
					{
						if (item.getDisplayName().getString().contains(entry.substring(1)))
						{
							found = true;
						}
					}
					else if (entry.startsWith("^"))
					{
						Minecraft mc = Minecraft.getInstance();
						List<Component> lines = item.getTooltipLines(mc.player, TooltipFlag.Default.ADVANCED);
						String tooltipText = "";
						
						// Skip title line.
						for (int n = 1; n < lines.size(); n++)
						{
							tooltipText += lines.get(n).getString() + '\n';
						}
						if (tooltipText.contains(entry.substring(1)))
						{
							found = true;
						}
					}

					if (found)
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

	// private static boolean validateCustomBorderEntry(String value)
	// {
	// 	// If this is a tag, check that it exists.
	// 	if (value.startsWith("$"))
	// 	{
	// 		return ResourceLocation.isValidResourceLocation(value.substring(1));
	// 	}
	// 	// If this is a mod, check that mod is loaded.
	// 	else if (value.startsWith("@"))
	// 	{
	// 		return true;
	// 	}
	// 	// If this is a rarity, make sure it's a valid one.
	// 	else if (value.startsWith("!"))
	// 	{
	// 		return rarities.keySet().contains(value.substring(1).toLowerCase());
	// 	}
	// 	// If this is a hex color, ensure it's in a valid format.
	// 	else if (value.startsWith("#"))
	// 	{
	// 		return TextColor.parseColor(value) != null;
	// 	}
	// 	// Text matches are always considered valid.
	// 	else if (value.startsWith("%") || value.startsWith("^"))
	// 	{
	// 		return true;
	// 	}
	// 	// Otherwise it's an item, so just make sure it's a value resource location.
	// 	else
	// 	{
	// 		return value == null || value == "" || ResourceLocation.isValidResourceLocation(value);
	// 	}
	// }

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