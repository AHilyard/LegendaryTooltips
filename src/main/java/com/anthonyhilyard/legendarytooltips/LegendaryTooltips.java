package com.anthonyhilyard.legendarytooltips;

import java.util.List;

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
import net.minecraft.ChatFormatting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;

import net.minecraftforge.api.ModLoadingContext;
import net.minecraftforge.api.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.config.ModConfig;

import com.anthonyhilyard.iceberg.events.RenderTickEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.GatherResult;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameDefinition;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.FrameSource;
import com.anthonyhilyard.legendarytooltips.render.TooltipDecor;
import com.anthonyhilyard.prism.item.ItemColors;
import com.anthonyhilyard.prism.text.DynamicColor;


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

	private static FrameDefinition getDefinitionColors(ItemStack item, int defaultStartBorder, int defaultEndBorder, int defaultStartBackground, int defaultEndBackground)
	{
		FrameDefinition result = LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(item);

		switch (result.index())
		{
			case NO_BORDER:
			result = new FrameDefinition(result.resource(), result.index(), () -> defaultStartBorder, () -> defaultEndBorder, () -> defaultStartBackground, () -> defaultEndBackground, FrameSource.NONE, 0);
				break;

			case STANDARD:
				// If the "match rarity" option is turned on, calculate some good-looking colors.
				if (LegendaryTooltipsConfig.INSTANCE.bordersMatchRarity.get())
				{
					// First grab the item's name color.
					DynamicColor rarityColor = DynamicColor.fromRgb(ItemColors.getColorForItem(item, TextColor.fromLegacyFormat(ChatFormatting.WHITE)).getValue());

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

					result = new FrameDefinition(result.resource(), result.index(), () -> startColor.getValue(), () -> endColor.getValue(), () -> startBGColor.getValue(), () -> endBGColor.getValue(), FrameSource.NONE, 0);
				}
				break;
		}

		if (result.startBorder() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), () -> defaultStartBorder, result.endBorder(), result.startBackground(), result.endBackground(), FrameSource.NONE, 0);
		}
		if (result.endBorder() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), result.startBorder(), () -> defaultEndBorder, result.startBackground(), result.endBackground(), FrameSource.NONE, 0);
		}
		if (result.startBackground() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), result.startBorder(), result.endBorder(), () -> defaultStartBackground, result.endBackground(), FrameSource.NONE, 0);
		}
		if (result.endBackground() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), result.startBorder(), result.endBorder(), result.startBackground(), () -> defaultEndBackground, FrameSource.NONE, 0);
		}
		return result;
	}

	public static void onRenderTick(float partialTick)
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
					ItemStack item = ((AbstractContainerScreen<?>)client.screen).hoveredSlot.getItem();
					if (lastTooltipItem != item)
					{
						TooltipDecor.resetTimer();
						lastTooltipItem = item;
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
		FrameDefinition frameDefinition = getDefinitionColors(stack, borderStart, borderEnd, backgroundStart, backgroundEnd);

		// Every tooltip will send a color event before a posttext event, so we can store the color here.
		TooltipDecor.setCurrentTooltipBorderStart(frameDefinition.startBorder().get());
		TooltipDecor.setCurrentTooltipBorderEnd(frameDefinition.endBorder().get());

		// If this is a comparison tooltip, we will make the border transparent here so that we can redraw it later.
		if (comparison)
		{
			result = new ColorExtResult(frameDefinition.startBackground().get(), frameDefinition.endBackground().get(), 0, 0);
		}
		else
		{
			result = new ColorExtResult(frameDefinition.startBackground().get(), frameDefinition.endBackground().get(), frameDefinition.startBorder().get(), frameDefinition.endBorder().get());
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
