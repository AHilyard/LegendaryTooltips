package com.anthonyhilyard.legendarytooltips;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.ColorType;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameDefinition;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameSource;
import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.exception.ExceptionUtils;

import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;

public final class FrameResourceParser implements ResourceManagerReloadListener
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
							ResourceLocation image = TooltipDecor.DEFAULT_BORDERS;
							int index = 0;
							int priority = 0;
							Map<String, TextColor> colors = new HashMap<String, TextColor>() {{
								put("startColor", LegendaryTooltipsConfig.defaultColors.get(ColorType.BORDER_START));
								put("endColor", LegendaryTooltipsConfig.defaultColors.get(ColorType.BORDER_END));
								put("bgColor", LegendaryTooltipsConfig.defaultColors.get(ColorType.BG_START));
								put("bgStartColor", null);
								put("bgEndColor", null);
							}};
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
									image = new ResourceLocation(parsedImage);;
								}
							}
							if (definitionObject.has("index")) { index = GsonHelper.getAsInt(definitionObject, "index"); }
							if (definitionObject.has("priority")) { priority = GsonHelper.getAsInt(definitionObject, "priority"); }

							// Do the colors now.
							for (String colorKey : colors.keySet())
							{
								if (definitionObject.has(colorKey))
								{
									ColorType colorType;
									switch (colorKey)
									{
										case "startColor":
											colorType = ColorType.BORDER_START;
											break;
										case "endColor":
											colorType = ColorType.BORDER_END;
											break;
										default:
										case "bgColor":
										case "bgStartColor":
											colorType = ColorType.BG_START;
											break;
										case "bgEndColor":
											colorType = ColorType.BG_END;
											break;
									}
									TextColor parsedColor = null;
									if (GsonHelper.isStringValue(definitionObject, colorKey))
									{
										parsedColor = LegendaryTooltipsConfig.getColor(GsonHelper.getAsString(definitionObject, colorKey), null, image, index, colorType);
									}
									else if (GsonHelper.isNumberValue(definitionObject, colorKey))
									{
										parsedColor = LegendaryTooltipsConfig.getColor((Long)GsonHelper.getAsLong(definitionObject, colorKey), null, image, index, colorType);
									}
									if (parsedColor != null)
									{
										colors.put(colorKey, parsedColor);
									}
								}
							}
							
							// Finally, add the fully-parsed definition.
							FrameDefinition definition = new FrameDefinition(image, index,
								() -> colors.get("startColor").getValue(),
								() -> colors.get("endColor").getValue(),
								() -> colors.get("bgStartColor") != null ? colors.get("bgStartColor").getValue() : colors.get("bgColor").getValue(),
								() -> colors.get("bgEndColor") != null ? colors.get("bgEndColor").getValue() : colors.get("bgColor").getValue(),
								FrameSource.DATA, priority);
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
	
}
