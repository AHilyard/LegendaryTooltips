package com.anthonyhilyard.legendarytooltips.tooltip;

import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class PaddingComponent implements TooltipComponent, ClientTooltipComponent
{
	private final int height;
	public PaddingComponent(int height)
	{
		this.height = height;
	}

	@Override
	public int getHeight() { return height; }

	@Override
	public int getWidth(Font font) { return 0; }

	public static void registerFactory()
	{
		TooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof PaddingComponent paddingComponent)
			{
				return paddingComponent;
			}
			return null;
		});
	}
}