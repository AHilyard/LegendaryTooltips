package com.anthonyhilyard.legendarytooltips.mixin;

import java.util.function.Function;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

@Mixin(value = TooltipRenderUtil.class, priority = 1001)
public class TooltipRenderUtilMixin
{
	@Redirect(method = "renderTooltipBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V", ordinal = 0))
	private void replaceBackgroundRender(GuiGraphics graphics, Function<ResourceLocation, RenderType> renderTypeFunction, ResourceLocation sprite, int x, int y, int width, int height)
	{
		if (LegendaryTooltipsConfig.showGradientBackground(sprite))
		{
			//Tooltips.renderItemTooltip(null, null, null, height, height, height, x, y, width, graphics, null, false, false, false, height);
		}
		else
		{
			graphics.blitSprite(renderTypeFunction, sprite, x, y, width, height);
		}
	}

	@Redirect(method = "renderTooltipBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V", ordinal = 1))
	private void replaceBorderRender(GuiGraphics graphics, Function<ResourceLocation, RenderType> renderTypeFunction, ResourceLocation sprite, int x, int y, int width, int height)
	{
		if (LegendaryTooltipsConfig.showGradientBorder(sprite))
		{
			//Tooltips.renderItemTooltip(null, null, null, height, height, height, x, y, width, graphics, null, false, false, false, height);
		}
		else
		{
			graphics.blitSprite(renderTypeFunction, sprite, x, y, width, height);
		}
	}
}