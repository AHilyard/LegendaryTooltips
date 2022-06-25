package com.anthonyhilyard.legendarytooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.packs.PackType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.ChatFormatting;

import java.util.List;

import com.anthonyhilyard.iceberg.events.RenderTickEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.GatherResult;
import com.anthonyhilyard.iceberg.util.ItemColor;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameDefinition;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameSource;
import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.api.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.config.ModConfig;

public class LegendaryTooltips implements ClientModInitializer
{
	public static final int STANDARD = -1;
	public static final int NO_BORDER = -2;
	public static final int NUM_FRAMES = 16;

	private static ItemStack lastTooltipItem = null;

	@Override
	public void onInitializeClient()
	{
		ModLoadingContext.registerConfig(Loader.MODID, ModConfig.Type.COMMON, LegendaryTooltipsConfig.SPEC);

		// Check for legacy config files to convert now.
		LegacyConfigConverter.convert();

		RenderTooltipEvents.GATHER.register(LegendaryTooltips::onGatherComponentsEvent);
		RenderTooltipEvents.COLOREXT.register(LegendaryTooltips::onTooltipColorEvent);
		RenderTooltipEvents.POSTEXT.register(LegendaryTooltips::onPostTooltipEvent);
		RenderTickEvents.START.register(LegendaryTooltips::onRenderTick);

		ModConfigEvent.RELOADING.register(LegendaryTooltipsConfig::onReload);

		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(FrameResourceParser.INSTANCE);
	}

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

	public static GatherResult onGatherComponentsEvent(ItemStack itemStack, int screenWidth, int screenHeight, List<Either<FormattedText, TooltipComponent>> tooltipElements, int maxWidth, int index)
	{
		if (LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(itemStack).index() != NO_BORDER)
		{
			TooltipDecor.setCachedLines(tooltipElements, index);
		}
		return new GatherResult(InteractionResult.PASS, maxWidth, tooltipElements);
	}

	public static ColorExtResult onTooltipColorEvent(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, Font font, int backgroundStart, int backgroundEnd, int borderStart, int borderEnd, boolean comparison, int index)
	{
		ColorExtResult result;
		FrameDefinition frameDefinition = getDefinitionColors(stack, borderStart, borderEnd, backgroundStart);

		// Every tooltip will send a color event before a posttext event, so we can store the color here.
		TooltipDecor.setCurrentTooltipBorderStart(frameDefinition.startBorder());
		TooltipDecor.setCurrentTooltipBorderEnd(frameDefinition.endBorder());

		// If this is a comparison tooltip, we will make the border transparent here so that we can redraw it later.
		// Either way, set the background color now.
		if (comparison)
		{
			result = new ColorExtResult(frameDefinition.background(), frameDefinition.background(), 0, 0);
		}
		else
		{
			result = new ColorExtResult(frameDefinition.background(), frameDefinition.background(), frameDefinition.startBorder(), frameDefinition.endBorder());
		}

		return result;
	}

	public static void onPostTooltipEvent(ItemStack itemStack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, Font font, int width, int height, boolean comparison, int index)
	{
		if (LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(itemStack).index() == NO_BORDER)
		{
			return;
		}

		// If tooltip shadows are enabled, draw one now.
		if (LegendaryTooltipsConfig.INSTANCE.tooltipShadow.get())
		{
			if (comparison)
			{
				TooltipDecor.drawShadow(poseStack, x, y - 11, width, height + 11);
			}
			else
			{
				TooltipDecor.drawShadow(poseStack, x, y, width, height);
			}
		}

		// If this is a rare item, draw special border.
		if (comparison)
		{
			TooltipDecor.drawBorder(poseStack, x, y - 11, width, height + 11, itemStack, font, LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(itemStack), comparison, index);
		}
		else
		{
			TooltipDecor.drawBorder(poseStack, x, y, width, height, itemStack, font, LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(itemStack), comparison, index);
		}
	}
}
