package com.anthonyhilyard.legendarytooltips.tooltip;

import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.anthonyhilyard.iceberg.util.GuiHelper;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameDefinition;

public class TooltipDecor
{
	public static final Identifier DEFAULT_BORDERS = Identifier.fromNamespaceAndPath(LegendaryTooltips.MODID, "textures/gui/tooltip_borders.png");
	
	static int currentTooltipBorderStart = 0;
	static int currentTooltipBorderEnd = 0;
	static int currentTooltipBackgroundStart = 0;
	static int currentTooltipBackgroundEnd = 0;

	private static float shineTimer = 2.5f;
	private record TextureDimensions(int width, int height) {}
	private static final Map<Identifier, TextureDimensions> textureSizeCache = new HashMap<>();

	public static void clearTextureSizeCache()
	{
		textureSizeCache.clear();
	}

	private static TextureDimensions getTextureDimensions(Identifier resource)
	{
		return textureSizeCache.computeIfAbsent(resource, res -> {
			try
			{
				GpuTexture gpuTexture = Minecraft.getInstance().getTextureManager().getTexture(res).getTexture();
				return new TextureDimensions(gpuTexture.getWidth(0), gpuTexture.getHeight(0));
			}
			catch (Exception e){}

			// Fallback dimensions if anything goes wrong.
			return new TextureDimensions(128, 128);
		});
	}

	public static void setCurrentTooltipBorderStart(int color)
	{
		currentTooltipBorderStart = color;
	}

	public static void setCurrentTooltipBorderEnd(int color)
	{
		currentTooltipBorderEnd = color;
	}

	public static void setCurrentTooltipBackgroundStart(int color)
	{
		currentTooltipBackgroundStart = color;
	}

	public static void setCurrentTooltipBackgroundEnd(int color)
	{
		currentTooltipBackgroundEnd = color;
	}

	public static void updateTimer(float deltaTime)
	{
		if (shineTimer > 0.0f)
		{
			shineTimer -= deltaTime;
		}
	}

	public static void resetTimer()
	{
		shineTimer = 2.5f;
	}

	public static void drawShadow(GuiGraphicsExtractor graphics, int x, int y, int width, int height)
	{
		int shadowColor = 0x44000000;

		graphics.nextStratum();
		GuiHelper.drawGradientRect(graphics, x - 1,         y + height + 4, x + width + 4, y + height + 5, shadowColor, shadowColor);
		GuiHelper.drawGradientRect(graphics, x + width + 4, y - 1,          x + width + 5, y + height + 5, shadowColor, shadowColor);
		GuiHelper.drawGradientRect(graphics, x + width + 3, y + height + 3, x + width + 4, y + height + 4, shadowColor, shadowColor);
		GuiHelper.drawGradientRect(graphics, x,             y + height + 5, x + width + 5, y + height + 6, shadowColor, shadowColor);
		GuiHelper.drawGradientRect(graphics, x + width + 5, y,              x + width + 6, y + height + 5, shadowColor, shadowColor);
	}

	public static void drawSeparator(GuiGraphicsExtractor graphics, int x, int y, int width, int color)
	{
		graphics.nextStratum();

		GuiHelper.drawGradientRectHorizontal(graphics, x, y, x + width / 2, y + 1, color & 0xFFFFFF, color);
		GuiHelper.drawGradientRectHorizontal(graphics, x + width / 2, y, x + width, y + 1, color, color & 0xFFFFFF);
	}

	public static void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height, ItemStack item, List<ClientTooltipComponent> components, Font font, FrameDefinition frameDefinition, boolean comparison, int index)
	{
		graphics.nextStratum();

		// If this is a comparison tooltip, we need to draw a separator under the "equipped" badge.
		if (comparison)
		{
			drawSeparator(graphics, x - 3 + 1, y - 3 + 1 + 10, width, currentTooltipBorderStart);
		}

		// If the separate name border is enabled, draw it now.
		if (LegendaryTooltipsConfig.getInstance().nameSeparator.get() && item != null && !item.isEmpty() && frameDefinition.index() != LegendaryTooltips.NO_BORDER)
		{
			// Determine the number of "title lines".  This will be the number of lines before the first TitleBreakComponent.
			// If for some reason there is no TitleBreakComponent, we'll default to 1.
			// If the TitleBreakComponent is the last component, don't draw the separator.
			int titleLines = Tooltips.calculateTitleLines(components);
			int numComponents = components.size();

			// Count how many components are not text components, except for (titleLines) count.
			for (int i = 0; i < components.size(); i++)
			{
				if (!(components.get(i) instanceof ClientTextTooltip))
				{
					numComponents--;
					if (numComponents == titleLines)
					{
						break;
					}
				}
			}

			if (titleLines < numComponents)
			{
				int offset = 0;
				int titleStart = 0;

				// If we are displaying a model, adjust the offset for it.
				if (LegendaryTooltipsConfig.showModelForItem(item))
				{
					offset += 7;
				}

				// Find the index of the first text component, which is where the actual title will start.
				for (int i = 0; i < components.size(); i++)
				{
					if (components.get(i) instanceof ClientTextTooltip)
					{
						titleStart = i;
						break;
					}
				}

				// Calculate the offset, which is the height of all components before the title plus the height of all title lines.
				for (int i = 0; i < titleStart + titleLines && i < components.size(); i++)
				{
					ClientTooltipComponent component = components.get(i);
					if (component instanceof ClientTextTooltip)
					{
						offset += Math.max(component.getHeight(font), font.lineHeight);
					}
					else
					{
						offset += component.getHeight(font);
						if (i <= titleStart)
						{
							offset += 2;
						}
					}
				}

				// If this is a comparison tooltip, we need to move the separator down further to the proper position.
				if (comparison)
				{
					offset += 11;
				}

				// Now draw the separator under the title.
				drawSeparator(graphics, x - 3 + 1, y - 3 + 2 + offset, width, currentTooltipBorderStart);
			}
		}

		if (frameDefinition.index() == LegendaryTooltips.STANDARD)
		{
			return;
		}

		if (LegendaryTooltipsConfig.getInstance().shineEffect.get())
		{
			// Draw shiny effect here.
			graphics.nextStratum();
			if (shineTimer >= 0.5f && shineTimer <= 2.0f)
			{
				float interval = Mth.clamp(shineTimer - 0.5f, 0.0f, 1.0f);
				int alpha = (int)(0x99 * interval) << 24;

				int horizontalMin = x - 3;
				int horizontalMax = x + width + 3;
				int horizontalInterval = (int)Mth.lerp(interval * interval, horizontalMax, horizontalMin);

				GuiHelper.drawGradientRectHorizontal(graphics, Math.max(horizontalInterval - 36, horizontalMin), y - 3, Math.min(horizontalInterval, horizontalMax), y - 3 + 1, 0x00FFFFFF, 0x00FFFFFF | alpha);
				GuiHelper.drawGradientRectHorizontal(graphics, Math.max(horizontalInterval, horizontalMin), y - 3, Math.min(horizontalInterval + 36, horizontalMax), y - 3 + 1, 0x00FFFFFF | alpha, 0x00FFFFFF);
			}

			if (shineTimer <= 1.0f)
			{
				float interval = Mth.clamp(shineTimer, 0.0f, 1.0f);
				int alpha = (int)(0x55 * interval) << 24;

				int verticalMin = y - 3 + 1;
				int verticalMax = y + height + 3 - 1;
				int verticalInterval = (int)Mth.lerp(interval * interval, verticalMax, verticalMin);

				GuiHelper.drawGradientRect(graphics, x - 3, Math.max(verticalInterval - 12, verticalMin), x - 3 + 1, Math.min(verticalInterval, verticalMax), 0x00FFFFFF, 0x00FFFFFF | alpha);
				GuiHelper.drawGradientRect(graphics, x - 3, Math.max(verticalInterval, verticalMin), x - 3 + 1, Math.min(verticalInterval + 12, verticalMax), 0x00FFFFFF | alpha, 0x00FFFFFF);
			}
		}

		// Grab the width and height of the texture.  This should be 128x128, but old resource packs could still be using 64x64.
		TextureDimensions dims = getTextureDimensions(frameDefinition.resource());
		int textureWidth = dims.width();
		int textureHeight = dims.height();

		final int frameIndex = frameDefinition.index();
		final int frameWidth = frameDefinition.frameWidth();
		final int partSize = frameDefinition.partSize();
		final int partOffset = frameDefinition.partOffset();
		final int cornerOffset = frameDefinition.cornerOffset();
		final int frameHeight = partSize * 2;
		final int partWidth = frameWidth - partSize * 2;

		graphics.nextStratum();

		GuiHelper.blit(graphics, frameDefinition.resource(), x - partSize + cornerOffset, y - partSize + cornerOffset, partSize, partSize, (frameIndex / 8) * frameWidth, (frameIndex * frameHeight) % textureHeight, partSize, partSize, textureWidth, textureHeight);
		GuiHelper.blit(graphics, frameDefinition.resource(), x + width - cornerOffset, y - partSize + cornerOffset, partSize, partSize, (frameWidth - partSize) + (frameIndex / 8) * frameWidth, (frameIndex * frameHeight) % textureHeight, partSize, partSize, textureWidth, textureHeight);
		GuiHelper.blit(graphics, frameDefinition.resource(), x - partSize + cornerOffset, y + height - cornerOffset, partSize, partSize, (frameIndex / 8) * frameWidth, (frameIndex * frameHeight) % textureHeight + partSize, partSize, partSize, textureWidth, textureHeight);
		GuiHelper.blit(graphics, frameDefinition.resource(), x + width - cornerOffset, y + height - cornerOffset, partSize, partSize, (frameWidth - partSize) + (frameIndex / 8) * frameWidth, (frameIndex * frameHeight) % textureHeight + partSize, partSize, partSize, textureWidth, textureHeight);

		// Only render central embellishments if the tooltip is 48 pixels wide or more.
		if (width >= partWidth)
		{
			GuiHelper.blit(graphics, frameDefinition.resource(), x + (width / 2) - (partWidth / 2), y - partSize + partOffset, partWidth, partSize, partSize + (frameIndex / 8) * frameWidth, (frameIndex * frameHeight) % textureHeight, partWidth, partSize, textureWidth, textureHeight);
			GuiHelper.blit(graphics, frameDefinition.resource(), x + (width / 2) - (partWidth / 2), y + height - partOffset, partWidth, partSize, partSize + (frameIndex / 8) * frameWidth, (frameIndex * frameHeight) % textureHeight + partSize, partWidth, partSize, textureWidth, textureHeight);
		}
	}
}