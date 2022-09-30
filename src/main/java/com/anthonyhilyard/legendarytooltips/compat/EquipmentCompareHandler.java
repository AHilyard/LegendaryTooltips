package com.anthonyhilyard.legendarytooltips.compat;

import com.anthonyhilyard.equipmentcompare.events.RenderTooltipExtEvent.IRenderTooltipExt;

import net.minecraftforge.fml.common.eventhandler.Event;

public class EquipmentCompareHandler
{
	public static boolean isComparisonEvent(Event event)
	{
		return event instanceof IRenderTooltipExt && ((IRenderTooltipExt) event).isComparison();
	}

	public static int getEventIndex(Event event)
	{
		return event instanceof IRenderTooltipExt ? ((IRenderTooltipExt) event).getIndex() : 0;
	}
}
