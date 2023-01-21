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
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import com.anthonyhilyard.legendarytooltips.util.ItemColor;

import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Mod(modid=Loader.MODID, name=Loader.MODNAME, version=Loader.MODVERSION, acceptedMinecraftVersions = "[1.12.2]", clientSideOnly = true)
@EventBusSubscriber(modid = Loader.MODID, value = Side.CLIENT)
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

	private static Integer[] itemFrameColors(ItemStack item, Integer[] defaults)
	{
		// If we are displaying a custom "legendary" border, use a gold color for borders.
		int frameLevel = LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(item);
		if (frameLevel != STANDARD)
		{
			int startColor = LegendaryTooltipsConfig.INSTANCE.getCustomBorderStartColor(frameLevel);
			int endColor = LegendaryTooltipsConfig.INSTANCE.getCustomBorderEndColor(frameLevel);
			int bgColor = LegendaryTooltipsConfig.INSTANCE.getCustomBackgroundColor(frameLevel);

			if (startColor == -1)
			{
				startColor = defaults[0];
			}
			if (endColor == -1)
			{
				endColor = defaults[1];
			}
			if (bgColor == -1)
			{
				bgColor = defaults[2];
			}
			return new Integer[] { startColor, endColor, bgColor };
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
			int bgColor = java.awt.Color.getHSBColor(addHue ? hsbVals[0] - 0.0045f : hsbVals[0] + 0.0045f, hsbVals[1] * 0.85f, 16.0f / 255.0f).getRGB();

			return new Integer[] { startColor & 0xDDFFFFFF, endColor & 0xAAFFFFFF, bgColor & 0xF0FFFFFF };
		}

		return defaults;
	}

	@SubscribeEvent
	@SuppressWarnings({"generic", "null"})
	public static void onRenderTick(TickEvent.RenderTickEvent event)
	{
		//Only tick timer once per tick, 2 phases
		if(event.phase == TickEvent.Phase.END) TooltipDecor.updateTimer();

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
				else TooltipDecor.resetTimer();//Required, otherwise mousing over an empty slot or no slot then back does not reset the timer
			}
		}
	}

	@SubscribeEvent
	public static void onPreTooltipEvent(RenderTooltipEvent.Pre event)
	{
		int index = 0;
		if (net.minecraftforge.fml.common.Loader.isModLoaded("equipmentcompare"))
		{
			try
			{
				index = (int)Class.forName("com.anthonyhilyard.legendarytooltips.compat.EquipmentCompareHandler").getMethod("getEventIndex", Event.class).invoke(null, event);
			}
			catch (Exception e)
			{
				Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}

		TooltipDecor.setCachedLines(event.getLines(), index);
	}

	@SubscribeEvent
	public static void onTooltipColorEvent(RenderTooltipEvent.Color event)
	{
		Integer[] borderColors = itemFrameColors(event.getStack(), new Integer[] { event.getBorderStart(), event.getBorderEnd(), event.getBackground() });

		// Every tooltip will send a color event before a posttext event, so we can store the color here.
		TooltipDecor.setCurrentTooltipBorderStart(borderColors[0]);
		TooltipDecor.setCurrentTooltipBorderEnd(borderColors[1]);

		boolean comparison = false;

		if (net.minecraftforge.fml.common.Loader.isModLoaded("equipmentcompare"))
		{
			try
			{
				comparison = (boolean)Class.forName("com.anthonyhilyard.legendarytooltips.compat.EquipmentCompareHandler").getMethod("isComparisonEvent", Event.class).invoke(null, event);
			}
			catch (Exception e)
			{
				Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}

		if (comparison)
		{
			event.setBorderStart(0);
			event.setBorderEnd(0);
		}
		else
		{
			event.setBorderStart(borderColors[0]);
			event.setBorderEnd(borderColors[1]);
		}
		event.setBackground(borderColors[2]);
	}

	@SubscribeEvent
	public static void onPostTooltipEvent(RenderTooltipEvent.PostText event)
	{
		boolean comparison = false;
		int index = 0;

		if (net.minecraftforge.fml.common.Loader.isModLoaded("equipmentcompare"))
		{
			try
			{
				comparison = (boolean)Class.forName("com.anthonyhilyard.legendarytooltips.compat.EquipmentCompareHandler").getMethod("isComparisonEvent", Event.class).invoke(null, event);
				index = (int)Class.forName("com.anthonyhilyard.legendarytooltips.compat.EquipmentCompareHandler").getMethod("getEventIndex", Event.class).invoke(null, event);
			}
			catch (Exception e)
			{
				Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
			}
		}

		// If tooltip shadows are enabled, draw one now.
		if (LegendaryTooltipsConfig.INSTANCE.tooltipShadow)
		{
			if (comparison)
			{
				TooltipDecor.drawShadow(event.getX(), event.getY() - 11, event.getWidth(), event.getHeight() + 11);
			}
			else
			{
				TooltipDecor.drawShadow(event.getX(), event.getY(), event.getWidth(), event.getHeight());
			}
		}

		// If this is a rare item, draw special border.
		if (comparison)
		{
			TooltipDecor.drawBorder(event.getX(), event.getY() - 11, event.getWidth(), event.getHeight() + 11, event.getStack(), event.getLines(), event.getFontRenderer(), LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(event.getStack()), comparison, index);
		}
		else
		{
			TooltipDecor.drawBorder(event.getX(), event.getY(), event.getWidth(), event.getHeight(), event.getStack(), event.getLines(), event.getFontRenderer(), LegendaryTooltipsConfig.INSTANCE.getFrameLevelForItem(event.getStack()), comparison, index);
		}
	}
}
