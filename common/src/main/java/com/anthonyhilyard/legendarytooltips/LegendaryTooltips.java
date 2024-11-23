package com.anthonyhilyard.legendarytooltips;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.ChatFormatting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameDefinition;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameSource;
import com.anthonyhilyard.legendarytooltips.tooltip.TooltipDecor;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.GatherResult;
import com.anthonyhilyard.iceberg.util.Tooltips.TitleBreakComponent;
import com.anthonyhilyard.legendarytooltips.tooltip.ItemModelComponent;
import com.anthonyhilyard.legendarytooltips.tooltip.PaddingComponent;
import com.anthonyhilyard.prism.item.ItemColors;
import com.anthonyhilyard.prism.text.DynamicColor;


public class LegendaryTooltips
{
	public static final String MODID = "legendarytooltips";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static final int STANDARD = -1;
	public static final int NO_BORDER = -2;
	public static final int NUM_FRAMES = 16;

	private static ItemStack lastTooltipItem = null;

	public static void init()
	{
		LegendaryTooltipsConfig.register(LegendaryTooltipsConfig.class, MODID);
	}

	public static FrameDefinition getDefinitionColors(ItemStack item, int defaultStartBorder, int defaultEndBorder, int defaultStartBackground, int defaultEndBackground, HolderLookup.Provider provider)
	{
		FrameDefinition result = LegendaryTooltipsConfig.getInstance().getFrameDefinition(item, provider);

		switch (result.index())
		{
			case NO_BORDER:
				result = new FrameDefinition(result.resource(), result.index(), () -> defaultStartBorder, () -> defaultEndBorder, () -> defaultStartBackground, () -> defaultEndBackground, FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
				break;

			case STANDARD:
				// If the "match rarity" option is turned on, calculate some good-looking colors.
				if (LegendaryTooltipsConfig.getInstance().bordersMatchRarity.get())
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

					result = new FrameDefinition(result.resource(), result.index(), () -> startColor.getIntValue(), () -> endColor.getIntValue(), () -> startBGColor.getIntValue(), () -> endBGColor.getIntValue(), FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
				}
				break;
		}

		if (result.startBorder() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), () -> defaultStartBorder, result.endBorder(), result.startBackground(), result.endBackground(), FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
		}
		if (result.endBorder() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), result.startBorder(), () -> defaultEndBorder, result.startBackground(), result.endBackground(), FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
		}
		if (result.startBackground() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), result.startBorder(), result.endBorder(), () -> defaultStartBackground, result.endBackground(), FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
		}
		if (result.endBackground() == null)
		{
			result = new FrameDefinition(result.resource(), result.index(), result.startBorder(), result.endBorder(), result.startBackground(), () -> defaultEndBackground, FrameSource.NONE, 0, result.frameWidth(), result.partSize(), result.partOffset(), result.cornerOffset());
		}
		return result;
	}

	public static GatherResult onGatherComponentsEvent(ItemStack itemStack, int screenWidth, int screenHeight, List<Either<FormattedText, TooltipComponent>> tooltipElements, int maxWidth, int index)
	{
		// If compact tooltips are turned on, remove a few unneeded lines from the tooltip.
		if (LegendaryTooltipsConfig.getInstance().compactTooltips.get())
		{
			// Search for any translatable components with translation keys that start with "item.modifiers." for removal.
			for (int i = 0; i < tooltipElements.size(); i++)
			{
				if (tooltipElements.get(i).left().isPresent())
				{
					FormattedText text = tooltipElements.get(i).left().get();
					if (text instanceof MutableComponent component && component.getContents() instanceof TranslatableContents contents)
					{
						// If we find a translatable component with a translation key that starts with "item.modifiers.", remove it and the blank line before it.
						if (contents.getKey().startsWith("item.modifiers."))
						{
							tooltipElements.remove(i);

							if (tooltipElements.size() > i - 1 && i > 0 &&
								(tooltipElements.get(i - 1).right().isPresent() && tooltipElements.get(i - 1).right().get() == CommonComponents.EMPTY) ||
								(tooltipElements.get(i - 1).left().isPresent()  && tooltipElements.get(i - 1).left().get().getString().isEmpty()))
							{
								tooltipElements.remove(i - 1);
							}
							break;
						}
					}
				}
			}
		}

		if (LegendaryTooltipsConfig.showModelForItem(itemStack))
		{
			// Alter the title by adding enough space to the beginning to make room for the item model.
			if (!tooltipElements.isEmpty() && tooltipElements.get(0).left().isPresent())
			{
				FormattedText title = LegendaryTooltipsConfig.getFormattedTitle(tooltipElements.get(0).left().get());

				if (title != null)
				{
					tooltipElements.set(0, Either.<FormattedText, TooltipComponent>left(title));

					// Insert an item model component before the title, and an empty line after it.
					tooltipElements.add(0, Either.<FormattedText, TooltipComponent>right(new ItemModelComponent(itemStack)));

					// If the only components at this point are the model and the title, we only need to add half a line of spacing.
					if (tooltipElements.stream().filter(x -> !(x.right().isPresent() && x.right().get() instanceof TitleBreakComponent)).count() == 2)
					{
						tooltipElements.add(2, Either.<FormattedText, TooltipComponent>right(new PaddingComponent(6)));
					}
					// Otherwise, we'll add a full line.
					else
					{
						tooltipElements.add(2, Either.<FormattedText, TooltipComponent>right(new PaddingComponent(12)));
					}
				}
			}
		}

		return new GatherResult(InteractionResult.PASS, maxWidth, tooltipElements);
	}

	public static void onRenderTick(DeltaTracker tracker)
	{
		if (LegendaryTooltipsConfig.getInstance() == null || !LegendaryTooltipsConfig.getInstance().isLoaded())
		{
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();

		float deltaTime = tracker.getRealtimeDeltaTicks() / 50.0f;
		TooltipDecor.updateTimer(deltaTime);
		ItemModelComponent.updateTimer(deltaTime);

		if (minecraft.screen != null)
		{
			if (minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)
			{
				if (containerScreen.hoveredSlot != null &&
					containerScreen.hoveredSlot.hasItem())
				{
					ItemStack item = containerScreen.hoveredSlot.getItem();
					if (lastTooltipItem != item)
					{
						TooltipDecor.resetTimer();
						lastTooltipItem = item;
					}
				}
			}
		}
	}

	public static ColorExtResult onTooltipColorEvent(ItemStack stack, GuiGraphics graphics, int x, int y, Font font, int backgroundStart, int backgroundEnd, int borderStart, int borderEnd, List<ClientTooltipComponent> components, boolean comparison, int index)
	{
		ColorExtResult result = new ColorExtResult(backgroundStart, backgroundEnd, borderStart, borderEnd);
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.level.registryAccess() == null)
		{
			return result;
		}

		FrameDefinition frameDefinition = getDefinitionColors(stack, borderStart, borderEnd, backgroundStart, backgroundEnd, minecraft.level.registryAccess());

		// Every tooltip will send a color event before a posttext event, so we can store the color here.
		TooltipDecor.setCurrentTooltipBorderStart(frameDefinition.startBorder().get());
		TooltipDecor.setCurrentTooltipBorderEnd(frameDefinition.endBorder().get());
		TooltipDecor.setCurrentTooltipBackgroundStart(frameDefinition.startBackground().get());
		TooltipDecor.setCurrentTooltipBackgroundEnd(frameDefinition.endBackground().get());

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

	public static void onPostTooltipEvent(ItemStack itemStack, GuiGraphics graphics, int x, int y, Font font, int width, int height, List<ClientTooltipComponent> components, boolean comparison, int index)
	{
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.level.registryAccess() == null)
		{
			return;
		}

		FrameDefinition frameDefinition = LegendaryTooltipsConfig.getInstance().getFrameDefinition(itemStack, minecraft.level.registryAccess());

		if (frameDefinition.index() == NO_BORDER)
		{
			return;
		}

		PoseStack poseStack = graphics.pose();

		// If tooltip shadows are enabled, draw one now.
		if (LegendaryTooltipsConfig.getInstance().tooltipShadow.get())
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

		// If this item has a defined border, draw it.
		if (comparison)
		{
			TooltipDecor.drawBorder(poseStack, x, y - 11, width, height + 11, itemStack, components, font, frameDefinition, comparison, index);
		}
		else
		{
			TooltipDecor.drawBorder(poseStack, x, y, width, height, itemStack, components, font, frameDefinition, comparison, index);
		}
	}
}