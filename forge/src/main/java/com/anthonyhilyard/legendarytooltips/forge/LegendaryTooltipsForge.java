package com.anthonyhilyard.legendarytooltips.forge;

import com.anthonyhilyard.legendarytooltips.LegendaryTooltips;

import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LegendaryTooltips.MODID)
public final class LegendaryTooltipsForge
{
	public LegendaryTooltipsForge(FMLJavaModLoadingContext context)
	{
		context.registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "ANY", (remote, isServer) -> true));
	}
}
