package com.anthonyhilyard.legendarytooltips.client;

import com.anthonyhilyard.iceberg.events.client.RenderTickEvents;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.services.Services;
import com.anthonyhilyard.legendarytooltips.tooltip.ItemModelComponent;
import com.anthonyhilyard.legendarytooltips.tooltip.PaddingComponent;

import net.minecraft.resources.Identifier;

import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;
import com.anthonyhilyard.legendarytooltips.config.FrameResourceParser;

public class LegendaryTooltipsClient
{
	public static void init()
	{
		ItemModelComponent.registerFactory();
		PaddingComponent.registerFactory();

		RenderTooltipEvents.GATHER.register(LegendaryTooltips::onGatherComponentsEvent);
		RenderTooltipEvents.COLOREXT.register(LegendaryTooltips::onTooltipColorEvent);
		RenderTooltipEvents.POSTEXT.register(LegendaryTooltips::onPostTooltipEvent);
		RenderTickEvents.START.register(LegendaryTooltips::onRenderTick);

		Services.getReloadListenerRegistrar().registerListener(FrameResourceParser.INSTANCE, Identifier.fromNamespaceAndPath(LegendaryTooltips.MODID, "frame_definitions"));
	}
}
