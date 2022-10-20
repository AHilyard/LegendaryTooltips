package com.anthonyhilyard.legendarytooltips.render;

import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import com.mojang.math.Matrix4f;
import net.minecraftforge.client.gui.GuiUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.anthonyhilyard.iceberg.util.GuiHelper;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.Loader;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameDefinition;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;

import org.lwjgl.opengl.GL11;

public class TooltipDecor
{
	public static final ResourceLocation DEFAULT_BORDERS = new ResourceLocation(Loader.MODID, "textures/gui/tooltip_borders.png");
	
	private static int currentTooltipBorderStart = 0;
	private static int currentTooltipBorderEnd = 0;

	private static int shineTimer = 0;

	private static Map<Integer, List<Either<FormattedText, TooltipComponent>>> cachedPreWrapLines = new HashMap<>();

	public static void setCurrentTooltipBorderStart(int color)
	{
		currentTooltipBorderStart = color;
	}

	public static void setCurrentTooltipBorderEnd(int color)
	{
		currentTooltipBorderEnd = color;
	}

	public static void setCachedLines(List<Either<FormattedText, TooltipComponent>> lines, int index)
	{
		cachedPreWrapLines.put(index, lines);
	}

	public static void updateTimer()
	{
		if (shineTimer > 0)
		{
			shineTimer--;
		}
	}

	public static void resetTimer()
	{
		shineTimer = 50;
	}
	
	public static void drawShadow(PoseStack poseStack, int x, int y, int width, int height)
	{
		int shadowColor = 0x44000000;
		
		poseStack.pushPose();
		Matrix4f mat = poseStack.last().pose();
		GuiUtils.drawGradientRect(mat, 390, x - 1,         y + height + 4, x + width + 4, y + height + 5, shadowColor, shadowColor);
		GuiUtils.drawGradientRect(mat, 390, x + width + 4, y - 1,          x + width + 5, y + height + 5, shadowColor, shadowColor);

		GuiUtils.drawGradientRect(mat, 390, x + width + 3, y + height + 3, x + width + 4, y + height + 4, shadowColor, shadowColor);

		GuiUtils.drawGradientRect(mat, 390, x,             y + height + 5, x + width + 5, y + height + 6, shadowColor, shadowColor);
		GuiUtils.drawGradientRect(mat, 390, x + width + 5, y,              x + width + 6, y + height + 5, shadowColor, shadowColor);
		poseStack.popPose();
	}

	public static void drawSeparator(PoseStack poseStack, int x, int y, int width, int color)
	{
		poseStack.pushPose();
		Matrix4f mat = poseStack.last().pose();
		GuiHelper.drawGradientRectHorizontal(mat, 402, x, y, x + width / 2, y + 1, color & 0xFFFFFF, color);
		GuiHelper.drawGradientRectHorizontal(mat, 402, x + width / 2, y, x + width, y + 1, color, color & 0xFFFFFF);
		poseStack.popPose();
	}

	public static void drawBorder(PoseStack poseStack, int x, int y, int width, int height, ItemStack item, Font font, FrameDefinition frameDefinition, boolean comparison, int index)
	{
		// If this is a comparison tooltip, we need to draw the actual border lines first.
		if (comparison)
		{
			poseStack.pushPose();
			Matrix4f mat = poseStack.last().pose();

			GuiUtils.drawGradientRect(mat, 400, x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, currentTooltipBorderStart, currentTooltipBorderEnd);
			GuiUtils.drawGradientRect(mat, 400, x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, currentTooltipBorderStart, currentTooltipBorderEnd);
			GuiUtils.drawGradientRect(mat, 400, x - 3, y - 3, x + width + 3, y - 3 + 1, currentTooltipBorderStart, currentTooltipBorderStart);
			GuiUtils.drawGradientRect(mat, 400, x - 3, y + height + 2, x + width + 3, y + height + 3, currentTooltipBorderEnd, currentTooltipBorderEnd);
			poseStack.popPose();

			// Now draw a separator under the "equipped" badge.
			drawSeparator(poseStack, x - 3 + 1, y - 3 + 1 + 10, width, currentTooltipBorderStart);
		}

		// If the separate name border is enabled, draw it now.
		if (LegendaryTooltipsConfig.INSTANCE.nameSeparator.get() && item != null && !item.isEmpty())
		{
			// Determine the number of "title lines".
			FormattedText textLine = null;
			int titleIndex = 0;
			if (cachedPreWrapLines.containsKey(index))
			{
				// Get the first text line from the cached lines.
				for (Either<FormattedText, TooltipComponent> line : cachedPreWrapLines.get(index))
				{
					if (line.left().isPresent())
					{
						textLine = line.left().get();
						break;
					}
					titleIndex++;
				}
			}
			else if (cachedPreWrapLines.containsKey(0))
			{
				index = 0;
				// Get the first text line from the cached lines.
				for (Either<FormattedText, TooltipComponent> line : cachedPreWrapLines.get(0))
				{
					if (line.left().isPresent())
					{
						textLine = line.left().get();
						break;
					}
					titleIndex++;
				}
			}

			if (textLine != null)
			{
				List<FormattedCharSequence> wrappedLine = font.split(textLine, width);
				int titleLineCount = wrappedLine.size();

				// Only do this if there's more lines below the title.
				if (cachedPreWrapLines.get(index).size() > titleLineCount)
				{
					int offset = 0;
					// Calculate the offset, which is the height of all components before the title.
					for (int i = 0; i < titleIndex; i++)
					{
						Either<FormattedText, TooltipComponent> line = cachedPreWrapLines.get(index).get(i);
						if (line.left().isPresent())
						{
							offset += font.lineHeight;
						}
						else if (line.right().isPresent())
						{
							offset += ClientTooltipComponent.create(line.right().get()).getHeight() + 2;
						}
					}

					// If this is a comparison tooltip, we need to move the separator down further to the proper position.
					if (comparison)
					{
						offset = 11;
					}

					// Now draw the separator under the title.
					drawSeparator(poseStack, x - 3 + 1, y - 3 + 1 + (titleLineCount * 10) + 1 + offset, width, currentTooltipBorderStart);
				}
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
			Matrix4f mat = poseStack.last().pose();

			if (shineTimer >= 10 && shineTimer <= 40)
			{
				float interval = Mth.clamp((float)(shineTimer - 10) / 20.0f, 0.0f, 1.0f);
				int alpha = (int)(0x99 * interval) << 24;

				int horizontalMin = x - 3;
				int horizontalMax = x + width + 3;
				int horizontalInterval = (int)Mth.lerp(interval * interval, horizontalMax, horizontalMin);
				GuiHelper.drawGradientRectHorizontal(mat, 402, Math.max(horizontalInterval - 36, horizontalMin), y - 3, Math.min(horizontalInterval, horizontalMax), y - 3 + 1, 0x00FFFFFF, 0x00FFFFFF | alpha);
				GuiHelper.drawGradientRectHorizontal(mat, 402, Math.max(horizontalInterval, horizontalMin), y - 3, Math.min(horizontalInterval + 36, horizontalMax), y - 3 + 1, 0x00FFFFFF | alpha, 0x00FFFFFF);
			}

			if (shineTimer <= 20)
			{
				float interval = Mth.clamp((float)shineTimer / 20.0f, 0.0f, 1.0f);
				int alpha = (int)(0x55 * interval) << 24;

				int verticalMin = y - 3 + 1;
				int verticalMax = y + height + 3 - 1;
				int verticalInterval = (int)Mth.lerp(interval * interval, verticalMax, verticalMin);
				GuiUtils.drawGradientRect(mat, 402, x - 3, Math.max(verticalInterval - 12, verticalMin), x - 3 + 1, Math.min(verticalInterval, verticalMax), 0x00FFFFFF, 0x00FFFFFF | alpha);
				GuiUtils.drawGradientRect(mat, 402, x - 3, Math.max(verticalInterval, verticalMin), x - 3 + 1, Math.min(verticalInterval + 12, verticalMax), 0x00FFFFFF | alpha, 0x00FFFFFF);
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
		GuiComponent.blit(poseStack, x - 6, y - 6, (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight, 8, 8, textureWidth, textureHeight);

		// Render top-right corner.
		GuiComponent.blit(poseStack, x + width - 8 + 6, y - 6, 56 + (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight, 8, 8, textureWidth, textureHeight);

		// Render bottom-left corner.
		GuiComponent.blit(poseStack, x - 6, y + height - 8 + 6, (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight + 8, 8, 8, textureWidth, textureHeight);

		// Render bottom-right corner.
		GuiComponent.blit(poseStack, x + width - 8 + 6, y + height - 8 + 6, 56 + (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight + 8, 8, 8, textureWidth, textureHeight);

		// Only render central embellishments if the tooltip is 48 pixels wide or more.
		if (width >= 48)
		{
			// Render top central embellishment.
			GuiComponent.blit(poseStack, x + (width / 2) - 24, y - 9, 8 + (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight, 48, 8, textureWidth, textureHeight);

			// Render bottom central embellishment.
			GuiComponent.blit(poseStack, x + (width / 2) - 24, y + height - 8 + 9, 8 + (frameIndex / 8) * 64, (frameIndex * 16) % textureHeight + 8, 48, 8, textureWidth, textureHeight);
		}

		poseStack.popPose();

	}
}
