package com.anthonyhilyard.legendarytooltips.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

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
		FMLJavaModLoadingContext.get().getModEventBus().addListener(PaddingComponent::onRegisterTooltipEvent);
	}

	private static void onRegisterTooltipEvent(RegisterClientTooltipComponentFactoriesEvent event)
	{
		event.register(PaddingComponent.class, x -> x);
	}
}
