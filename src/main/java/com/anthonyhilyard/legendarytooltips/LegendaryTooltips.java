package com.anthonyhilyard.legendarytooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import com.anthonyhilyard.legendarytooltips.util.ItemColor;

import org.apache.commons.lang3.tuple.Pair;

@Mod(modid=Loader.MODID, name=Loader.MODNAME, version=Loader.MODVERSION, acceptedMinecraftVersions = "[1.12.2]")
@EventBusSubscriber(modid = Loader.MODID)
public class LegendaryTooltips
{
	@Instance(Loader.MODID)
	public LegendaryTooltips instance;

	public static final int STANDARD = -1;
	public static final int NUM_FRAMES = 16;

	private static ItemStack lastTooltipItem = null;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		LegendaryTooltipsConfig.loadConfig(event.getSuggestedConfigurationFile());
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
			int rarityColor = ItemColor.getColorForItem(item, 0xFFFFFF);

			float[] hsbVals = new float[3];
			java.awt.Color.RGBtoHSB((rarityColor >> 16) & 0xFF, (rarityColor >> 8) & 0xFF, (rarityColor >> 0) & 0xFF, hsbVals);
			boolean addHue = false;
			if (hsbVals[0] * 360 < 62)
			{
				addHue = false;
			}
			else if (hsbVals[0] * 360 <= 240)
			{
				addHue = true;
			}
			
			int startColor = java.awt.Color.getHSBColor(addHue ? hsbVals[0] - 0.006f : hsbVals[0] + 0.006f, hsbVals[1], hsbVals[2]).getRGB();
			int endColor =java.awt.Color.getHSBColor(addHue ? hsbVals[0] + 0.04f : hsbVals[0] - 0.04f, hsbVals[1], hsbVals[2]).getRGB();

			return Pair.of(startColor & 0xAAFFFFFF, endColor & 0x44FFFFFF);
		}

		return defaults;
	}

	@SubscribeEvent
	@SuppressWarnings("generic")
	public static void onRenderTick(TickEvent.RenderTickEvent event)
	{
		TooltipDecor.updateTimer();

		Minecraft mc = Minecraft.getMinecraft();
		if (mc.currentScreen != null)
		{
			if (mc.currentScreen instanceof GuiContainer)
			{
				if (((GuiContainer)mc.currentScreen).getSlotUnderMouse() != null && 
					((GuiContainer)mc.currentScreen).getSlotUnderMouse().getHasStack())
				{
					if (lastTooltipItem != ((GuiContainer)mc.currentScreen).getSlotUnderMouse().getStack())
					{
						TooltipDecor.resetTimer();
						lastTooltipItem = ((GuiContainer)mc.currentScreen).getSlotUnderMouse().getStack();
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

		event.setBorderStart(borderColors.getLeft());
		event.setBorderEnd(borderColors.getRight());
	}

	@SubscribeEvent
	public static void onPostTooltipEvent(RenderTooltipEvent.PostText event)
	{
		// If tooltip shadows are enabled, draw one now.
		if (LegendaryTooltipsConfig.INSTANCE.tooltipShadow)
		{
			TooltipDecor.drawShadow(event.getX(), event.getY(), event.getWidth(), event.getHeight());
		}

		// If this is a rare item, draw special border.
		TooltipDecor.drawBorder(event.getX(), event.getY(), event.getWidth(), event.getHeight(), event.getStack(), event.getLines(), event.getFontRenderer(), LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(event.getStack()));
	}
}
