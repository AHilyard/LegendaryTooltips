package com.anthonyhilyard.legendarytooltips;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashMap;

import com.anthonyhilyard.iceberg.util.Selectors;
import com.electronwill.nightconfig.core.Config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.LongValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(modid = Loader.MODID, bus = Bus.MOD)
public class LegendaryTooltipsConfig
{
	public static final ForgeConfigSpec SPEC;
	public static final LegendaryTooltipsConfig INSTANCE;

	public final BooleanValue nameSeparator;
	public final BooleanValue bordersMatchRarity;
	public final BooleanValue tooltipShadow;
	public final BooleanValue shineEffect;
	public final BooleanValue compatibilityMode;

	public final LongValue[] startColors = new LongValue[LegendaryTooltips.NUM_FRAMES];
	public final LongValue[] endColors = new LongValue[LegendaryTooltips.NUM_FRAMES];

	private final ConfigValue<List<? extends Integer>> framePriorities;

	private final List<ConfigValue<List<? extends String>>> itemSelectors = new ArrayList<ConfigValue<List<? extends String>>>(LegendaryTooltips.NUM_FRAMES);
	private final ConfigValue<List<? extends String>> blacklist;

	private static final Map<ItemStack, Integer> frameLevelCache = new HashMap<ItemStack, Integer>();

	static
	{
		Config.setInsertionOrderPreserved(true);
		Pair<LegendaryTooltipsConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(LegendaryTooltipsConfig::new);
		SPEC = specPair.getRight();
		INSTANCE = specPair.getLeft();
	}

	public LegendaryTooltipsConfig(ForgeConfigSpec.Builder build)
	{
		build.comment("Client Configuration").push("client").push("visual_options");

		nameSeparator = build.comment(" Whether item names in tooltips should have a line under them separating them from the rest of the tooltip.").define("name_separator", true);
		bordersMatchRarity = build.comment(" If enabled, tooltip border colors will match item rarity colors (except for custom borders).").define("borders_match_rarity", true);
		tooltipShadow = build.comment(" If enabled, tooltips will display a drop shadow.").define("tooltip_shadow", true);
		shineEffect = build.comment(" If enabled, items showing a custom border will have a special shine effect when hovered over.").define("shine_effect", true);
		compatibilityMode = build.comment(" If enabled, utilizes some workarounds to improve compatibility with mods that alter tooltips.").define("compatibility_mode", false);

		build.pop().comment(String.format(" Custom borders are broken into %d \"levels\", with level 0 being intended for the \"best\" or \"rarest\" items. Only level 0 has a custom border built-in, but others can be added with resource packs.", LegendaryTooltips.NUM_FRAMES)).push("custom_borders");
		build.comment(" The start and end border colors of each levels' borders. Note that they can be entered as a hex code in the format \"0xAARRGGBB\" or \"0xRRGGBB\" for convenience.").push("colors");
		for (int i = 0; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			startColors[i] = build.defineInRange(String.format("level%d_start_color", i), 0xFF996922L, 0x00000000L, 0xFFFFFFFFL);
			endColors[i] = build.defineInRange(String.format("level%d_end_color", i), 0xFF5A3A1DL, 0x00000000L, 0xFFFFFFFFL);
		}

		build.pop().comment(" Set border priorities here.  This should be a list of numbers that correspond to border levels, with numbers coming first being higher priority.");
		build.push("priorities");

		framePriorities = build.defineList("priorities", () -> IntStream.rangeClosed(0, LegendaryTooltips.NUM_FRAMES - 1).boxed().collect(Collectors.toList()), e -> ((int)e >= 0 && (int)e < LegendaryTooltips.NUM_FRAMES));

		build.pop().comment(" Entry types:\n" + 
							"   Item name - Use item name for vanilla items or include mod name for modded items.  Examples: \"minecraft:stick\", \"iron_ore\"\n" +
							"   Tag - $ followed by tag name.  Examples: \"$forge:stone\" or \"$planks\"\n" +
							"   Mod name - @ followed by mod identifier.  Examples: \"@spoiledeggs\"\n" +
							"   Rarity - ! followed by item's rarity.  This is ONLY vanilla rarities.  Examples: \"!uncommon\", \"!rare\", \"!epic\"\n" +
							"   Item name color - # followed by color hex code, the hex code must match exactly.  Examples: \"#23F632\"\n" +
							"   Display name - % followed by any text.  Will match any item with this text in its tooltip display name.  Examples: \"%[Uncommon]\"\n" +
							"   Tooltip text - ^ followed by any text.  Will match any item with this text anywhere in the tooltip text (besides the name).  Examples: \"^Legendary\"\n" + 
							"   NBT tag - & followed by tag name and optional comparator (=, >, <, or !=) and value, in the format <tag><comparator><value> or just <tag>.  Examples: \"&Damage=0\", \"&Tier>1\", \"&Broken\", \"&map!=128\"");
		build.push("definitions");

		// Level 0 by default applies to epic and rare items.
		itemSelectors.add(build.defineListAllowEmpty(Arrays.asList("level0_entries"), () -> Arrays.asList("!epic", "!rare"), e -> Selectors.validateSelector((String)e) ));

		// Other levels don't apply to anything by default.
		for (int i = 1; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			itemSelectors.add(build.defineListAllowEmpty(Arrays.asList(String.format("level%d_entries", i)), () -> new ArrayList<String>(), e -> Selectors.validateSelector((String)e) ));
		}

		blacklist = build.comment(" Enter blacklist selectors here using the same format as above.  Any items that match these selectors will NOT show a border.").defineListAllowEmpty(Arrays.asList("blacklist"), () -> Arrays.asList(), e -> Selectors.validateSelector((String)e));
		
		build.pop().pop().pop();
	}

	public int getFrameLevelForItem(ItemStack item)
	{
		if (frameLevelCache.containsKey(item))
		{
			return frameLevelCache.get(item);
		}

		// First check the blacklist.
		for (String entry : blacklist.get())
		{
			if (Selectors.itemMatches(item, entry))
			{
				// Add to cache.
				frameLevelCache.put(item, LegendaryTooltips.NO_BORDER);
				return LegendaryTooltips.NO_BORDER;
			}
		}

		// Check each level from 0 to 3 for matches for this item, from most specific to least.
		for (int i = 0; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			if (i < framePriorities.get().size())
			{
				int frameIndex = framePriorities.get().get(i);
				for (String entry : itemSelectors.get(frameIndex).get())
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

	@SubscribeEvent
	public static void onLoad(ModConfig.Reloading e)
	{
		if (e.getConfig().getModId().equals(Loader.MODID))
		{
			// Clear the frame level cache in case anything has changed.
			frameLevelCache.clear();
		}
	}

	public int getCustomBorderStartColor(int level)
	{
		if (level >= 0 && level <= 15 && startColors[level] != null)
		{
			int startColor = (int)startColors[level].get().longValue();

			// If alpha is 0 but the color isn't 0x00000000, assume alpha is intended to be 0xFF.
			// Only downside is if users want black borders they'd have to specify "0xFF000000".
			if (startColor > 0 && startColor <= 0xFFFFFF)
			{
				return startColor | (0xFF << 24);
			}
			return startColor;
		}
		return -1;
	}

	public int getCustomBorderEndColor(int level)
	{
		if (level >= 0 && level <= 15 && endColors[level] != null)
		{
			int endColor = (int)endColors[level].get().longValue();

			// If alpha is 0 but the color isn't 0x00000000, assume alpha is intended to be 0xFF.
			// Only downside is if users want black borders they'd have to specify "0xFF000000".
			if (endColor > 0 && endColor <= 0xFFFFFF)
			{
				return endColor | (0xFF << 24);
			}
			return endColor;
		}
		return -1;
	}

}