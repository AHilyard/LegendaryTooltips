package com.anthonyhilyard.legendarytooltips;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import com.anthonyhilyard.iceberg.util.Selectors;
import com.anthonyhilyard.iceberg.util.Selectors.SelectorDocumentation;
import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import com.anthonyhilyard.prism.text.DynamicColor;
import com.anthonyhilyard.prism.util.ConfigHelper;
import com.anthonyhilyard.prism.util.IColor;
import com.anthonyhilyard.prism.util.ImageAnalysis;
import com.anthonyhilyard.prism.util.ConfigHelper.ColorFormatDocumentation;
import com.electronwill.nightconfig.core.Config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Loader.MODID, bus = Bus.MOD)
public class LegendaryTooltipsConfig
{
	public static final ForgeConfigSpec SPEC;
	public static final LegendaryTooltipsConfig INSTANCE;

	public enum FrameSource
	{
		NONE,
		CONFIG,
		API,
		DATA
	}

	public enum ColorType
	{
		BORDER_START,
		BORDER_END,
		BG_START,
		BG_END
	}

	public record FrameDefinition(ResourceLocation resource, int index, Supplier<Integer> startBorder, Supplier<Integer> endBorder, Supplier<Integer> startBackground, Supplier<Integer> endBackground, FrameSource source, int priority) {};
	private static final FrameDefinition STANDARD_BORDER = new FrameDefinition(null, LegendaryTooltips.STANDARD, null, null, null, null, FrameSource.NONE, 0);
	private static final FrameDefinition NO_BORDER = new FrameDefinition(null, LegendaryTooltips.NO_BORDER, null, null, null, null, FrameSource.NONE, 0);

	public static final Map<ColorType, TextColor> defaultColors = Map.of(
		ColorType.BORDER_START, TextColor.fromRgb(0xFF996922),
		ColorType.BORDER_END, TextColor.fromRgb(0xFF5A3A1D),
		ColorType.BG_START, TextColor.fromRgb(0xF0160A00),
		ColorType.BG_END, TextColor.fromRgb(0xE8160A00)
	);

	public final BooleanValue nameSeparator;
	public final BooleanValue bordersMatchRarity;
	public final BooleanValue tooltipShadow;
	public final BooleanValue shineEffect;
	public final BooleanValue centeredTitle;
	public final BooleanValue enforceMinimumWidth;

	private final TextColor[] startColors = new TextColor[LegendaryTooltips.NUM_FRAMES];
	private final TextColor[] endColors = new TextColor[LegendaryTooltips.NUM_FRAMES];
	private final TextColor[] startBGColors = new TextColor[LegendaryTooltips.NUM_FRAMES];
	private final TextColor[] endBGColors = new TextColor[LegendaryTooltips.NUM_FRAMES];

	private final ConfigValue<List<? extends Integer>> framePriorities;
	
	private static final List<ConfigValue<List<? extends String>>> itemSelectors = new ArrayList<ConfigValue<List<? extends String>>>(LegendaryTooltips.NUM_FRAMES);
	private final ConfigValue<List<? extends String>> blacklist;

	private static final Map<FrameDefinition, Set<String>> customFrameDefinitions = new LinkedHashMap<>();

	private static final Map<ItemStack, FrameDefinition> frameDefinitionCache = new HashMap<>();

	private static final List<Supplier<ConfigValue<?>>> colorSuppliers = new ArrayList<>();

	static
	{
		Config.setInsertionOrderPreserved(true);
		Pair<LegendaryTooltipsConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(LegendaryTooltipsConfig::new);
		SPEC = specPair.getRight();
		INSTANCE = specPair.getLeft();
	}

	public LegendaryTooltipsConfig(ForgeConfigSpec.Builder build)
	{
		build.comment(" Legendary Tooltips Configuration Instructions\n\n" +

					  " *** READ THIS FIRST ***\n\n" +

					  " By default, this mod does not apply special borders to most items.  It was designed to work well with mod packs where\n" +
					  " the available selection of items can vary widely, so it is up to the user or mod pack designer to customize as needed.\n" +
					  " There are many options available for setting up which custom borders (also called frames) apply to which items.  Follow these steps:\n" +
					  "   1. Decide which items you want to have custom borders, and which borders.  Note that each custom border has a number associated with it (starting at 0).\n" +
					  "   2. For each custom border you want to use, fill out the associated list in the \"definitions\" section.  This will be filled out with a list of \"selectors\",\n" +
					  "      each of which tell the mod what items have that border.  Please read the information above the definitions section for specifics.\n" +
					  "   3. Selectors for borders are checked in the order provided in the \"priorities\" section.  Once a match is found, that border is displayed.\n" +
					  "      For example, if border 0 had the selector \"%Diamond\" and border 1 had the selector \"diamond_sword\", they would both match for diamond swords.\n" +
					  "      In this case, whichever border number came first in the priority list would be the border that would get drawn in-game.\n" +
					  "   4. Optionally, border colors associated with custom borders can be set in the \"colors\" section.  The start color is the color at the top of the tooltip,\n" +
					  "      and the end color is the bottom, with a smooth transition between.  Please read the information above the color section for specifics.").push("client").push("visual_options");

		nameSeparator = build.comment(" Whether item names in tooltips should have a line under them separating them from the rest of the tooltip.").define("name_separator", true);
		bordersMatchRarity = build.comment(" If enabled, tooltip border colors will match item rarity colors (except for custom borders).").define("borders_match_rarity", true);
		tooltipShadow = build.comment(" If enabled, tooltips will display a drop shadow.").define("tooltip_shadow", true);
		shineEffect = build.comment(" If enabled, items showing a custom border will have a special shine effect when hovered over.").define("shine_effect", true);
		centeredTitle = build.comment(" If enabled, tooltip titles will be drawn centered.").define("centered_title", true);
		enforceMinimumWidth = build.comment(" If enabled, tooltips with custom borders will always be at least wide enough to display all border decorations.").define("enforce_minimum_width", false);

		build.pop().comment(String.format(" Custom borders are broken into %d \"levels\", with level 0 being intended for the \"best\" or \"rarest\" items. Only level 0 has a custom border built-in, but others can be added with resource packs.", LegendaryTooltips.NUM_FRAMES)).push("custom_borders");

		// Build the comment for manual borders.
		StringBuilder selectorsComment = new StringBuilder(" Entry types:\n");
		for (SelectorDocumentation doc : Selectors.selectorDocumentation())
		{
			selectorsComment.append("    ").append(doc.name()).append(" - ").append(doc.description());

			if (!doc.examples().isEmpty())
			{
				selectorsComment.append("  Examples: ");
				for (int i = 0; i < doc.examples().size(); i++)
				{
					if (i > 0)
					{
						selectorsComment.append(", ");
					}
					selectorsComment.append("\"").append(doc.examples().get(i)).append("\"");
				}
			}
			selectorsComment.append("\n");
		}

		// Remove the final newline.
		selectorsComment.setLength(selectorsComment.length() - 1);

		build.pop().comment(selectorsComment.toString());
		build.push("definitions");

		itemSelectors.clear();

		// Level 0 by default applies to epic and rare items.
		itemSelectors.add(build.defineListAllowEmpty(Arrays.asList("level0_entries"), () -> Arrays.asList("!epic", "!rare"), e -> Selectors.validateSelector((String)e) ));

		// Other levels don't apply to anything by default.
		for (int i = 1; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			itemSelectors.add(build.defineListAllowEmpty(Arrays.asList(String.format("level%d_entries", i)), () -> new ArrayList<String>(), e -> Selectors.validateSelector((String)e) ));
		}
		blacklist = build.comment(" Enter blacklist selectors here using the same format as above. Any items that match these selectors will NOT show a border.").defineListAllowEmpty(Arrays.asList("blacklist"), () -> Arrays.asList(), e -> Selectors.validateSelector((String)e));

		build.pop().comment(" Set border priorities here.  This should be a list of numbers that correspond to border levels, with numbers coming first being higher priority.\n" +
							" Optionally, -1 can be inserted to indicate relative priority of data and api-defined borders.  If you don't know what that means, you don't need to worry about it.").push("priorities");
		framePriorities = build.defineList("priorities", () -> IntStream.rangeClosed(0, LegendaryTooltips.NUM_FRAMES - 1).boxed().collect(Collectors.toList()), e -> ((int)e >= -1 && (int)e < LegendaryTooltips.NUM_FRAMES));

		// Build the comment for manual borders.
		StringBuilder colorFormatsComment = new StringBuilder(" VALID COLOR FORMATS\n");
		for (ColorFormatDocumentation doc : ConfigHelper.colorFormatDocumentation())
		{
			colorFormatsComment.append("   ").append(doc.name()).append(" - ").append(doc.description().replace("\n", "\n         "));

			if (!doc.examples().isEmpty())
			{
				colorFormatsComment.append("\n     Examples: ");
				for (int i = 0; i < doc.examples().size(); i++)
				{
					if (i > 0)
					{
						colorFormatsComment.append(", ");
					}
					colorFormatsComment.append(doc.examples().get(i));
				}
			}
			colorFormatsComment.append("\n\n");
		}

		// Remove the final newline.
		colorFormatsComment.setLength(colorFormatsComment.length() - 2);

		build.pop().comment(" The colors used for each tooltip, in this order: top border color, bottom border color, top background color, bottom background color.\n" +
							" None of these colors are required, though any colors not specified will be replaced with the default tooltip colors.\n\n" +
							colorFormatsComment.toString()).push("colors");

		colorSuppliers.clear();
		for (int i = 0; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			// We need to define the configuration paths here.
			ConfigValue<?> colorsValue = build.defineList(String.format("level%d_colors", i),
				i == 0 ?
				List.<Object>of(defaultColors.get(ColorType.BORDER_START).getValue(), defaultColors.get(ColorType.BORDER_END).getValue(), defaultColors.get(ColorType.BG_START).getValue(), defaultColors.get(ColorType.BG_END).getValue()) :
				List.<Object>of("auto", "auto", "auto", "auto"), v -> validateColor(v));
			
			// Store them as suppliers for resolution after the spec is finished being built.
			colorSuppliers.add(() -> colorsValue);
		}

		build.pop().pop();
	}

	public static TextColor getColor(Object value)
	{
		return getColor(value, null, null, 0, null);
	}

	public static TextColor getColor(Object value, TextColor defaultColor, ResourceLocation borderImage, int index, ColorType colorType)
	{
		TextColor color = (TextColor)(Object)ConfigHelper.parseColor(value);
		if (color == null)
		{
			// If the specified color is automatic, try to get the right color for this border.
			if (value instanceof String string && string.contentEquals("auto") && borderImage != null)
			{
				Rect2i region = new Rect2i((index / 8) * 64, (index * 16) % 128, 64, 16);

				switch (colorType)
				{
					case BORDER_START:
						region.setHeight(8);
						break;
					case BORDER_END:
						region.setHeight(8);
						region.setY(region.getY() + 8);
						break;
					default:
				}

				color = ImageAnalysis.getDominantColor(borderImage, region);

				// Color can be null if the image was entirely transparent, white, or black for example.
				// Don't return the default color for automatic spec.
				if (color == null)
				{
					return defaultColor;
				}

				switch (colorType)
				{
					case BORDER_START:
						if (DynamicColor.fromColor((IColor)(Object)color).value() > 80)
						{
							color = ConfigHelper.applyModifiers(List.of("-v10", "+s10"), color);
							if (DynamicColor.fromColor((IColor)(Object)color).value() > 200)
							{
								color = ConfigHelper.applyModifiers(List.of("-v10"), color);
							}
						}
						if (DynamicColor.fromColor((IColor)(Object)color).saturation() > 40)
						{
							color = ConfigHelper.applyModifiers(List.of("+s10"), color);
						}
						break;
					case BORDER_END:
						if (DynamicColor.fromColor((IColor)(Object)color).value() > 80)
						{
							color = ConfigHelper.applyModifiers(List.of("-v30"), color);
							if (DynamicColor.fromColor((IColor)(Object)color).value() > 170 &&
								DynamicColor.fromColor((IColor)(Object)color).value() < 220)
							{
								color = ConfigHelper.applyModifiers(List.of("-v30"), color);
							}
						}
						if (DynamicColor.fromColor((IColor)(Object)color).saturation() > 40)
						{
							color = ConfigHelper.applyModifiers(List.of("+s50"), color);
						}
						break;
					case BG_START:
						color = ConfigHelper.applyModifiers(List.of("=v8", "+s50", "=a245"), color);
						break;
					case BG_END:
						color = ConfigHelper.applyModifiers(List.of("=v20", "+s75", "=a230"), color);
						break;
				}
				
				if (color != null)
				{
					return color;
				}
			}
			return defaultColor;
		}
		else
		{
			return color;
		}
	}

	private static boolean validateColor(Object value)
	{
		return (getColor(value) != null || (value instanceof String string && string.contentEquals("auto")));
	}

	private static void resolveColors()
	{
		for (int i = 0; i < LegendaryTooltips.NUM_FRAMES; i++)
		{
			Object colors = colorSuppliers.get(i).get().get();

			if (colors instanceof List<?> colorsList)
			{
				INSTANCE.startColors[i] =	getColor(colorsList.size() > 0 ? colorsList.get(0) : null, defaultColors.get(ColorType.BORDER_START), TooltipDecor.DEFAULT_BORDERS, i, ColorType.BORDER_START);
				INSTANCE.endColors[i] =		getColor(colorsList.size() > 1 ? colorsList.get(1) : null, defaultColors.get(ColorType.BORDER_END), TooltipDecor.DEFAULT_BORDERS, i, ColorType.BORDER_END);
				INSTANCE.startBGColors[i] =	getColor(colorsList.size() > 2 ? colorsList.get(2) : null, defaultColors.get(ColorType.BG_START), TooltipDecor.DEFAULT_BORDERS, i, ColorType.BG_START);
				INSTANCE.endBGColors[i] =	getColor(colorsList.size() > 3 ? colorsList.get(3) : null, defaultColors.get(ColorType.BG_END), TooltipDecor.DEFAULT_BORDERS, i, ColorType.BG_END);
			}
			else
			{
				INSTANCE.startColors[i] =	defaultColors.get(ColorType.BORDER_START);
				INSTANCE.endColors[i] =		defaultColors.get(ColorType.BORDER_END);
				INSTANCE.startBGColors[i] =	defaultColors.get(ColorType.BG_START);
				INSTANCE.endBGColors[i] =	defaultColors.get(ColorType.BG_END);
			}
		}
	}

	/**
	 * Adds a new custom frame definition.  If the same frame definition already exists, 
	 * the provided selectors are added after the already-configured selectors.
	 */
	public void addFrameDefinition(ResourceLocation resource, int index, Supplier<Integer> startBorder, Supplier<Integer> endBorder, Supplier<Integer> background, int priority, List<String> selectors)
	{
		addFrameDefinition(resource, index, startBorder, endBorder, background, background, priority, selectors);
	}

	/**
	 * Adds a new custom frame definition.  If the same frame definition already exists, 
	 * the provided selectors are added after the already-configured selectors.
	 */
	public void addFrameDefinition(ResourceLocation resource, int index, Supplier<Integer> startBorder, Supplier<Integer> endBorder, Supplier<Integer> startBackground, Supplier<Integer> endBackground, int priority, List<String> selectors)
	{
		FrameDefinition definition = new FrameDefinition(resource, index, startBorder, endBorder, startBackground, endBackground, FrameSource.API, priority);
		addFrameDefinition(definition, selectors);
	}

	void addFrameDefinition(FrameDefinition definition, List<String> selectors)
	{
		if (definition.source != FrameSource.API && definition.source != FrameSource.DATA)
		{
			return;
		}

		Set<String> selectorSet = new LinkedHashSet<>();
		if (customFrameDefinitions.containsKey(definition))
		{
			selectorSet.addAll(customFrameDefinitions.get(definition));
		}
		selectorSet.addAll(selectors);
		customFrameDefinitions.put(definition, selectorSet);
	}

	public FrameDefinition getFrameDefinition(ItemStack item)
	{
		if (frameDefinitionCache.containsKey(item))
		{
			return frameDefinitionCache.get(item);
		}

		if (item == null)
		{
			frameDefinitionCache.put(item, STANDARD_BORDER);
			return STANDARD_BORDER;
		}

		if (startColors[0] == null)
		{
			// Somehow colors haven't been resolved yet, so do it now.
			resolveColors();
		}

		// First check the blacklist.
		for (String entry : blacklist.get())
		{
			if (Selectors.itemMatches(item, entry))
			{
				// Add to cache.
				frameDefinitionCache.put(item, NO_BORDER);
				return NO_BORDER;
			}
		}

		List<Integer> priorities = framePriorities.get().stream().map(i -> Integer.valueOf(i)).collect(Collectors.toCollection(ArrayList::new));
		if (!priorities.contains(-1))
		{
			priorities.add(0, -1);
		}

		// Check each level for matches for this item, from most specific to least.
		for (int i = 0; i < priorities.size(); i++)
		{
			int frameIndex = priorities.get(i);

			// Standard config-based frame.
			if (frameIndex != -1 && frameIndex < LegendaryTooltips.NUM_FRAMES)
			{
				TextColor startColor =		startColors[frameIndex] == null ? defaultColors.get(ColorType.BORDER_START) : startColors[frameIndex];
				TextColor endColor =		endColors[frameIndex] == null ? defaultColors.get(ColorType.BORDER_END) : endColors[frameIndex];
				TextColor startBGColor =	startBGColors[frameIndex] == null ? defaultColors.get(ColorType.BG_START) : startBGColors[frameIndex];
				TextColor endBGColor =		endBGColors[frameIndex] == null ? defaultColors.get(ColorType.BG_END) : endBGColors[frameIndex];

				for (String entry : itemSelectors.get(frameIndex).get())
				{
					if (Selectors.itemMatches(item, entry))
					{
						// Add to cache.
						FrameDefinition frameDefinition = new FrameDefinition(TooltipDecor.DEFAULT_BORDERS, frameIndex, () -> startColor.getValue(), () -> endColor.getValue(), () -> startBGColor.getValue(), () -> endBGColor.getValue(), FrameSource.CONFIG, i);
						frameDefinitionCache.put(item, frameDefinition);
						return frameDefinition;
					}
				}
			}
			// Either API or data-specified frames.
			else
			{
				// Sort the definitions by priority.
				List<FrameDefinition> sortedDefinitions = customFrameDefinitions.keySet().stream().sorted((a, b) -> Integer.compare(a.priority, b.priority)).toList();

				// Check API frames first, since they are probably less common and more likely to do something weird.
				for (FrameDefinition frameDefinition : sortedDefinitions)
				{
					for (String entry : customFrameDefinitions.get(frameDefinition))
					{
						if (Selectors.itemMatches(item, entry))
						{
							// Add to cache.
							frameDefinitionCache.put(item, frameDefinition);
							return frameDefinition;
						}
					}
				}
			}
		}
		
		// Add to cache.
		frameDefinitionCache.put(item, STANDARD_BORDER);
		return STANDARD_BORDER;
	}

	@SubscribeEvent
	public static void onReload(ModConfigEvent.Reloading e)
	{
		if (e.getConfig().getModId().equals(Loader.MODID))
		{
			// Clear the frame level cache in case anything has changed.
			frameDefinitionCache.clear();

			// Also resolve the colors again.
			resolveColors();
		}
	}
}