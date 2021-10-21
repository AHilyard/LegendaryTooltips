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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Loader.MODID, bus = Bus.MOD)
public class LegendaryTooltipsConfig
{
	public static final ForgeConfigSpec SPEC;
	public static final LegendaryTooltipsConfig INSTANCE;

	public final BooleanValue nameSeparator;
	public final BooleanValue bordersMatchRarity;
	public final BooleanValue tooltipShadow;
	public final BooleanValue shineEffect;

	public final LongValue[] startColors = new LongValue[LegendaryTooltips.NUM_FRAMES];
	public final LongValue[] endColors = new LongValue[LegendaryTooltips.NUM_FRAMES];
	
	private static final List<ConfigValue<List<? extends String>>> itemSelectors = new ArrayList<ConfigValue<List<? extends String>>>(LegendaryTooltips.NUM_FRAMES);

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

		build.pop().comment(String.format(" Custom borders are broken into %d \"levels\", with level 0 being intended for the \"best\" or \"rarest\" items. Only level 0 has a custom border built-in, but others can be added with resource packs.", LegendaryTooltips.NUM_FRAMES)).push("custom_borders");
		build.comment(" The start and end border colors of each levels' borders. Note that they can be entered as a hex code in the format \"0xAARRGGBB\" for convenience.").push("colors");
		for (int i = 0; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			startColors[i] = build.defineInRange(String.format("level%d_start_color", i), 0xFF996922L, 0x00000000L, 0xFFFFFFFFL);
			endColors[i] = build.defineInRange(String.format("level%d_end_color", i), 0xFF5A3A1DL, 0x00000000L, 0xFFFFFFFFL);
		}

		build.pop().comment(" Entry types:\n" + 
							"   Item name - Use item name for vanilla items or include mod name for modded items.  Examples: minecraft:stick, iron_ore\n" +
							"   Tag - $ followed by tag name.  Examples: $forge:stone or $planks\n" +
							"   Mod name - @ followed by mod identifier.  Examples: @spoiledeggs\n" +
							"   Rarity - ! followed by item's rarity.  This is ONLY vanilla rarities.  Examples: !uncommon, !rare, !epic\n" +
							"   Item name color - # followed by color hex code, the hex code must match exactly.  Examples: #23F632\n" +
							"   Display name - % followed by any text.  Will match any item with this text in its tooltip display name.  Examples: %[Uncommon]\n" +
							"   Tooltip text - ^ followed by any text.  Will match any item with this text anywhere in the tooltip text (besides the name).  Examples: ^Rarity: Legendary" +
							"   PLEASE NOTE: Lower frame levels take precedence, so if two levels match the same item, that item will display the lower numbered frame!");
		build.push("definitions");

		itemSelectors.clear();

		// Level 0 by default applies to epic and rare items.
		itemSelectors.add(build.defineListAllowEmpty(Arrays.asList("level0_entries"), () -> Arrays.asList("!epic", "!rare"), e -> validateCustomBorderEntry((String)e) ));

		// Other levels don't apply to anything by default.
		for (int i = 1; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			itemSelectors.add(build.defineListAllowEmpty(Arrays.asList(String.format("level%d_entries", i)), () -> new ArrayList<String>(), e -> validateCustomBorderEntry((String)e) ));
		}
		build.pop().pop().pop();
	}

	public int getFrameLevelForItem(ItemStack item)
	{
		if (frameLevelCache.containsKey(item))
		{
			return frameLevelCache.get(item);
		}

		// Check each level from 0 to 3 for matches for this item, from most specific to least.
		String itemResourceLocation = item.getItem().getRegistryName().toString();

		for (int i = 0; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			for (String entry : itemSelectors.get(i).get())
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
		frameLevelCache.put(item, LegendaryTooltips.STANDARD);
		return LegendaryTooltips.STANDARD;
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
	public static void onReload(ModConfigEvent.Reloading e)
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
			return (int)startColors[level].get().longValue();
		}
		return -1;
	}

	public int getCustomBorderEndColor(int level)
	{
		if (level >= 0 && level <= 15 && endColors[level] != null)
		{
			return (int)endColors[level].get().longValue();
		}
		return -1;
	}

}