package com.anthonyhilyard.legendarytooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

import com.anthonyhilyard.iceberg.events.GatherComponentsExtEvent;
import com.anthonyhilyard.iceberg.events.RenderTooltipExtEvent;
import com.anthonyhilyard.iceberg.util.ItemColor;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameDefinition;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameSource;
import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;

@Mod.EventBusSubscriber(modid = Loader.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public class LegendaryTooltips
{
	public static final int STANDARD = -1;
	public static final int NO_BORDER = -2;
	public static final int NUM_FRAMES = 16;

	private static ItemStack lastTooltipItem = null;

	private static FrameDefinition getDefinitionColors(ItemStack item, int defaultStartBorder, int defaultEndBorder, int defaultBackground)
	{
		FrameDefinition result = LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(item);

		switch (result.index())
		{
			case NO_BORDER:
				result = new FrameDefinition(result.resource(), result.index(), defaultStartBorder, defaultEndBorder, defaultBackground, FrameSource.NONE, 0);
				break;

			case STANDARD:
				// If the "match rarity" option is turned on, calculate some good-looking colors.
				if (LegendaryTooltipsConfig.INSTANCE.bordersMatchRarity.get())
				{
					// First grab the item's name color.
					TextColor rarityColor = ItemColor.getColorForItem(item, TextColor.fromLegacyFormat(ChatFormatting.WHITE));
		
					// Convert the color from RGB to HSB for easier manipulation.
					float[] hsbVals = new float[3];
					java.awt.Color.RGBtoHSB((rarityColor.getValue() >> 16) & 0xFF, (rarityColor.getValue() >> 8) & 0xFF, (rarityColor.getValue() >> 0) & 0xFF, hsbVals);
					boolean addHue = false;
		
					// These hue ranges are arbitrarily decided.  I just think they look the best.
					if (hsbVals[0] * 360 < 62)
					{
						addHue = false;
					}
					else if (hsbVals[0] * 360 <= 240)
					{
						addHue = true;
					}
					
					// The start color will hue-shift by 0.6%, and the end will hue-shift the opposite direction by 4%.
					// This gives a very nice looking gradient, while still matching the name color quite well.
					float startHue = addHue ? hsbVals[0] - 0.006f : hsbVals[0] + 0.006f;
					float endHue = addHue ? hsbVals[0] + 0.04f : hsbVals[0] - 0.04f;
					
					// Ensure values stay between 0 and 1.
					startHue = (startHue + 1.0f) % 1.0f;
					endHue = (endHue + 1.0f) % 1.0f;
		
					TextColor startColor = TextColor.fromRgb(java.awt.Color.getHSBColor(startHue, hsbVals[1], hsbVals[2]).getRGB());
					TextColor endColor = TextColor.fromRgb(java.awt.Color.getHSBColor(endHue, hsbVals[1], hsbVals[2]).getRGB());
					TextColor backgroundColor = TextColor.fromRgb(java.awt.Color.getHSBColor(hsbVals[0], hsbVals[1] * 0.9f, 0.06f).getRGB());
		
					result = new FrameDefinition(result.resource(), result.index(), startColor.getValue() & (0xAAFFFFFF), endColor.getValue() & (0x44FFFFFF), backgroundColor.getValue() & (0xF0FFFFFF), FrameSource.NONE, 0);
				}
				break;
		}

		if (result.startBorder() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), defaultStartBorder, result.endBorder(), result.background(), FrameSource.NONE, 0);
		}
		if (result.endBorder() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), result.startBorder(), defaultEndBorder, result.background(), FrameSource.NONE, 0);
		}
		if (result.background() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), result.startBorder(), result.endBorder(), defaultBackground, FrameSource.NONE, 0);
		}

		return result;
	}

	@SubscribeEvent
	@SuppressWarnings("generic")
	public static void onRenderTick(TickEvent.RenderTickEvent event)
	{
		TooltipDecor.updateTimer();

		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null)
		{
			if (mc.screen instanceof AbstractContainerScreen)
			{
				if (((AbstractContainerScreen<?>)mc.screen).getSlotUnderMouse() != null && 
					((AbstractContainerScreen<?>)mc.screen).getSlotUnderMouse().hasItem())
				{
					if (lastTooltipItem != ((AbstractContainerScreen<?>)mc.screen).getSlotUnderMouse().getItem())
					{
						TooltipDecor.resetTimer();
						lastTooltipItem = ((AbstractContainerScreen<?>)mc.screen).getSlotUnderMouse().getItem();
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onGatherComponentsEvent(GatherComponents event)
	{
		if (LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(event.getItemStack()).index() != NO_BORDER)
		{
			int index = 0;
			if (event instanceof GatherComponentsExtEvent)
			{
				index = ((GatherComponentsExtEvent)event).getIndex();
			}
			TooltipDecor.setCachedLines(event.getTooltipElements(), index);
		}
	}

	@SubscribeEvent
	public static void onTooltipColorEvent(RenderTooltipEvent.Color event)
	{
		FrameDefinition frameDefinition = getDefinitionColors(event.getItemStack(), event.getBorderStart(), event.getBorderEnd(), event.getBackgroundStart());

		// Every tooltip will send a color event before a posttext event, so we can store the color here.
		TooltipDecor.setCurrentTooltipBorderStart(frameDefinition.startBorder());
		TooltipDecor.setCurrentTooltipBorderEnd(frameDefinition.endBorder());

		// If this is a comparison tooltip, we will make the border transparent here so that we can redraw it later.
		boolean comparison = false;
		if (event instanceof RenderTooltipExtEvent.Color)
		{
			comparison = ((RenderTooltipExtEvent.Color)event).isComparison();
		}

		if (comparison)
		{
			event.setBorderStart(0);
			event.setBorderEnd(0);
		}
		else
		{
			event.setBorderStart(frameDefinition.startBorder());
			event.setBorderEnd(frameDefinition.endBorder());
		}

		// Either way, set the background color now.
		event.setBackground(frameDefinition.background());
	}

	@SubscribeEvent
	public static void onPostTooltipEvent(RenderTooltipExtEvent.Post event)
	{
		if (LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(event.getItemStack()).index() == NO_BORDER)
		{
			return;
		}

		// If tooltip shadows are enabled, draw one now.
		if (LegendaryTooltipsConfig.INSTANCE.tooltipShadow.get())
		{
			if (event.isComparison())
			{
				TooltipDecor.drawShadow(event.getPoseStack(), event.getX(), event.getY() - 11, event.getWidth(), event.getHeight() + 11);
			}
			else
			{
				TooltipDecor.drawShadow(event.getPoseStack(), event.getX(), event.getY(), event.getWidth(), event.getHeight());
			}
		}

		// If this is a rare item, draw special border.
		if (event.isComparison())
		{
			TooltipDecor.drawBorder(event.getPoseStack(), event.getX(), event.getY() - 11, event.getWidth(), event.getHeight() + 11, event.getItemStack(), event.getFont(), LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(event.getItemStack()), event.isComparison(), event.getIndex());
		}
		else
		{
			TooltipDecor.drawBorder(event.getPoseStack(), event.getX(), event.getY(), event.getWidth(), event.getHeight(), event.getItemStack(), event.getFont(), LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(event.getItemStack()), event.isComparison(), event.getIndex());
		}
	}
}
