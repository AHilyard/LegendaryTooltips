package com.anthonyhilyard.legendarytooltips.tooltip;

import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;

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
	public int getHeight(Font font) { return height; }

	@Override
	public int getWidth(Font font) { return 0; }

	public static void registerFactory()
	{
		RegisterTooltipComponentFactoryEvent.EVENT.register(PaddingComponent.class, data -> {
			if (data instanceof PaddingComponent paddingComponent)
			{
				return paddingComponent;
			}
			return null;
		});
	}
}