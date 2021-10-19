package com.anthonyhilyard.legendarytooltips;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.anthonyhilyard.iceberg.util.ItemColor;
import com.electronwill.nightconfig.core.Config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.LongValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.IConfigEvent;

public class LegendaryTooltipsConfig
{
	public static final ForgeConfigSpec SPEC;
	public static final LegendaryTooltipsConfig INSTANCE;

	public final BooleanValue nameSeparator;
	public final BooleanValue bordersMatchRarity;
	public final BooleanValue tooltipShadow;
	public final BooleanValue shineEffect;

	public final LongValue level0StartColor;
	public final LongValue level0EndColor;
	public final LongValue level1StartColor;
	public final LongValue level1EndColor;
	public final LongValue level2StartColor;
	public final LongValue level2EndColor;
	public final LongValue level3StartColor;
	public final LongValue level3EndColor;
	
	public final ConfigValue<List<? extends String>> level0Items;
	public final ConfigValue<List<? extends String>> level1Items;
	public final ConfigValue<List<? extends String>> level2Items;
	public final ConfigValue<List<? extends String>> level3Items;

	private static final Map<String, Rarity> rarities = new HashMap<String, Rarity>() {{
		put("common", Rarity.COMMON);
		put("uncommon", Rarity.UNCOMMON);
		put("rare", Rarity.RARE);
		put("epic", Rarity.EPIC);
	}};

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

		build.pop().comment(" Custom borders are broken into 4 \"levels\", with level 0 being intended for the \"best\" or \"rarest\" items.  Only level 0 has a custom border built-in, but 1-3 can be added with a resource pack.").push("custom_borders");
		build.push("colors");
		level0StartColor = build.comment(" The start border color of the level 0 custom border.  Can be entered as a hex code in the format \"0xAARRGGBB\".").defineInRange("level0_start_color", 0xFF996922L, 0x00000000L, 0xFFFFFFFFL);
		level0EndColor = build.comment(" The end border color of the level 0 custom border.  Can be entered as a hex code in the format \"0xAARRGGBB\".").defineInRange("level0_end_color", 0xFF5A3A1DL, 0x00000000L, 0xFFFFFFFFL);
		level1StartColor = build.comment(" The start border color of the level 1 custom border.  Can be entered as a hex code in the format \"0xAARRGGBB\".").defineInRange("level1_start_color", 0xFF996922L, 0x00000000L, 0xFFFFFFFFL);
		level1EndColor = build.comment(" The end border color of the level 1 custom border.  Can be entered as a hex code in the format \"0xAARRGGBB\".").defineInRange("level1_end_color", 0xFF5A3A1DL, 0x00000000L, 0xFFFFFFFFL);
		level2StartColor = build.comment(" The start border color of the level 2 custom border.  Can be entered as a hex code in the format \"0xAARRGGBB\".").defineInRange("level2_start_color", 0xFF996922L, 0x00000000L, 0xFFFFFFFFL);
		level2EndColor = build.comment(" The end border color of the level 2 custom border.  Can be entered as a hex code in the format \"0xAARRGGBB\".").defineInRange("level2_end_color", 0xFF5A3A1DL, 0x00000000L, 0xFFFFFFFFL);
		level3StartColor = build.comment(" The start border color of the level 3 custom border.  Can be entered as a hex code in the format \"0xAARRGGBB\".").defineInRange("level3_start_color", 0xFF996922L, 0x00000000L, 0xFFFFFFFFL);
		level3EndColor = build.comment(" The end border color of the level 3 custom border.  Can be entered as a hex code in the format \"0xAARRGGBB\".").defineInRange("level3_end_color", 0xFF5A3A1DL, 0x00000000L, 0xFFFFFFFFL);


		build.pop().comment(" Entry types:\n" + 
							"   Item name - Use item name for vanilla items or include mod name for modded items.  Examples: minecraft:stick, iron_ore\n" +
							"   Tag - $ followed by tag name.  Examples: $forge:stone or $planks\n" +
							"   Mod name - @ followed by mod identifier.  Examples: @spoiledeggs\n" +
							"   Rarity - ! followed by item's rarity.  This is ONLY vanilla rarities.  Examples: !uncommon, !rare, !epic\n" +
							"   Item name color - # followed by color hex code, the hex code must match exactly.  Examples: #23F632\n" +
							"   Display name - % followed by any text.  Will match any item with this text in its tooltip display name.  Examples: %[Uncommon]\n" +
							"   Tooltip text - ^ followed by any text.  Will match any item with this text anywhere in the tooltip text (besides the name).  Examples: ^Rarity: Legendary");
		build.push("definitions");

		level0Items = build.comment(" List of level 0 custom border entries.  Each entry can be an item name, a tag, a mod name, a rarity level, or an item name color.").defineListAllowEmpty(Arrays.asList("level0_entries"), () -> Arrays.asList("!epic"), e -> validateCustomBorderEntry((String)e) );
		level1Items = build.comment(" List of level 1 custom border entries.  Each entry can be an item name, a tag, a mod name, a rarity level, or an item name color.").defineListAllowEmpty(Arrays.asList("level1_entries"), () -> new ArrayList<String>(), e -> validateCustomBorderEntry((String)e) );
		level2Items = build.comment(" List of level 2 custom border entries.  Each entry can be an item name, a tag, a mod name, a rarity level, or an item name color.").defineListAllowEmpty(Arrays.asList("level2_entries"), () -> new ArrayList<String>(), e -> validateCustomBorderEntry((String)e) );
		level3Items = build.comment(" List of level 3 custom border entries.  Each entry can be an item name, a tag, a mod name, a rarity level, or an item name color.").defineListAllowEmpty(Arrays.asList("level3_entries"), () -> new ArrayList<String>(), e -> validateCustomBorderEntry((String)e) );
		
		build.pop().pop().pop();
	}

	public int getFrameLevelForItem(ItemStack item)
	{
		if (frameLevelCache.containsKey(item))
		{
			return frameLevelCache.get(item);
		}

		Map<Integer, List<? extends String>> levelEntries = new HashMap<Integer, List<? extends String>>(){{
			put(0, level0Items.get());
			put(1, level1Items.get());
			put(2, level2Items.get());
			put(3, level3Items.get());
		}};

		// Check each level from 0 to 3 for matches for this item, from most specific to least.
		String itemResourceLocation = item.getItem().getRegistryName().toString();

		for (int i = 0; i < 4; i++)
		{
			for (String entry : levelEntries.get(i))
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
					frameLevelCache.put(item, i);
					return i;
				}
			}
		}
		
		// Add to cache.
		frameLevelCache.put(item, 4);
		return 4;
	}

	private static boolean validateCustomBorderEntry(String value)
	{
		// If this is a tag, check that it exists.
		if (value.startsWith("$"))
		{
			return ResourceLocation.isValidResourceLocation(value.substring(1));
		}
		// If this is a mod, check that mod is loaded.
		else if (value.startsWith("@"))
		{
			return true;
		}
		// If this is a rarity, make sure it's a valid one.
		else if (value.startsWith("!"))
		{
			return rarities.keySet().contains(value.substring(1).toLowerCase());
		}
		// If this is a hex color, ensure it's in a valid format.
		else if (value.startsWith("#"))
		{
			return TextColor.parseColor(value) != null;
		}
		// Text matches are always considered valid.
		else if (value.startsWith("%") || value.startsWith("^"))
		{
			return true;
		}
		// Otherwise it's an item, so just make sure it's a value resource location.
		else
		{
			return value == null || value == "" || ResourceLocation.isValidResourceLocation(value);
		}
	}

	@SubscribeEvent
	public static void onLoad(IConfigEvent e)
	{
		if (e.getConfig().getModId().equals(Loader.MODID))
		{
			// Clear the frame level cache in case anything has changed.
			frameLevelCache.clear();
		}
	}

	public int getCustomBorderStartColor(int level)
	{
		Long value = -1L;
		switch (level)
		{
			case 0:
				value = level0StartColor.get().longValue();
				break;
			case 1:
				value = level1StartColor.get().longValue();
				break;
			case 2:
				value = level2StartColor.get().longValue();
				break;
			case 3:
				value = level3StartColor.get().longValue();
				break;
		}
		return (int)value.longValue();
	}

	public int getCustomBorderEndColor(int level)
	{
		Long value = -1L;
		switch (level)
		{
			case 0:
				value = level0EndColor.get().longValue();
				break;
			case 1:
				value = level1EndColor.get().longValue();
				break;
			case 2:
				value = level2EndColor.get().longValue();
				break;
			case 3:
				value = level3EndColor.get().longValue();
				break;
		}
		return (int)value.longValue();
	}

}