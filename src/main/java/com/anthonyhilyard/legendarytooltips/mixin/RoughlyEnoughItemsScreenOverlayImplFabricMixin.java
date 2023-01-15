package com.anthonyhilyard.legendarytooltips.mixin;

import java.lang.reflect.Field;
import java.util.List;

import com.anthonyhilyard.iceberg.util.Tooltips;
import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.impl.client.gui.fabric.ScreenOverlayImplFabric;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@Mixin(ScreenOverlayImplFabric.class)
public class RoughlyEnoughItemsScreenOverlayImplFabricMixin
{
	@Inject(method ="renderTooltipInner(Lnet/minecraft/client/gui/screens/Screen;Lcom/mojang/blaze3d/vertex/PoseStack;Lme/shedaniel/rei/api/client/gui/widgets/Tooltip;II)V",
			at = @At(value = "HEAD"), require = 0)
	private void setHoverStack(Screen screen, PoseStack poseStack, Tooltip tooltip, int mouseX, int mouseY, CallbackInfo info)
	{
		EntryStack<?> entryStack = tooltip.getContextStack();
		ItemStack itemStack = entryStack.getType() == VanillaEntryTypes.ITEM ? entryStack.castValue() : ItemStack.EMPTY;

		try
		{
			Field tooltipStackField = Screen.class.getDeclaredField("tooltipStack");
			tooltipStackField.setAccessible(true);

			tooltipStackField.set(screen, itemStack);
		}
		catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
	}

	@Inject(method ="renderTooltipInner(Lnet/minecraft/client/gui/screens/Screen;Lcom/mojang/blaze3d/vertex/PoseStack;Lme/shedaniel/rei/api/client/gui/widgets/Tooltip;II)V",
			at = @At(value = "INVOKE", target = "Lme/shedaniel/rei/impl/client/gui/fabric/ScreenOverlayImplFabric;renderTooltipInner(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;II)V",
			shift = Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT, require = 0)
	private void formatTooltipComponents(Screen screen, PoseStack poseStack, Tooltip tooltip, int mouseX, int mouseY, CallbackInfo info, List<ClientTooltipComponent> lines)
	{
		EntryStack<?> entryStack = tooltip.getContextStack();
		ItemStack itemStack = entryStack.getType() == VanillaEntryTypes.ITEM ? entryStack.castValue() : ItemStack.EMPTY;

		if (itemStack.isEmpty())
		{
			return;
		}

		List<Component> textElements = tooltip.entries().stream().map(e -> e.isText() ? e.getAsText() : null).filter(e -> e != null).toList();
		lines.clear();
		lines.addAll(Tooltips.gatherTooltipComponents(itemStack, textElements, itemStack.getTooltipImage(), mouseX, screen.width, screen.height, null, screen.font, -1));
	}
}
