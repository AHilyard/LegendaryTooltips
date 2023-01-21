package com.anthonyhilyard.legendarytooltips;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.anthonyhilyard.legendarytooltips.util.Selectors;
import com.anthonyhilyard.legendarytooltips.util.TextColor;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(modid = Loader.MODID, value = Side.CLIENT)
public class LegendaryTooltipsConfig extends Configuration
{
	public static LegendaryTooltipsConfig INSTANCE;

	public static final String DEFAULT_START_COLOR = "#FF996922";
	public static final String DEFAULT_END_COLOR = "#FF5A3A1D";
	public static final String DEFAULT_BG_COLOR = "#F0160A00";

	public boolean nameSeparator;
	public boolean bordersMatchRarity;
	public boolean tooltipShadow;
	public boolean shineEffect;

	public boolean shineRepeat;
	public boolean shineSync;
	public int shineTicks;

	private final List<List<String>> itemSelectors = new ArrayList<List<String>>(LegendaryTooltips.NUM_FRAMES);

	private final List<Integer> framePriorities = new ArrayList<>();

	private String[] startColors = new String[LegendaryTooltips.NUM_FRAMES];
	private String[] endColors = new String[LegendaryTooltips.NUM_FRAMES];
	private String[] bgColors = new String[LegendaryTooltips.NUM_FRAMES];

	private transient final Map<ItemStack, Integer> frameLevelCache = new HashMap<ItemStack, Integer>();


	public static void loadConfig(File file)
	{
		INSTANCE = new LegendaryTooltipsConfig(file);
	}

	private LegendaryTooltipsConfig(File file)
	{
		super(file);
		load();

		// Update the data type of the categories collection so it maintains the proper order.
		try
		{
			Field categoriesField = Configuration.class.getDeclaredField("categories");
			categoriesField.setAccessible(true);
			Map<String, ConfigCategory> categories = new LinkedHashMap<>();

			// Get or create all the categories in the proper order by getting them here.
			categories.put("visual_options", getCategory("visual_options"));
			categories.put("definitions", getCategory("definitions"));
			categories.put("priorities", getCategory("priorities"));
			categories.put("colors", getCategory("colors"));

			categoriesField.set(this, categories);
		}
		catch (Exception e)
		{
			Loader.LOGGER.error(e);
		}

		getCategory("visual_options").setComment(" Legendary Tooltips Configuration Instructions\n\n" +

			" *** READ THIS FIRST ***\n\n" +

			" By default, this mod does not apply special borders to most items.  It was designed to work well with mod packs\n" +
			" where the available selection of items can vary widely, so it is up to the user or mod pack designer to customize as needed.\n" +
			" There are many options available for setting up which custom borders (also called frames) apply to which items.  Follow these steps:\n" +
			"   1. Decide which items you want to have custom borders, and which borders.  Note that each custom border has a number associated with it (starting at 0).\n" +
			"   2. For each custom border you want to use, fill out the associated list in the \"definitions\" section.  This will be filled out with a list of \"selectors\",\n" +
			"      each of which tell the mod what items have that border.  Please read the information above the definitions section for specifics.\n" +
			"   3. Selectors for borders are checked in the order provided in the \"priorities\" section.  Once a match is found, that border is displayed.\n" +
			"      For example, if border 0 had the selector \"%Diamond\" and border 1 had the selector \"diamond_sword\", they would both match for diamond swords.\n" +
			"      In this case, whichever border number came first in the priority list would be the border that would get drawn in-game.\n" +
			"   4. Optionally, border colors associated with custom borders can be set in the \"colors\" section.  The start color is the color at the top of the tooltip,\n" +
			"      and the end color is the bottom, with a smooth transition between.  Please read the information above the color section for specifics.");

		getCategory("definitions").setPropertyOrder(IntStream.rangeClosed(0, 15).boxed().map(x -> String.format("level%d_entries", x)).collect(Collectors.toList()))
			.setComment(" Entry types:\n" + 
			"   Item name - Use item name for vanilla items or include mod name for modded items.  Examples: minecraft:stick, iron_ore\n" +
			"   Item Metadata - After an item name, you may include a colon and then a number to indicate metadata.  Examples: minecraft:wool:5, minecraft:sponge:1\n" +
			"   Tag - $ followed by ore dictionary tag name.  Examples: $plankWood or $ingotIron\n" +
			"   Mod name - @ followed by mod identifier.  Examples: @spoiledeggs\n" +
			"   Rarity - ! followed by item's rarity.  Examples: !uncommon, !rare !epic\n" +
			"   Item name color - # followed by color hex code, the hex code must match exactly.  Examples: #23F632\n" +
			"   Display name - % followed by any text.  Will match any item with this text in its tooltip display name.  Examples: %Uncommon\n" +
			"   Tooltip text - ^ followed by any text.  Will match any item with this text anywhere in the tooltip text (besides the name).  Examples: ^Legendary\n" + 
			"   NBT tag - & followed by tag name and optional comparator (=, >, <, or !=) and value, in the format <tag><comparator><value> or just <tag>.\n" +
			"             Examples: &Damage=0, &Tier>1, &Enchantments, &map!=128");

		getCategory("priorities").setComment("Set border priorities here.  This should be a string containing a comma-separated list of numbers that\n" +
											 "correspond to border levels, with numbers coming first being higher priority.");

		getCategory("colors").setPropertyOrder(IntStream.rangeClosed(0, 15).boxed().flatMap(x -> Stream.of(String.format("level%d_start_color", x), String.format("level%d_end_color", x))).collect(Collectors.toList()))
			.setComment("The start and end border colors and background colors of each level.\n" +
			" Note that they can be entered as any one of: a hex color code in the format #AARRGGBB or #RRGGBB, OR a string color name.\n" +
			" Examples: #FFFF00, #FF73D984, red, #FFCC00");

		nameSeparator = getBoolean("name_separator", "visual_options", true, "Whether item names in tooltips should have a line under them separating them from the rest of the tooltip.");
		bordersMatchRarity = getBoolean("borders_match_rarity", "visual_options", true, "If enabled, tooltip border colors will match item rarity colors (except for custom borders).");
		tooltipShadow = getBoolean("tooltip_shadow", "visual_options", true, "If enabled, tooltips will display a drop shadow.");
		shineEffect = getBoolean("shine_effect", "visual_options", true, "If enabled, items showing a custom border will have a special shine effect when hovered over.");
		shineRepeat = getBoolean("shine_repeat", "visual_options", false, "Whether or not to repeat the shine effect, or to only play it once.");
		shineSync = getBoolean("shine_sync", "visual_options", false, "Whether or not to sync horizontal and vertical shine, or delay vertical shine.");
		shineTicks = getInt("shine_ticks", "visual_options", 50, 50, 800, "The speed at which to player the shine effect, higher value is slower.");


		// Level 0 by default applies to epic and rare items.
		itemSelectors.add(Arrays.asList(getStringList("level0_entries", "definitions", new String[]{"!epic", "!rare"}, "")));

		// Other levels don't apply to anything by default.
		for (int i = 1; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			itemSelectors.add(Arrays.asList(getStringList(String.format("level%d_entries", i), "definitions", new String[0], "")));
		}

		framePriorities.clear();
		framePriorities.addAll(Arrays.asList(getString("priorities", "priorities", "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15", "").split(",")).stream().map(x -> x.trim()).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList()));

		startColors[0] = getString("level0_start_color", "colors", DEFAULT_START_COLOR, "");
		endColors[0] = getString("level0_end_color", "colors", DEFAULT_END_COLOR, "");
		bgColors[0] = getString("level0_bg_color", "colors", DEFAULT_BG_COLOR, "");
		for (int i = 1; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			startColors[i] = getString(String.format("level%d_start_color", i), "colors", DEFAULT_START_COLOR, "");
			endColors[i] = getString(String.format("level%d_end_color", i), "colors", DEFAULT_END_COLOR, "");
			bgColors[i] = getString(String.format("level%d_bg_color", i), "colors", DEFAULT_BG_COLOR, "");
		}

		save();
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
			if (i < framePriorities.size())
			{
				int frameIndex = framePriorities.get(i);
				if (frameIndex < itemSelectors.size())
				{
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
		}
		
		// Add to cache.
		frameLevelCache.put(item, LegendaryTooltips.STANDARD);
		return LegendaryTooltips.STANDARD;
	}

	@SubscribeEvent
	public static void onLoad(final ConfigChangedEvent.OnConfigChangedEvent e)
	{
		if (e.getModID().equals(Loader.MODID))
		{
			ConfigManager.sync(Loader.MODID, Config.Type.INSTANCE);

			// Clear the frame level cache in case anything has changed.
			INSTANCE.frameLevelCache.clear();
		}
	}

	private Integer getColor(String colorString)
	{
		colorString = colorString.toLowerCase().replace("0x", "").replace("#", "");
		Integer color = TextColor.parseColor(colorString);
		if (color == null)
		{
			if (colorString.length() == 6 || colorString.length() == 8)
			{
				color = TextColor.parseColor("#" + colorString);
			}
		}
		return color;
	}

	public Integer getCustomBorderStartColor(int level)
	{
		if (level >= 0 && level <= 15 && startColors[level] != null)
		{
			Integer startColor = getColor(startColors[level]);

			// If alpha is 0 but the color isn't 0x00000000, assume alpha is intended to be 0xFF.
			// Only downside is if users want black borders they'd have to specify "0xFF000000".
			if (startColor > 0 && startColor <= 0xFFFFFF)
			{
				return startColor | (0xFF << 24);
			}
			return startColor;
		}
		return null;
	}

	public Integer getCustomBorderEndColor(int level)
	{
		if (level >= 0 && level <= 15 && endColors[level] != null)
		{
			Integer endColor = getColor(endColors[level]);

			// If alpha is 0 but the color isn't 0x00000000, assume alpha is intended to be 0xFF.
			// Only downside is if users want black borders they'd have to specify "0xFF000000".
			if (endColor > 0 && endColor <= 0xFFFFFF)
			{
				return endColor | (0xFF << 24);
			}
			return endColor;
		}
		return null;
	}

	public Integer getCustomBackgroundColor(int level)
	{
		if (level >= 0 && level <= 15 && bgColors[level] != null)
		{
			Integer bgColor = getColor(bgColors[level]);

			// If alpha is 0 but the color isn't 0x00000000, assume alpha is intended to be 0xFF.
			// Only downside is if users want black borders they'd have to specify "0xFF000000".
			if (bgColor > 0 && bgColor <= 0xFFFFFF)
			{
				return bgColor | (0xFF << 24);
			}
			return bgColor;
		}
		return null;
	}

}
