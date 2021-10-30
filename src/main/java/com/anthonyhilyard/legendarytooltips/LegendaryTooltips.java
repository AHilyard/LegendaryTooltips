package com.anthonyhilyard.legendarytooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextColor;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.ChatFormatting;

import java.util.List;

import com.anthonyhilyard.iceberg.events.RenderTickEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.ColorResult;
import com.anthonyhilyard.iceberg.util.ItemColor;
import com.anthonyhilyard.iceberg.util.StringRecomposer;
import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import com.mojang.blaze3d.vertex.PoseStack;

import org.apache.commons.lang3.tuple.Pair;

public class LegendaryTooltips implements ClientModInitializer
{
	public static final int STANDARD = -1;
	public static final int NUM_FRAMES = 16;

	private static ItemStack lastTooltipItem = null;

	@Override
	public void onInitializeClient()
	{
		LegendaryTooltipsConfig.init();

		RenderTooltipEvents.PRE.register(LegendaryTooltips::onPreTooltipEvent);
		RenderTooltipEvents.COLOR.register(LegendaryTooltips::onTooltipColorEvent);
		RenderTooltipEvents.POST.register(LegendaryTooltips::onPostTooltipEvent);
		RenderTickEvents.START.register(LegendaryTooltips::onRenderTick);
	}

	private static Pair<Integer, Integer> itemFrameColors(ItemStack item, Pair<Integer, Integer> defaults)
	{
		// If we are displaying a custom "legendary" border, use a gold color for borders.
		int frameLevel = LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(item);
		if (frameLevel != STANDARD)
		{
			int startColor = LegendaryTooltipsConfig.INSTANCE.getCustomBorderStartColor(frameLevel);
			int endColor = LegendaryTooltipsConfig.INSTANCE.getCustomBorderEndColor(frameLevel);

			if (startColor == -1)
			{
				startColor = defaults.getLeft();
			}
			if (endColor == -1)
			{
				endColor = defaults.getRight();
			}
			return Pair.of(startColor, endColor);
		}
		else if (LegendaryTooltipsConfig.INSTANCE.bordersMatchRarity)
		{
			TextColor rarityColor = ItemColor.getColorForItem(item, TextColor.fromLegacyFormat(ChatFormatting.WHITE));

			float[] hsbVals = new float[3];
			java.awt.Color.RGBtoHSB((rarityColor.getValue() >> 16) & 0xFF, (rarityColor.getValue() >> 8) & 0xFF, (rarityColor.getValue() >> 0) & 0xFF, hsbVals);
			boolean addHue = false;
			if (hsbVals[0] * 360 < 62)
			{
				addHue = false;
			}
			else if (hsbVals[0] * 360 <= 240)
			{
				addHue = true;
			}
			
			TextColor startColor = TextColor.fromRgb(java.awt.Color.getHSBColor(addHue ? hsbVals[0] - 0.006f : hsbVals[0] + 0.006f, hsbVals[1], hsbVals[2]).getRGB());
			TextColor endColor = TextColor.fromRgb(java.awt.Color.getHSBColor(addHue ? hsbVals[0] + 0.04f : hsbVals[0] - 0.04f, hsbVals[1], hsbVals[2]).getRGB());

			return Pair.of(startColor.getValue() & (0xAAFFFFFF), endColor.getValue() & (0x44FFFFFF));
		}

		return defaults;
	}

	public static void onRenderTick(float timer)
	{
		Minecraft client = Minecraft.getInstance();
		TooltipDecor.updateTimer();

		if (client.screen != null)
		{
			if (client.screen instanceof AbstractContainerScreen)
			{
				if (((AbstractContainerScreen<?>)client.screen).hoveredSlot != null &&
					((AbstractContainerScreen<?>)client.screen).hoveredSlot.hasItem())
				{
					if (lastTooltipItem != ((AbstractContainerScreen<?>)client.screen).hoveredSlot.getItem())
					{
						TooltipDecor.resetTimer();
						lastTooltipItem = ((AbstractContainerScreen<?>)client.screen).hoveredSlot.getItem();
					}
				}
			}
		}
	}

	public static InteractionResult onPreTooltipEvent(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, int screenWidth, int screenHeight, int maxWidth, Font font, boolean comparison)
	{
		List<? extends FormattedText> standinLines = StringRecomposer.recompose(components);
		TooltipDecor.setCachedLines(standinLines);
		return InteractionResult.PASS;
	}

	public static ColorResult onTooltipColorEvent(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, Font font, int background, int borderStart, int borderEnd, boolean comparison)
	{
		ColorResult result;
		Pair<Integer, Integer> borderColors = itemFrameColors(stack, Pair.of(borderStart, borderEnd));

		// Every tooltip will send a color event before a posttext event, so we can store the color here.
		TooltipDecor.setCurrentTooltipBorderStart(borderColors.getLeft());
		TooltipDecor.setCurrentTooltipBorderEnd(borderColors.getRight());

		// If this is a comparison tooltip, we will make the border transparent here so that we can redraw it later.
		if (comparison)
		{
			result = new ColorResult(background, 0, 0);
		}
		else
		{
			result = new ColorResult(background, borderColors.getLeft(), borderColors.getRight());
		}

		return result;
	}

	public static void onPostTooltipEvent(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, Font font, int width, int height, boolean comparison)
	{
		// If tooltip shadows are enabled, draw one now.
		if (LegendaryTooltipsConfig.INSTANCE.tooltipShadow)
		{
			TooltipDecor.drawShadow(poseStack, x, y, width, height);
		}

		// If this is a rare item, draw special border.
		if (comparison)
		{
			TooltipDecor.drawBorder(poseStack, x, y - 11, width, height + 11, stack, components, font, LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(stack), comparison);
		}
		else
		{
			TooltipDecor.drawBorder(poseStack, x, y, width, height, stack, components, font, LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(stack), comparison);
		}
	}
}
