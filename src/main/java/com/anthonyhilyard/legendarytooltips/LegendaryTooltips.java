package com.anthonyhilyard.legendarytooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
//import net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

//import com.anthonyhilyard.iceberg.events.GatherComponentsExtEvent;
import com.anthonyhilyard.iceberg.events.RenderTooltipExtEvent;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameDefinition;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameSource;
import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import com.anthonyhilyard.prism.item.ItemColors;
import com.anthonyhilyard.prism.text.DynamicColor;

@Mod.EventBusSubscriber(modid = Loader.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public class LegendaryTooltips
{
	public static final int STANDARD = -1;
	public static final int NO_BORDER = -2;
	public static final int NUM_FRAMES = 16;

	private static ItemStack lastTooltipItem = null;

	private static FrameDefinition getDefinitionColors(ItemStack item, int defaultStartBorder, int defaultEndBorder, int defaultStartBackground, int defaultEndBackground)
	{
		FrameDefinition result = LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(item);

		switch (result.index)
		{
			case NO_BORDER:
			result = new FrameDefinition(result.resource, result.index, () -> defaultStartBorder, () -> defaultEndBorder, () -> defaultStartBackground, () -> defaultEndBackground, FrameSource.NONE, 0);
				break;

			case STANDARD:
				// If the "match rarity" option is turned on, calculate some good-looking colors.
				if (LegendaryTooltipsConfig.INSTANCE.bordersMatchRarity.get())
				{
					// First grab the item's name color.
					DynamicColor rarityColor = DynamicColor.fromRgb(ItemColors.getColorForItem(item, Color.fromLegacyFormat(TextFormatting.WHITE)).getValue());

					int hue = rarityColor.hue();
					boolean addHue = false;

					// These hue ranges are arbitrarily decided.  I just think they look the best.
					if (hue >= 62 && hue <= 240)
					{
						addHue = true;
					}

					// The start color will hue-shift by 0.6%, and the end will hue-shift the opposite direction by 4%.
					// This gives a very nice looking gradient, while still matching the name color quite well.
					int startHue = addHue ? hue - 4 : hue + 4;
					int endHue = addHue ? hue + 18 : hue - 18;
					int startBGHue = addHue ? hue - 3 : hue + 3;
					int endBGHue = addHue ? hue + 13 : hue - 13;

					// Ensure values stay between 0 and 360.
					startHue = (startHue + 360) % 360;
					endHue = (endHue + 360) % 360;
					startBGHue = (startBGHue + 360) % 360;
					endBGHue = (endBGHue + 360) % 360;

					DynamicColor startColor = DynamicColor.fromAHSV(0xFF, startHue, rarityColor.saturation(), rarityColor.value());
					DynamicColor endColor = DynamicColor.fromAHSV(0xFF, endHue, rarityColor.saturation(), (int)(rarityColor.value() * 0.95f));
					DynamicColor startBGColor = DynamicColor.fromAHSV(0xE4, startBGHue, (int)(rarityColor.saturation() * 0.9f), 14);
					DynamicColor endBGColor = DynamicColor.fromAHSV(0xFD, endBGHue, (int)(rarityColor.saturation() * 0.8f), 18);

					result = new FrameDefinition(result.resource, result.index, () -> startColor.getValue(), () -> endColor.getValue(), () -> startBGColor.getValue(), () -> endBGColor.getValue(), FrameSource.NONE, 0);
				}
				break;
		}

		if (result.startBorder == null)
		{
			result = new FrameDefinition(result.resource, result.index, () -> defaultStartBorder, result.endBorder, result.startBackground, result.endBackground, FrameSource.NONE, 0);
		}
		if (result.endBorder == null)
		{
			result = new FrameDefinition(result.resource, result.index, result.startBorder, () -> defaultEndBorder, result.startBackground, result.endBackground, FrameSource.NONE, 0);
		}
		if (result.startBackground == null)
		{
			result = new FrameDefinition(result.resource, result.index, result.startBorder, result.endBorder, () -> defaultStartBackground, result.endBackground, FrameSource.NONE, 0);
		}
		if (result.endBackground == null)
		{
			result = new FrameDefinition(result.resource, result.index, result.startBorder, result.endBorder, result.startBackground, () -> defaultEndBackground, FrameSource.NONE, 0);
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
		if (LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(event.getStack()).index != NO_BORDER)
		{
			int index = 0;
			if (event instanceof RenderTooltipExtEvent.Pre)
			{
				index = ((RenderTooltipExtEvent.Pre)event).getIndex();
			}
			TooltipDecor.setCachedLines(event.getLines(), index);
		}
	}

	@SubscribeEvent
	public static void onTooltipColorEvent(RenderTooltipEvent.Color event)
	{
		FrameDefinition frameDefinition = getDefinitionColors(event.getStack(), event.getBorderStart(), event.getBorderEnd(), event.getBackground(), event.getBackground());

		// Every tooltip will send a color event before a posttext event, so we can store the color here.
		TooltipDecor.setCurrentTooltipBorderStart(frameDefinition.startBorder.get());
		TooltipDecor.setCurrentTooltipBorderEnd(frameDefinition.endBorder.get());

		// If this is a comparison tooltip, we will make the border transparent here so that we can redraw it later.
		boolean comparison = false;
		if (event instanceof RenderTooltipExtEvent.Color)
		{
			RenderTooltipExtEvent.Color extEvent = (RenderTooltipExtEvent.Color)event;
			comparison = ((RenderTooltipExtEvent.Color)event).isComparison();
			extEvent.setBackgroundStart(frameDefinition.startBackground.get());
			extEvent.setBackgroundEnd(frameDefinition.endBackground.get());
		}
		else
		{
			event.setBackground(frameDefinition.startBackground.get());
		}

		if (comparison)
		{
			event.setBorderStart(0);
			event.setBorderEnd(0);
		}
		else
		{
			event.setBorderStart(frameDefinition.startBorder.get());
			event.setBorderEnd(frameDefinition.endBorder.get());
		}
	}

	@SubscribeEvent
	public static void onPostTooltipEvent(RenderTooltipEvent.PostText event)
	{
		if (LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(event.getStack()).index == NO_BORDER)
		{
			return;
		}

		boolean comparison = false;
		int index = 0;
		if (event instanceof RenderTooltipExtEvent.PostText)
		{
			comparison = ((RenderTooltipExtEvent.PostText)event).isComparison();
			index = ((RenderTooltipExtEvent.PostText)event).getIndex();
		}

		// If tooltip shadows are enabled, draw one now.
		if (LegendaryTooltipsConfig.INSTANCE.tooltipShadow.get())
		{
			if (comparison)
			{
				TooltipDecor.drawShadow(event.getMatrixStack(), event.getX(), event.getY() - 11, event.getWidth(), event.getHeight() + 11);
			}
			else
			{
				TooltipDecor.drawShadow(event.getMatrixStack(), event.getX(), event.getY(), event.getWidth(), event.getHeight());
			}
		}

		// If this is a rare item, draw special border.
		if (comparison)
		{
			TooltipDecor.drawBorder(event.getMatrixStack(), event.getX(), event.getY() - 11, event.getWidth(), event.getHeight() + 11, event.getStack(), event.getLines(), event.getFontRenderer(), LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(event.getStack()), comparison, index);
		}
		else
		{
			TooltipDecor.drawBorder(event.getMatrixStack(), event.getX(), event.getY(), event.getWidth(), event.getHeight(), event.getStack(), event.getLines(), event.getFontRenderer(), LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(event.getStack()), comparison, index);
		}

		// if (LegendaryTooltipsConfig.INSTANCE.compatibilityMode.get())
		// {
		// 	event.getMatrixStack().translate(0, 0, 500);
		// }
	}
}