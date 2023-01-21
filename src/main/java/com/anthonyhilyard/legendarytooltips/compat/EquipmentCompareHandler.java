package com.anthonyhilyard.legendarytooltips.compat;

import com.anthonyhilyard.equipmentcompare.EquipmentCompare;
import com.anthonyhilyard.equipmentcompare.events.RenderTooltipExtEvent.IRenderTooltipExt;

import net.minecraftforge.fml.common.eventhandler.Event;

public class EquipmentCompareHandler
{
	private static boolean comparisonsWereActive = false;

	public static boolean isComparisonEvent(Event event)
	{
		return event instanceof IRenderTooltipExt && ((IRenderTooltipExt) event).isComparison();
	}

	public static int getEventIndex(Event event)
	{
		return event instanceof IRenderTooltipExt ? ((IRenderTooltipExt) event).getIndex() : 0;
	}

	public static boolean isComparisonActive()
	{
		return EquipmentCompare.comparisonsActive;
	}

	public static boolean comparisonsJustActivated()
	{
		if (!comparisonsWereActive && isComparisonActive())
		{
			comparisonsWereActive = true;
			return true;
		}
		else if (!isComparisonActive())
		{
			comparisonsWereActive = false;
		}

		return false;
	}
}
