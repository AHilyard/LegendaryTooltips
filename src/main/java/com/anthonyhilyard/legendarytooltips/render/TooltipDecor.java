package com.anthonyhilyard.legendarytooltips.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;
import net.minecraftforge.fml.client.gui.GuiUtils;

import java.util.ArrayList;
import java.util.List;

import com.anthonyhilyard.iceberg.util.GuiHelper;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.Loader;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;

import org.lwjgl.opengl.GL11;

public class TooltipDecor
{
	private static final ResourceLocation TEXTURE_TOOLTIP_BORDERS = new ResourceLocation(Loader.MODID, "textures/gui/tooltip_borders.png");
	
	private static int currentTooltipBorderStart = 0;
	private static int currentTooltipBorderEnd = 0;

	private static int shineTimer = 0;

	private static List<ITextProperties> cachedPreWrapLines = new ArrayList<>();

	public static void setCurrentTooltipBorderStart(int color)
	{
		currentTooltipBorderStart = color;
	}

	public static void setCurrentTooltipBorderEnd(int color)
	{
		currentTooltipBorderEnd = color;
	}

	public static void setCachedLines(List<? extends ITextProperties> lines)
	{
		cachedPreWrapLines.clear();
		cachedPreWrapLines.addAll(lines);
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
	
	public static void drawShadow(MatrixStack matrixStack, int x, int y, int width, int height)
	{
		int shadowColor = 0x44000000;
		
		matrixStack.pushPose();
		Matrix4f mat = matrixStack.last().pose();
		GuiUtils.drawGradientRect(mat, 390, x - 1,         y + height + 4, x + width + 4, y + height + 5, shadowColor, shadowColor);
		GuiUtils.drawGradientRect(mat, 390, x + width + 4, y - 1,          x + width + 5, y + height + 5, shadowColor, shadowColor);

		GuiUtils.drawGradientRect(mat, 390, x + width + 3, y + height + 3, x + width + 4, y + height + 4, shadowColor, shadowColor);

		GuiUtils.drawGradientRect(mat, 390, x,             y + height + 5, x + width + 5, y + height + 6, shadowColor, shadowColor);
		GuiUtils.drawGradientRect(mat, 390, x + width + 5, y,              x + width + 6, y + height + 5, shadowColor, shadowColor);
		matrixStack.popPose();
	}

	public static void drawSeparator(MatrixStack matrixStack, int x, int y, int width, int color)
	{
		matrixStack.pushPose();
		Matrix4f mat = matrixStack.last().pose();
		GuiHelper.drawGradientRectHorizontal(mat, 402, x, y, x + width / 2, y + 1, color & 0xFFFFFF, color);
		GuiHelper.drawGradientRectHorizontal(mat, 402, x + width / 2, y, x + width, y + 1, color, color & 0xFFFFFF);
		matrixStack.popPose();
	}

	public static void drawBorder(MatrixStack matrixStack, int x, int y, int width, int height, ItemStack item, List<? extends ITextProperties> lines, FontRenderer font, int frameLevel, boolean comparison)
	{
		// If this is a comparison tooltip, we need to draw the actual border lines first.
		if (comparison)
		{
			matrixStack.pushPose();
			Matrix4f mat = matrixStack.last().pose();

			GuiUtils.drawGradientRect(mat, 400, x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, currentTooltipBorderStart, currentTooltipBorderEnd);
			GuiUtils.drawGradientRect(mat, 400, x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, currentTooltipBorderStart, currentTooltipBorderEnd);
			GuiUtils.drawGradientRect(mat, 400, x - 3, y - 3, x + width + 3, y - 3 + 1, currentTooltipBorderStart, currentTooltipBorderStart);
			GuiUtils.drawGradientRect(mat, 400, x - 3, y + height + 2, x + width + 3, y + height + 3, currentTooltipBorderEnd, currentTooltipBorderEnd);
			matrixStack.popPose();

			// Now draw a separator under the "equipped" badge.
			drawSeparator(matrixStack, x - 3 + 1, y - 3 + 1 + 10, width, currentTooltipBorderStart);
		}

		// If the separate name border is enabled, draw it now.
		if (LegendaryTooltipsConfig.INSTANCE.nameSeparator.get() && !cachedPreWrapLines.isEmpty())
		{
			// Determine and store the number of "title lines".
			ITextProperties textLine = cachedPreWrapLines.get(0);
			List<ITextProperties> wrappedLine = font.getSplitter().splitLines(textLine, width, Style.EMPTY);
			int titleLineCount = wrappedLine.size();

			// Only do this if there's more lines below the title.
			if (cachedPreWrapLines.size() > titleLineCount)
			{
				// If this is a comparison tooltip, we need to move this separator down to the proper position.
				int offset = 0;
				if (comparison)
				{
					offset = 11;
				}

				// Now draw the separator under the title.
				drawSeparator(matrixStack, x - 3 + 1, y - 3 + 1 + (titleLineCount * 10) + 1 + offset, width, currentTooltipBorderStart);
			}
		}

		if (frameLevel >= LegendaryTooltips.NUM_FRAMES || frameLevel < 0)
		{
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		mc.getTextureManager().bind(TEXTURE_TOOLTIP_BORDERS);

		// Grab the width and height of the texture.  This should be 128x128, but old resource packs could still be using 64x64.
		int textureWidth  = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int textureHeight = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

		if (LegendaryTooltipsConfig.INSTANCE.shineEffect.get())
		{
			// Draw shiny effect here.
			matrixStack.pushPose();
			Matrix4f mat = matrixStack.last().pose();

			if (shineTimer >= 10 && shineTimer <= 40)
			{
				float interval = MathHelper.clamp((float)(shineTimer - 10) / 20.0f, 0.0f, 1.0f);
				int alpha = (int)(0x99 * interval) << 24;

				int horizontalMin = x - 3;
				int horizontalMax = x + width + 3;
				int horizontalInterval = (int)MathHelper.lerp(interval * interval, horizontalMax, horizontalMin);
				GuiHelper.drawGradientRectHorizontal(mat, 402, Math.max(horizontalInterval - 36, horizontalMin), y - 3, Math.min(horizontalInterval, horizontalMax), y - 3 + 1, 0x00FFFFFF, 0x00FFFFFF | alpha);
				GuiHelper.drawGradientRectHorizontal(mat, 402, Math.max(horizontalInterval, horizontalMin), y - 3, Math.min(horizontalInterval + 36, horizontalMax), y - 3 + 1, 0x00FFFFFF | alpha, 0x00FFFFFF);
			}

			if (shineTimer <= 20)
			{
				float interval = MathHelper.clamp((float)shineTimer / 20.0f, 0.0f, 1.0f);
				int alpha = (int)(0x55 * interval) << 24;

				int verticalMin = y - 3 + 1;
				int verticalMax = y + height + 3 - 1;
				int verticalInterval = (int)MathHelper.lerp(interval * interval, verticalMax, verticalMin);
				GuiUtils.drawGradientRect(mat, 402, x - 3, Math.max(verticalInterval - 12, verticalMin), x - 3 + 1, Math.min(verticalInterval, verticalMax), 0x00FFFFFF, 0x00FFFFFF | alpha);
				GuiUtils.drawGradientRect(mat, 402, x - 3, Math.max(verticalInterval, verticalMin), x - 3 + 1, Math.min(verticalInterval + 12, verticalMax), 0x00FFFFFF | alpha, 0x00FFFFFF);
			}
			
			matrixStack.popPose();
		}

		// Here we will overlay a 6-patch border over the tooltip to make it look fancy.
		matrixStack.pushPose();
		matrixStack.translate(0, 0, 410.0);

		// Render top-left corner.
		AbstractGui.blit(matrixStack, x - 6, y - 6, (frameLevel / 8) * 64, (frameLevel * 16) % textureHeight, 8, 8, textureWidth, textureHeight);

		// Render top-right corner.
		AbstractGui.blit(matrixStack, x + width - 8 + 6, y - 6, 56 + (frameLevel / 8) * 64, (frameLevel * 16) % textureHeight, 8, 8, textureWidth, textureHeight);

		// Render bottom-left corner.
		AbstractGui.blit(matrixStack, x - 6, y + height - 8 + 6, (frameLevel / 8) * 64, (frameLevel * 16) % textureHeight + 8, 8, 8, textureWidth, textureHeight);

		// Render bottom-right corner.
		AbstractGui.blit(matrixStack, x + width - 8 + 6, y + height - 8 + 6, 56 + (frameLevel / 8) * 64, (frameLevel * 16) % textureHeight + 8, 8, 8, textureWidth, textureHeight);

		// Only render central embellishments if the tooltip is 48 pixels wide or more.
		if (width >= 48)
		{
			// Render top central embellishment.
			AbstractGui.blit(matrixStack, x + (width / 2) - 24, y - 9, 8 + (frameLevel / 8) * 64, (frameLevel * 16) % textureHeight, 48, 8, textureWidth, textureHeight);

			// Render bottom central embellishment.
			AbstractGui.blit(matrixStack, x + (width / 2) - 24, y + height - 8 + 9, 8 + (frameLevel / 8) * 64, (frameLevel * 16) % textureHeight + 8, 48, 8, textureWidth, textureHeight);
		}

		matrixStack.popPose();
	}
}
