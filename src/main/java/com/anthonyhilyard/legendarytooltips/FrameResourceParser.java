package com.anthonyhilyard.legendarytooltips;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameDefinition;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameSource;
import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

public final class FrameResourceParser implements SimpleSynchronousResourceReloadListener
{
	public static final FrameResourceParser INSTANCE = new FrameResourceParser();
	private FrameResourceParser() { }

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager)
	{
		// Load all versions of data-defined frames now.  Priority for data-defined frames is determined by ordering.
		// Order within a single file, as well as order within the set of files.  Reorder resource packs or mod loading order as needed.
		// JSON-format:
		// {
		// 	"definitions": [
		// 		{
		// 			"image": "legendarytooltips:textures/gui/tooltip_borders.png",	// optional, defaults to standard or resource-pack defined frame texture
		// 			"index": 0,														// optional, defaults to 0
		// 			"startColor": "",												// optional, defaults to #FF996922 also accepts integer color
		// 			"endColor": "",													// optional, defaults to #FF5A3A1D also accepts integer color
		// 			"bgColor": "",													// optional, defaults to #F0160A00 also accepts integer color
		// 			"priority": 0,													// optional, defaults to 0
		// 			"selectors": []													// required
		// 		},
		// 		{
		// 			...
		// 		}
		// 	]
		// }

		LegendaryTooltipsConfig.reset();
		
		try
		{
			for (Resource resource : resourceManager.getResources(new ResourceLocation(Loader.MODID, "frame_definitions.json")))
			{
				try (InputStream inputStream = resource.getInputStream())
				{
					JsonObject rootObject = GsonHelper.parse(new InputStreamReader(inputStream), true);

					// If the definitions key exists, handle it.  It's okay if it's missing.
					if (rootObject.has("definitions"))
					{
						JsonArray definitions = GsonHelper.getAsJsonArray(rootObject, "definitions");
						for (int i = 0; i < definitions.size(); i++)
						{
							JsonObject definitionObject = GsonHelper.convertToJsonObject(definitions.get(i), String.format("definitions[%d]", i));
							String image = TooltipDecor.DEFAULT_BORDERS.toString();
							int index = 0;
							int priority = 0;
							Map<String, TextColor> colors = new HashMap<>(Map.of(
								"startColor", LegendaryTooltipsConfig.DEFAULT_START_COLOR,
								"endColor", LegendaryTooltipsConfig.DEFAULT_END_COLOR,
								"bgColor", LegendaryTooltipsConfig.DEFAULT_BG_COLOR
							));
							List<String> selectors = new ArrayList<String>();

							// Selectors are required, so do those first.
							for (JsonElement selectorElement : GsonHelper.getAsJsonArray(definitionObject, "selectors"))
							{
								selectors.add(GsonHelper.convertToString(selectorElement, "selector"));
							}

							// Update optional components.
							if (definitionObject.has("image"))
							{
								String parsedImage = GsonHelper.getAsString(definitionObject, "image");
								if (ResourceLocation.isValidResourceLocation(parsedImage))
								{
									image = parsedImage;
								}
							}
							if (definitionObject.has("index")) { index = GsonHelper.getAsInt(definitionObject, "index"); }
							if (definitionObject.has("priority")) { priority = GsonHelper.getAsInt(definitionObject, "priority"); }

							// Do the colors now.
							for (String colorKey : colors.keySet())
							{
								if (definitionObject.has(colorKey))
								{
									TextColor parsedColor = null;
									if (GsonHelper.isStringValue(definitionObject, colorKey))
									{
										parsedColor = LegendaryTooltipsConfig.getColor(GsonHelper.getAsString(definitionObject, colorKey));
									}
									else if (GsonHelper.isNumberValue(definitionObject, colorKey))
									{
										parsedColor = LegendaryTooltipsConfig.getColor((Long)GsonHelper.getAsLong(definitionObject, colorKey));
									}
									if (parsedColor != null)
									{
										colors.put(colorKey, parsedColor);
									}
								}
							}
							
							// Finally, add the fully-parsed definition.
							FrameDefinition definition = new FrameDefinition(new ResourceLocation(image), index, colors.get("startColor").getValue(), colors.get("endColor").getValue(), colors.get("bgColor").getValue(), FrameSource.DATA, priority);
							LegendaryTooltipsConfig.INSTANCE.addFrameDefinition(definition, selectors);
						}
					}
				}
				catch (Exception e) { throw e; }
			}
		}
		catch (Exception e)
		{
			Loader.LOGGER.warn("An error occurred while parsing frame definitions data:\n {}", ExceptionUtils.getStackTrace(e));
		}
		
	}

	@Override
	public ResourceLocation getFabricId() { return new ResourceLocation(Loader.MODID, "frame_definitions"); }
}