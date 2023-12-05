package com.anthonyhilyard.legendarytooltips;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.locale.Language;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.server.packs.PackType;
import net.minecraft.ChatFormatting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;

import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import net.minecraftforge.fml.config.ModConfig;

import com.anthonyhilyard.iceberg.events.RenderTickEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.GatherResult;
import com.anthonyhilyard.legendarytooltips.config.FrameResourceParser;
import com.anthonyhilyard.legendarytooltips.config.LegacyConfigConverter;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameDefinition;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig.FrameSource;
import com.anthonyhilyard.legendarytooltips.tooltip.TooltipDecor;
import com.anthonyhilyard.iceberg.util.StringRecomposer;
import com.anthonyhilyard.iceberg.util.Tooltips.TitleBreakComponent;
import com.anthonyhilyard.legendarytooltips.tooltip.ItemModelComponent;
import com.anthonyhilyard.legendarytooltips.tooltip.PaddingComponent;
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
		// Check for legacy config files to convert now.
		LegacyConfigConverter.convert();

		ForgeConfigRegistry.INSTANCE.register(Loader.MODID, ModConfig.Type.COMMON, LegendaryTooltipsConfig.SPEC);

		ItemModelComponent.registerFactory();
		PaddingComponent.registerFactory();

		RenderTooltipEvents.GATHER.register(LegendaryTooltips::onGatherComponentsEvent);
		RenderTooltipEvents.COLOREXT.register(LegendaryTooltips::onTooltipColorEvent);
		RenderTooltipEvents.POSTEXT.register(LegendaryTooltips::onPostTooltipEvent);
		RenderTickEvents.START.register(LegendaryTooltips::onRenderTick);

		ModConfigEvents.reloading(Loader.MODID).register(LegendaryTooltipsConfig::onReload);

		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(FrameResourceParser.INSTANCE);
	}

	public static FrameDefinition getDefinitionColors(ItemStack item, int defaultStartBorder, int defaultEndBorder, int defaultStartBackground, int defaultEndBackground)
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

	public static GatherResult onGatherComponentsEvent(ItemStack itemStack, int screenWidth, int screenHeight, List<Either<FormattedText, TooltipComponent>> tooltipElements, int maxWidth, int index)
	{
		// If compact tooltips are turned on, remove a few unneeded lines from the tooltip.
		if (LegendaryTooltipsConfig.INSTANCE.compactTooltips.get())
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
				FormattedText title = tooltipElements.get(0).left().get();
				FormattedCharSequence paddedTitle = FormattedCharSequence.fromList(List.of(FormattedCharSequence.forward("      ", Style.EMPTY), Language.getInstance().getVisualOrder(title), FormattedCharSequence.forward(" ", Style.EMPTY)));
				List<FormattedText> recomposedTitle = StringRecomposer.recompose(List.of(ClientTooltipComponent.create(paddedTitle)));
				if (!recomposedTitle.isEmpty())
				{
					tooltipElements.set(0, Either.<FormattedText, TooltipComponent>left(recomposedTitle.get(0)));

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
						tooltipElements.add(2, Either.<FormattedText, TooltipComponent>left(FormattedText.of(" ")));
					}
				}
			}
		}

		return new GatherResult(InteractionResult.PASS, maxWidth, tooltipElements);
	}

	public static void onRenderTick(float partialTick)
	{
		Minecraft mc = Minecraft.getInstance();

		float deltaTime = mc.getDeltaFrameTime() / 50.0f;
		TooltipDecor.updateTimer(deltaTime);
		ItemModelComponent.updateTimer(deltaTime);

		if (mc.screen != null)
		{
			if (mc.screen instanceof AbstractContainerScreen<?> containerScreen)
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
		ColorExtResult result;
		FrameDefinition frameDefinition = getDefinitionColors(stack, borderStart, borderEnd, backgroundStart, backgroundEnd);

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
		if (LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(itemStack).index() == NO_BORDER)
		{
			return;
		}

		PoseStack poseStack = graphics.pose();

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
			// (PoseStack poseStack, int x, int y, int width, int height, ItemStack item, List<ClientTooltipComponent> components, Font font, FrameDefinition frameDefinition, boolean comparison, int index)
			TooltipDecor.drawBorder(poseStack, x, y - 11, width, height + 11, itemStack, components, font, LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(itemStack), comparison, index);
		}
		else
		{
			TooltipDecor.drawBorder(poseStack, x, y, width, height, itemStack, components, font, LegendaryTooltipsConfig.INSTANCE.getFrameDefinition(itemStack), comparison, index);
		}
	}
}