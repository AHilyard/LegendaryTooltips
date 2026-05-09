package com.anthonyhilyard.legendarytooltips.fabric.mixin;

import java.util.List;

import com.anthonyhilyard.iceberg.util.ITooltipAccess;
import com.anthonyhilyard.iceberg.util.Tooltips;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.impl.client.gui.fabric.ScreenOverlayImplFabric;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@Mixin(value = ScreenOverlayImplFabric.class, remap = false)
public class RoughlyEnoughItemsScreenOverlayImplFabricMixin
{
	@Inject(method = "renderTooltipInner(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;Lme/shedaniel/rei/api/client/gui/widgets/Tooltip;II)V",
			at = @At(value = "HEAD"), require = 0)
	private void addItemStackAccess(Screen screen, GuiGraphics graphics, Tooltip tooltip, int mouseX, int mouseY, CallbackInfo info)
	{
		EntryStack<?> entryStack = tooltip.getContextStack();
		ItemStack itemStack = entryStack.getType() == VanillaEntryTypes.ITEM ? entryStack.castValue() : ItemStack.EMPTY;

		if (graphics instanceof ITooltipAccess tooltipAccess) {
			tooltipAccess.setIcebergTooltipStack(itemStack);
		}
	}

	@ModifyVariable(method = "renderTooltipInner(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/GuiGraphics;Lme/shedaniel/rei/api/client/gui/widgets/Tooltip;II)V",
			at = @At(value = "INVOKE", target = "Lme/shedaniel/rei/impl/client/gui/fabric/ScreenOverlayImplFabric;renderTooltipInner(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;IILnet/minecraft/resources/Identifier;)V"),
			require = 0)
	private List<ClientTooltipComponent> formatTooltipComponents(List<ClientTooltipComponent> originalLines, Screen screen, GuiGraphics graphics, Tooltip tooltip, int mouseX, int mouseY)
	{
		EntryStack<?> entryStack = tooltip.getContextStack();
		ItemStack itemStack = entryStack.getType() == VanillaEntryTypes.ITEM ? entryStack.castValue() : ItemStack.EMPTY;

		if (itemStack.isEmpty())
		{
			return originalLines;
		}

		List<Component> textElements = tooltip.entries().stream().map(e -> e.isText() ? e.getAsText() : null).filter(e -> e != null).toList();

		return Tooltips.gatherTooltipComponents(itemStack, textElements, itemStack.getTooltipImage(), mouseX, screen.width, screen.height, null, Minecraft.getInstance().font, -1);
	}
}
