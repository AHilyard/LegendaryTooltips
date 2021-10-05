package com.anthonyhilyard.legendarytooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
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

	private static final int STANDARD = 4;

	private static ItemStack lastTooltipItem = null;

	private static Pair<Integer, Integer> itemFrameColors(ItemStack item, Pair<Integer, Integer> defaults)
	{
		// If we are displaying a custom "legendary" border, use a gold color for borders.
		int frameLevel = LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(item);
		if (frameLevel != STANDARD)
		{
			return Pair.of(LegendaryTooltipsConfig.INSTANCE.getCustomBorderStartColor(frameLevel),
						   LegendaryTooltipsConfig.INSTANCE.getCustomBorderEndColor(frameLevel));
		}
		else if (LegendaryTooltipsConfig.INSTANCE.bordersMatchRarity.get())
		{
			Color rarityColor = ItemColor.getColorForItem(item, Color.fromLegacyFormat(TextFormatting.WHITE));

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
			
			Color startColor = Color.fromRgb(java.awt.Color.getHSBColor(addHue ? hsbVals[0] - 0.006f : hsbVals[0] + 0.006f, hsbVals[1], hsbVals[2]).getRGB());
			Color endColor = Color.fromRgb(java.awt.Color.getHSBColor(addHue ? hsbVals[0] + 0.04f : hsbVals[0] - 0.04f, hsbVals[1], hsbVals[2]).getRGB());

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
			if (mc.screen instanceof ContainerScreen)
			{
				if (((ContainerScreen<?>)mc.screen).getSlotUnderMouse() != null && 
					((ContainerScreen<?>)mc.screen).getSlotUnderMouse().hasItem())
				{
					if (lastTooltipItem != ((ContainerScreen<?>)mc.screen).getSlotUnderMouse().getItem())
					{
						TooltipDecor.resetTimer();
						lastTooltipItem = ((ContainerScreen<?>)mc.screen).getSlotUnderMouse().getItem();
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPreTooltipEvent(RenderTooltipEvent.Pre event)
	{
		TooltipDecor.setCachedLines(event.getLines());
	}

	@SubscribeEvent
	public static void onTooltipColorEvent(RenderTooltipEvent.Color event)
	{
		Pair<Integer, Integer> borderColors = itemFrameColors(event.getStack(), Pair.of(event.getBorderStart(), event.getBorderEnd()));

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
	public static void onPostTooltipEvent(RenderTooltipEvent.PostText event)
	{
		// If tooltip shadows are enabled, draw one now.
		if (LegendaryTooltipsConfig.INSTANCE.tooltipShadow.get())
		{
			TooltipDecor.drawShadow(event.getMatrixStack(), event.getX(), event.getY(), event.getWidth(), event.getHeight());
		}

		boolean comparison = false;
		if (event instanceof RenderTooltipExtEvent.PostText)
		{
			comparison = ((RenderTooltipExtEvent.PostText)event).isComparison();
		}

		// If this is a rare item, draw special border.
		if (comparison)
		{
			TooltipDecor.drawBorder(event.getMatrixStack(), event.getX(), event.getY() - 11, event.getWidth(), event.getHeight() + 11, event.getStack(), event.getLines(), event.getFontRenderer(), LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(event.getStack()), comparison);
		}
		else
		{
			TooltipDecor.drawBorder(event.getMatrixStack(), event.getX(), event.getY(), event.getWidth(), event.getHeight(), event.getStack(), event.getLines(), event.getFontRenderer(), LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(event.getStack()), comparison);
		}
	}
}
