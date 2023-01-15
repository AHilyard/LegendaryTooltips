package com.anthonyhilyard.legendarytooltips.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.anthonyhilyard.legendarytooltips.Loader;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.ColorType;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.GsonHelper;


public class LegacyConfigConverter
{
	private static final String JSON_FILENAME = "legendarytooltips.json5";
	private static final String TOML_FILENAME = "legendarytooltips-common.toml";

	public static void convert()
	{
		// Check for a legacy configuration file (.json5).
		File legacyJsonConfig = FabricLoader.getInstance().getConfigDir().resolve(JSON_FILENAME).toFile();
		if (legacyJsonConfig.exists() && !legacyJsonConfig.isDirectory())
		{
			Loader.LOGGER.info("Legacy configuration file \"{}\" found, converting to new format!", JSON_FILENAME);

			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(legacyJsonConfig)))
			{
				// File found!  Let's parse it then rename it.
				JsonObject rootObject = GsonHelper.parse(reader, true);

				if (rootObject.has("nameSeparator"))
				{
					LegendaryTooltipsConfig.INSTANCE.nameSeparator.set(GsonHelper.getAsBoolean(rootObject, "nameSeparator"));
				}

				if (rootObject.has("bordersMatchRarity"))
				{
					LegendaryTooltipsConfig.INSTANCE.bordersMatchRarity.set(GsonHelper.getAsBoolean(rootObject, "bordersMatchRarity"));
				}

				if (rootObject.has("tooltipShadow"))
				{
					LegendaryTooltipsConfig.INSTANCE.tooltipShadow.set(GsonHelper.getAsBoolean(rootObject, "tooltipShadow"));
				}

				if (rootObject.has("shineEffect"))
				{
					LegendaryTooltipsConfig.INSTANCE.shineEffect.set(GsonHelper.getAsBoolean(rootObject, "shineEffect"));
				}

				if (rootObject.has("startColors"))
				{
					JsonArray startColorsArray = GsonHelper.getAsJsonArray(rootObject, "startColors");
					for (int i = 0; i < startColorsArray.size() && i < LegendaryTooltipsConfig.INSTANCE.startColors.length; i++)
					{
						LegendaryTooltipsConfig.INSTANCE.startColors[i] = TextColor.fromRgb(startColorsArray.get(i).getAsInt() & 0xFFFFFFFF);
					}
				}

				if (rootObject.has("endColors"))
				{
					JsonArray endColorsArray = GsonHelper.getAsJsonArray(rootObject, "endColors");
					for (int i = 0; i < endColorsArray.size() && i < LegendaryTooltipsConfig.INSTANCE.endColors.length; i++)
					{
						LegendaryTooltipsConfig.INSTANCE.endColors[i] = TextColor.fromRgb(endColorsArray.get(i).getAsInt() & 0xFFFFFFFF);
					}
				}

				if (rootObject.has("framePriorities"))
				{
					List<Integer> newPriorities = new ArrayList<>();
					JsonArray framePrioritiesArray = GsonHelper.getAsJsonArray(rootObject, "framePriorities");
					for (int i = 0; i < framePrioritiesArray.size(); i++)
					{
						newPriorities.add(framePrioritiesArray.get(i).getAsInt());
					}
					LegendaryTooltipsConfig.INSTANCE.framePriorities.set(newPriorities);
				}

				if (rootObject.has("itemSelectors"))
				{
					JsonArray itemSelectorsArray = GsonHelper.getAsJsonArray(rootObject, "itemSelectors");
					for (int i = 0; i < itemSelectorsArray.size(); i++)
					{
						List<String> newSelectors = new ArrayList<>();
						for (JsonElement selector : itemSelectorsArray.get(i).getAsJsonArray())
						{
							newSelectors.add(selector.getAsString());
						}
						LegendaryTooltipsConfig.itemSelectors.get(i).set(newSelectors);
					}
				}

				Loader.LOGGER.info("Backing up legacy file to \"{}.bak\".  Configuration file \"{}\" updated from legacy configuration.", JSON_FILENAME, "legendarytooltips-common.toml");
				LegendaryTooltipsConfig.SPEC.save();
				LegendaryTooltipsConfig.reset();

				reader.close();

				legacyJsonConfig.renameTo(FabricLoader.getInstance().getConfigDir().resolve(JSON_FILENAME + ".bak").toFile());
			}
			catch (Exception e)
			{
				// Crap, something happened.  Oh well.
				Loader.LOGGER.warn("An error occurred while converting a legacy configuration: {}", e);
			}
		}

		// Check for a legacy configuration file (.toml).
		File legacyTomlConfig = FabricLoader.getInstance().getConfigDir().resolve(TOML_FILENAME).toFile();
		if (legacyTomlConfig.exists() && !legacyTomlConfig.isDirectory())
		{
			boolean convert = false;
			List<List<Object>> colorDefs = Lists.newArrayList();
			try (FileInputStream fileStream = new FileInputStream(legacyTomlConfig))
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(fileStream));

				// File found!  Let's parse it.
				Config parsedConfig = Config.of(TomlFormat.instance());
				TomlParser parser = new TomlParser();
				parser.parse(reader, parsedConfig, ParsingMode.REPLACE);

				if (parsedConfig.get("client.custom_borders.colors.level0_start_color") != null)
				{
					for (int i = 0; i < 16; i++)
					{
						Object startColor = parsedConfig.get(String.format("client.custom_borders.colors.level%d_start_color", i));
						Object endColor = parsedConfig.get(String.format("client.custom_borders.colors.level%d_end_color", i));
						Object bgColor = parsedConfig.get(String.format("client.custom_borders.colors.level%d_bg_color", i));

						if (i > 0)
						{
							// If the legacy config is using the old default colors for entries above 0, switch it to the new default of "auto".
							if ((startColor instanceof Number && ((Number)startColor).intValue() == (Integer)LegendaryTooltipsConfig.defaultColors.get(ColorType.BORDER_START).getValue()) ||
								(startColor instanceof String && Integer.parseInt((String)startColor) == (Integer)LegendaryTooltipsConfig.defaultColors.get(ColorType.BORDER_START).getValue()))
							{
								startColor = "auto";
							}

							if ((endColor instanceof Number && ((Number)endColor).intValue() == (Integer)LegendaryTooltipsConfig.defaultColors.get(ColorType.BORDER_END).getValue()) ||
								(endColor instanceof String && Integer.parseInt((String)endColor) == (Integer)LegendaryTooltipsConfig.defaultColors.get(ColorType.BORDER_END).getValue()))
							{
								endColor = "auto";
							}
						}

						if (bgColor == null)
						{
							bgColor = "auto";
						}
						colorDefs.add(Arrays.asList(startColor, endColor, bgColor, bgColor));
					}

					Loader.LOGGER.info("Legacy configuration file \"{}\" found, converting to new format!", TOML_FILENAME);
					convert = true;
				}
				reader.close();
			}
			catch (Exception e)
			{
				// Crap, something happened.  Oh well.
				Loader.LOGGER.warn("An error occurred while converting a legacy configuration: {}", ExceptionUtils.getStackTrace(e));
			}

			if (!convert)
			{
				return;
			}

			try
			{
				List<String> lines = Files.readAllLines(legacyTomlConfig.toPath());
				FileWriter writer = new FileWriter(legacyTomlConfig);

				for (int i = 0; i < lines.size(); i++)
				{
					if (lines.get(i).contains("[client.custom_borders]"))
					{
						continue;
					}

					if (lines.get(i).contains("_start_color"))
					{
						Pattern pattern = Pattern.compile("(\\d+)_start");
						Matcher matcher = pattern.matcher(lines.get(i));
						matcher.find();
						int borderIndex = Integer.parseInt(matcher.group(1));
						String replacement = lines.get(i).split("=")[0];
						replacement += colorDefs.get(borderIndex).stream().map(o -> formatColorValues(o)).collect(Collectors.joining(", ", "= [", "]"));
						lines.set(i, replacement.replace("_start_color", "_colors"));
					}

					if (!lines.get(i).contains("_end_color") && !lines.get(i).contains("_bg_color"))
					{
						writer.write(lines.get(i).replace("custom_borders.", "") + "\r\n");
					}
				}
				writer.close();

			}
			catch (Exception e)
			{
				// Crap, something happened.  Oh well.
				Loader.LOGGER.warn("An error occurred while converting a legacy configuration: {}", ExceptionUtils.getStackTrace(e));
			}
		}
	}

	private static String formatColorValues(Object value)
	{
		if (value instanceof String)
		{
			return String.format("\"%s\"", value);
		}
		else if (value instanceof Number)
		{
			Number number = (Number)value;
			return "\"#" + Integer.toHexString(number.intValue()).toUpperCase() + '"';
		}

		return "";
	}
}