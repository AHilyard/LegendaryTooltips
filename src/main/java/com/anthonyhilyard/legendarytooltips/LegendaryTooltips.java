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

import com.anthonyhilyard.iceberg.events.RenderTooltipExtEvent;
import com.anthonyhilyard.iceberg.util.ItemColor;
import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = Loader.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public class LegendaryTooltips
{
	@SuppressWarnings("unused")
	public static final Logger LOGGER = LogManager.getLogger();

	public static final int STANDARD = -1;
	public static final int NUM_FRAMES = 16;

	private static ItemStack lastTooltipItem = null;

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
		else if (LegendaryTooltipsConfig.INSTANCE.bordersMatchRarity.get())
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
		TooltipDecor.setCachedLines(event.getTooltipElements());
	}

	@SubscribeEvent
	public static void onTooltipColorEvent(RenderTooltipEvent.Color event)
	{
		Pair<Integer, Integer> borderColors = itemFrameColors(event.getItemStack(), Pair.of(event.getBorderStart(), event.getBorderEnd()));

		// Every tooltip will send a color event before a posttext event, so we can store the color here.
		TooltipDecor.setCurrentTooltipBorderStart(borderColors.getLeft());
		TooltipDecor.setCurrentTooltipBorderEnd(borderColors.getRight());

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
			event.setBorderStart(borderColors.getLeft());
			event.setBorderEnd(borderColors.getRight());
		}
	}

	@SubscribeEvent
	public static void onPostTooltipEvent(RenderTooltipExtEvent.Post event)
	{
		// If tooltip shadows are enabled, draw one now.
		if (LegendaryTooltipsConfig.INSTANCE.tooltipShadow.get())
		{
			TooltipDecor.drawShadow(event.getPoseStack(), event.getX(), event.getY(), event.getWidth(), event.getHeight());
		}

		// If this is a rare item, draw special border.
		if (event.isComparison())
		{
			TooltipDecor.drawBorder(event.getPoseStack(), event.getX(), event.getY() - 11, event.getWidth(), event.getHeight() + 11, event.getItemStack(), event.getFont(), LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(event.getItemStack()), event.isComparison());
		}
		else
		{
			TooltipDecor.drawBorder(event.getPoseStack(), event.getX(), event.getY(), event.getWidth(), event.getHeight(), event.getItemStack(), event.getFont(), LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(event.getItemStack()), event.isComparison());
		}
	}
}
