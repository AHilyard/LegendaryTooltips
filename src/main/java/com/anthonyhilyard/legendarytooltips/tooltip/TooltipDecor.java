package com.anthonyhilyard.legendarytooltips.tooltip;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import org.joml.Matrix4f;

import java.util.List;

import com.anthonyhilyard.iceberg.util.GuiHelper;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;
import com.anthonyhilyard.legendarytooltips.Loader;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameDefinition;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import org.lwjgl.opengl.GL11;

public class TooltipDecor
{
	public static final ResourceLocation DEFAULT_BORDERS = new ResourceLocation(Loader.MODID, "textures/gui/tooltip_borders.png");
	
	static int currentTooltipBorderStart = 0;
	static int currentTooltipBorderEnd = 0;
	static int currentTooltipBackgroundStart = 0;
	static int currentTooltipBackgroundEnd = 0;

	private static float shineTimer = 2.5f;

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
	
	public static void drawShadow(PoseStack poseStack, int x, int y, int width, int height)
	{
		int shadowColor = 0x44000000;
		
		poseStack.pushPose();
		Matrix4f matrix = poseStack.last().pose();
		GuiHelper.drawGradientRect(matrix, 390, x - 1,         y + height + 4, x + width + 4, y + height + 5, shadowColor, shadowColor);
		GuiHelper.drawGradientRect(matrix, 390, x + width + 4, y - 1,          x + width + 5, y + height + 5, shadowColor, shadowColor);

		GuiHelper.drawGradientRect(matrix, 390, x + width + 3, y + height + 3, x + width + 4, y + height + 4, shadowColor, shadowColor);

		GuiHelper.drawGradientRect(matrix, 390, x,             y + height + 5, x + width + 5, y + height + 6, shadowColor, shadowColor);
		GuiHelper.drawGradientRect(matrix, 390, x + width + 5, y,              x + width + 6, y + height + 5, shadowColor, shadowColor);
		poseStack.popPose();
	}

	public static void drawSeparator(PoseStack poseStack, int x, int y, int width, int color)
	{
		poseStack.pushPose();
		Matrix4f matrix = poseStack.last().pose();
		GuiHelper.drawGradientRectHorizontal(matrix, 402, x, y, x + width / 2, y + 1, color & 0xFFFFFF, color);
		GuiHelper.drawGradientRectHorizontal(matrix, 402, x + width / 2, y, x + width, y + 1, color, color & 0xFFFFFF);
		poseStack.popPose();
	}

	public static void drawBorder(PoseStack poseStack, int x, int y, int width, int height, ItemStack item, List<ClientTooltipComponent> components, Font font, FrameDefinition frameDefinition, boolean comparison, int index)
	{
		// If this is a comparison tooltip, we need to draw the actual border lines first.
		if (comparison)
		{
			poseStack.pushPose();
			Matrix4f matrix = poseStack.last().pose();
			GuiHelper.drawGradientRect(matrix, 400, x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, currentTooltipBorderStart, currentTooltipBorderEnd);
			GuiHelper.drawGradientRect(matrix, 400, x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, currentTooltipBorderStart, currentTooltipBorderEnd);
			GuiHelper.drawGradientRect(matrix, 400, x - 3, y - 3, x + width + 3, y - 3 + 1, currentTooltipBorderStart, currentTooltipBorderStart);
			GuiHelper.drawGradientRect(matrix, 400, x - 3, y + height + 2, x + width + 3, y + height + 3, currentTooltipBorderEnd, currentTooltipBorderEnd);
			poseStack.popPose();

			// Now draw a separator under the "equipped" badge.
			drawSeparator(poseStack, x - 3 + 1, y - 3 + 1 + 10, width, currentTooltipBorderStart);
		}

		// If the separate name border is enabled, draw it now.
		if (LegendaryTooltipsConfig.INSTANCE.nameSeparator.get() && item != null && !item.isEmpty() && frameDefinition.index() != LegendaryTooltips.NO_BORDER)
		{
			// Determine the number of "title lines".  This will be the number of lines before the first TitleBreakComponent.
			// If for some reason there is no TitleBreakComponent, we'll default to 1.
			// If the TitleBreakComponent is the last component, don't draw the separator.
			int titleLines = Tooltips.calculateTitleLines(components);

			int numComponents = components.size() - 1;
			if (LegendaryTooltipsConfig.showModelForItem(item))
			{
				titleLines--;
				numComponents -= 2;
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
						offset += Math.max(component.getHeight(), font.lineHeight);
					}
					else
					{
						offset += component.getHeight();
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
				drawSeparator(poseStack, x - 3 + 1, y - 3 + 2 + offset, width, currentTooltipBorderStart);
			}
		}

		if (frameDefinition.index() == LegendaryTooltips.STANDARD)
		{
			return;
		}

		if (LegendaryTooltipsConfig.INSTANCE.shineEffect.get())
		{
			// Draw shiny effect here.
			poseStack.pushPose();
			Matrix4f matrix = poseStack.last().pose();

			if (shineTimer >= 0.5f && shineTimer <= 2.0f)
			{
				float interval = Mth.clamp(shineTimer - 0.5f, 0.0f, 1.0f);
				int alpha = (int)(0x99 * interval) << 24;

				int horizontalMin = x - 3;
				int horizontalMax = x + width + 3;
				int horizontalInterval = (int)Mth.lerp(interval * interval, horizontalMax, horizontalMin);
				GuiHelper.drawGradientRectHorizontal(matrix, 402, Math.max(horizontalInterval - 36, horizontalMin), y - 3, Math.min(horizontalInterval, horizontalMax), y - 3 + 1, 0x00FFFFFF, 0x00FFFFFF | alpha);
				GuiHelper.drawGradientRectHorizontal(matrix, 402, Math.max(horizontalInterval, horizontalMin), y - 3, Math.min(horizontalInterval + 36, horizontalMax), y - 3 + 1, 0x00FFFFFF | alpha, 0x00FFFFFF);
			}

			if (shineTimer <= 1.0f)
			{
				float interval = Mth.clamp(shineTimer, 0.0f, 1.0f);
				int alpha = (int)(0x55 * interval) << 24;

				int verticalMin = y - 3 + 1;
				int verticalMax = y + height + 3 - 1;
				int verticalInterval = (int)Mth.lerp(interval * interval, verticalMax, verticalMin);
				GuiHelper.drawGradientRect(matrix, 402, x - 3, Math.max(verticalInterval - 12, verticalMin), x - 3 + 1, Math.min(verticalInterval, verticalMax), 0x00FFFFFF, 0x00FFFFFF | alpha);
				GuiHelper.drawGradientRect(matrix, 402, x - 3, Math.max(verticalInterval, verticalMin), x - 3 + 1, Math.min(verticalInterval + 12, verticalMax), 0x00FFFFFF | alpha, 0x00FFFFFF);
			}
			
			poseStack.popPose();
		}

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.setShaderTexture(0, frameDefinition.resource());

		// We have to bind the texture to be able to query it, so do that.
		final Minecraft minecraft = Minecraft.getInstance();
		AbstractTexture borderTexture = minecraft.getTextureManager().getTexture(frameDefinition.resource());
		borderTexture.bind();

		// Grab the width and height of the texture.  This should be 128x128, but old resource packs could still be using 64x64.
		int textureWidth = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int textureHeight = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

		final int frameIndex = frameDefinition.index();

		// Here we will overlay a 6-patch border over the tooltip to make it look fancy.
		poseStack.pushPose();
		poseStack.translate(0, 0, 410.0);

		// Render top-left corner.
		GuiHelper.blit(poseStack, x - 6, y - 6, 8, 8, (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight, 8, 8, textureWidth, textureHeight);

		// Render top-right corner.
		GuiHelper.blit(poseStack, x + width - 8 + 6, y - 6, 8, 8, 56 + (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight, 8, 8, textureWidth, textureHeight);

		// Render bottom-left corner.
		GuiHelper.blit(poseStack, x - 6, y + height - 8 + 6, 8, 8, (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight + 8, 8, 8, textureWidth, textureHeight);

		// Render bottom-right corner.
		GuiHelper.blit(poseStack, x + width - 8 + 6, y + height - 8 + 6, 8, 8, 56 + (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight + 8, 8, 8, textureWidth, textureHeight);

		// Only render central embellishments if the tooltip is 48 pixels wide or more.
		if (width >= 48)
		{
			// Render top central embellishment.
			GuiHelper.blit(poseStack, x + (width / 2) - 24, y - 9, 48, 8, 8 + (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight, 48, 8, textureWidth, textureHeight);

			// Render bottom central embellishment.
			GuiHelper.blit(poseStack, x + (width / 2) - 24, y + height - 8 + 9, 48, 8, 8 + (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight + 8, 48, 8, textureWidth, textureHeight);
		}

		poseStack.popPose();

	}
}
