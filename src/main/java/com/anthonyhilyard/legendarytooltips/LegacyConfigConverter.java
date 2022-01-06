package com.anthonyhilyard.legendarytooltips;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;


public class LegacyConfigConverter
{
	private static final String FILENAME = "legendarytooltips.json5";

	public static void convert()
	{
		// Check for a legacy configuration file (.json5).
		File legacyConfig = FabricLoader.getInstance().getConfigDir().resolve(FILENAME).toFile();
		if (legacyConfig.exists() && !legacyConfig.isDirectory())
		{
			Loader.LOGGER.info("Legacy configuration file \"{}\" found, converting to new format!", FILENAME);

			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(legacyConfig)))
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
					for (int i = 0; i < startColorsArray.size() && i < LegendaryTooltipsConfig.startColorConfigs.size(); i++)
					{
						LegendaryTooltipsConfig.startColorConfigs.get(i).set(((Long)startColorsArray.get(i).getAsLong()) & 0xFFFFFFFFL);
					}
				}

				if (rootObject.has("endColors"))
				{
					JsonArray endColorsArray = GsonHelper.getAsJsonArray(rootObject, "endColors");
					for (int i = 0; i < endColorsArray.size() && i < LegendaryTooltipsConfig.endColorConfigs.size(); i++)
					{
						LegendaryTooltipsConfig.endColorConfigs.get(i).set(((Long)endColorsArray.get(i).getAsLong()) & 0xFFFFFFFFL);
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

				Loader.LOGGER.info("Backing up legacy file to \"{}.bak\".  Configuration file \"{}\" updated from legacy configuration.", FILENAME, "legendarytooltips-common.toml");
				LegendaryTooltipsConfig.SPEC.save();
				LegendaryTooltipsConfig.reset();

				reader.close();

				legacyConfig.renameTo(FabricLoader.getInstance().getConfigDir().resolve(FILENAME + ".bak").toFile());
			}
			catch (Exception e)
			{
				// Crap, something happened.  Oh well.
				Loader.LOGGER.warn("An error occurred while converting a legacy configuration: {}", e);
			}
		}
	}
}