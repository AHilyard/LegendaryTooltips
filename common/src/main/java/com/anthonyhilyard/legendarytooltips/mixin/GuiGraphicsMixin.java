package com.anthonyhilyard.legendarytooltips.mixin;

import java.util.ArrayList;
import java.util.List;

import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.iceberg.util.Tooltips.TooltipInfo;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.resources.Identifier;

@Mixin(value = GuiGraphics.class, priority = 1001)
public class GuiGraphicsMixin
{
	@ModifyVariable(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V", at = @At("HEAD"), argsOnly = true)
	private List<ClientTooltipComponent> makeTooltipMutable(List<ClientTooltipComponent> components)
	{
		// Makes the components list mutable.
		return new ArrayList<>(components);
	}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V", at = @At("HEAD"))
	private void applyLegendaryFormatting(Font font, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, Identifier resourceLocation, CallbackInfo info)
	{
		if (components.isEmpty() || font == null) return;

		boolean enforceWidth = LegendaryTooltipsConfig.getInstance().enforceMinimumWidth.get();
		boolean centerTitle = LegendaryTooltipsConfig.getInstance().centeredTitle.get();

		if (centerTitle || enforceWidth)
		{
			int minWidth = enforceWidth ? 48 : 0;
			int currentWidth = new TooltipInfo(components, font, 1).getMaxLineWidth(minWidth);

			if (centerTitle)
			{
				List<ClientTooltipComponent> centered = Tooltips.centerTitle(components, font, currentWidth, Tooltips.calculateTitleLines(components));
				components.clear();
				components.addAll(centered);
			}
			else if (enforceWidth)
			{
				List<ClientTooltipComponent> padded = Tooltips.centerTitle(components, font, currentWidth, 1);
				components.set(0, padded.get(0));
			}
		}
	}
}