package com.anthonyhilyard.legendarytooltips.tooltip;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;
import com.anthonyhilyard.iceberg.renderer.CustomItemRenderer;
import com.anthonyhilyard.iceberg.util.GuiHelper;
import com.anthonyhilyard.iceberg.util.Tooltips.InlineComponent;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.anthonyhilyard.prism.text.DynamicColor;
import com.anthonyhilyard.prism.util.ColorUtil;
import com.anthonyhilyard.prism.util.ConfigHelper;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class ItemModelComponent implements TooltipComponent, ClientTooltipComponent, InlineComponent
{
	private static CustomItemRenderer customItemRenderer = null;

	private static float rotationTimer = 0.0f;

	private final ItemStack itemStack;

	public static void updateTimer(float partialTick)
	{
		double rotationInterval = LegendaryTooltipsConfig.getInstance().modelRotationSpeed.get();
		if (rotationInterval > 0)
		{
			rotationTimer += partialTick;
			if (rotationTimer > rotationInterval)
			{
				rotationTimer -= rotationInterval;
			}
		}
		else
		{
			rotationTimer = 0;
		}

	}

	public ItemModelComponent(ItemStack itemStack)
	{
		this.itemStack = itemStack;

		if (customItemRenderer == null)
		{
			Minecraft minecraft = Minecraft.getInstance();
			customItemRenderer = new CustomItemRenderer(minecraft);
		}
	}

	public int getRenderHeight() { return 22; }
	public int getRenderWidth() { return 22; }
	
	@Override
	public int getHeight(Font font) { return 4; }

	@Override
	public int getWidth(Font font) { return getRenderWidth(); }

	@Override
	public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics)
	{
		y--;
		x--;
		final int margin = 2;

		DynamicColor borderStartColor = DynamicColor.fromRgb(TooltipDecor.currentTooltipBorderStart);
		DynamicColor backgroundStartColor = DynamicColor.fromRgb(TooltipDecor.currentTooltipBackgroundStart);
		DynamicColor backgroundEndColor = ConfigHelper.applyModifiers(List.of("v+35", "s+10"), DynamicColor.fromRgb(TooltipDecor.currentTooltipBackgroundEnd));

		int borderStart = ColorUtil.combineARGB((int)(borderStartColor.alpha() * 0.35f), borderStartColor.red(), borderStartColor.green(), borderStartColor.blue());
		int backgroundStart = ColorUtil.combineARGB((int)(backgroundStartColor.alpha() * 0.15f), backgroundStartColor.red(), backgroundStartColor.green(), backgroundStartColor.blue());
		int backgroundEnd = ColorUtil.combineARGB((int)(backgroundEndColor.alpha() * 0.6f), backgroundEndColor.red(), backgroundEndColor.green(), backgroundEndColor.blue());

		// Draw the background first.
		GuiHelper.drawGradientRect(graphics, x + margin + 1, y + margin + 1, x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin - 1, backgroundStart, backgroundEnd);
		GuiHelper.drawGradientRect(graphics, x + margin + 1, y + margin + 1, x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin - 1, backgroundEnd, backgroundStart);
		GuiHelper.drawGradientRectHorizontal(graphics, x + margin + 1, y + margin + 1, x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin - 1, backgroundStart, backgroundEnd);
		GuiHelper.drawGradientRectHorizontal(graphics, x + margin + 1, y + margin + 1, x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin - 1, backgroundEnd, backgroundStart);

		// Draw the border.
		GuiHelper.drawGradientRect(graphics, x + margin + 1, y + margin, x + getRenderWidth() - margin - 1, y + margin + 1, borderStart, borderStart);
		GuiHelper.drawGradientRect(graphics, x + margin + 1, y + getRenderHeight() - margin - 1, x + getRenderWidth() - margin - 1, y + getRenderHeight() - margin, borderStart, borderStart);
		GuiHelper.drawGradientRect(graphics, x + margin, y + margin + 1, x + margin + 1, y + getRenderHeight() - margin - 1, borderStart, borderStart);
		GuiHelper.drawGradientRect(graphics, x + getRenderWidth() - margin - 1, y + margin + 1, x + getRenderWidth() - margin, y + getRenderHeight() - margin - 1, borderStart, borderStart);

		Matrix3x2fStack poseStack = graphics.pose();
		poseStack.pushMatrix();

		poseStack.translate(x + margin - 1, y + margin - 1);
		poseStack.scale(1.25f, 1.25f);

		float rotationAngle = 0.0f;
		if (LegendaryTooltipsConfig.getInstance().modelRotationSpeed.get() > 0)
		{
			rotationAngle = Mth.lerp(rotationTimer / LegendaryTooltipsConfig.getInstance().modelRotationSpeed.get().floatValue(), 0, 360.0f);
		}

		graphics.nextStratum();

		customItemRenderer.renderDetailModelIntoGUI(itemStack, 0, 0, Axis.YP.rotationDegrees(rotationAngle), graphics);

		poseStack.popMatrix();
	}

	public static void registerFactory()
	{
		RegisterTooltipComponentFactoryEvent.EVENT.register(ItemModelComponent.class, data -> {
			if (data instanceof ItemModelComponent itemModelComponent)
			{
				return itemModelComponent;
			}
			return null;
		});
	}
}
