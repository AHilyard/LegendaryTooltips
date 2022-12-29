package com.anthonyhilyard.legendarytooltips.mixin;

import java.util.ArrayList;
import java.util.List;

import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.iceberg.util.Tooltips.TooltipInfo;
import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraftforge.client.event.RenderTooltipEvent;

@Mixin(Screen.class)
public class ScreenMixin
{
	@ModifyVariable(method = "renderTooltipInternal", ordinal = 0, at = @At(value = "LOAD", ordinal = 0), argsOnly = true)
	private List<ClientTooltipComponent> mutableComponents(List<ClientTooltipComponent> components)
	{
		if (LegendaryTooltipsConfig.INSTANCE.centeredTitle.get())
		{
			return new ArrayList<>(components);
		}
		else
		{
			return components;
		}
	}

	@ModifyVariable(method = "renderTooltipInternal", ordinal = 2, at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0))
	private int setMinimumWidth(int width)
	{
		if (LegendaryTooltipsConfig.INSTANCE.enforceMinimumWidth.get())
		{
			return Math.max(width, 48);
		}
		else
		{
			return width;
		}
	}

	@Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0),
		locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void centerTitle(PoseStack poseStack, List<ClientTooltipComponent> components, int x, int y, CallbackInfo info, RenderTooltipEvent.Pre preEvent)
	{
		if (!components.isEmpty() && LegendaryTooltipsConfig.INSTANCE.centeredTitle.get())
		{
			// Calculate tooltip width first.
			int tooltipWidth = 0;
			if (LegendaryTooltipsConfig.INSTANCE.enforceMinimumWidth.get())
			{
				tooltipWidth = 48;
			}

			tooltipWidth = new TooltipInfo(components, preEvent.getFont(), 1).getMaxLineWidth(tooltipWidth);

			// Replace the first component with the newly-centered version.
			List<ClientTooltipComponent> centeredComponents = Tooltips.centerTitle(components, preEvent.getFont(), tooltipWidth, Tooltips.calculateTitleLines(components));
			components.clear();
			components.addAll(centeredComponents);
		}
	}
}
