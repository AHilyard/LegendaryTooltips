package com.anthonyhilyard.legendarytooltips.forge.client;

import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;
import com.anthonyhilyard.legendarytooltips.client.LegendaryTooltipsClient;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

@EventBusSubscriber(modid = LegendaryTooltips.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public class LegendaryTooltipsForgeClient
{
	@SubscribeEvent
	public static void onConstructMod(final FMLConstructModEvent event)
	{
		LegendaryTooltips.init();
		LegendaryTooltipsClient.init();
	}
}
