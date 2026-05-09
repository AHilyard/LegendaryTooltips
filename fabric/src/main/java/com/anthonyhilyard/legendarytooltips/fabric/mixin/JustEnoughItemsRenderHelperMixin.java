package com.anthonyhilyard.legendarytooltips.fabric.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Stream;

import mezz.jei.fabric.platform.RenderHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.util.ITooltipAccess;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.mojang.datafixers.util.Either;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

@Mixin(RenderHelper.class)
public class JustEnoughItemsRenderHelperMixin
{
	// For JEI versions prior to 19.5.
	@Surrogate
	private void setTooltipStack(Screen screen,
		GuiGraphics graphics,
		List<Component> textComponents,
		Optional<TooltipComponent> tooltipComponent,
		int x, int y,
		Font font,
		ItemStack itemStack, CallbackInfo info)
	{
		((ITooltipAccess)graphics).setIcebergTooltipStack(itemStack);
	}

	@Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;IILnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;)V",
			at = @At(value = "HEAD"), require = 0)
	private void setTooltipStack(GuiGraphics graphics,
		List<Either<FormattedText, TooltipComponent>> elements,
		int x, int y,
		Font font,
		ItemStack itemStack,
		CallbackInfo info)
	{
		((ITooltipAccess)graphics).setIcebergTooltipStack(itemStack);
	}

	@Redirect(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;IILnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;)V",
			  at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;collect(Ljava/util/stream/Collector;)Ljava/lang/Object;", remap = false))
	private Object formatTooltipComponents(Stream<ClientTooltipComponent> stream, Collector<ClientTooltipComponent, ?, ?> collector,
		GuiGraphics graphics,
		List<Either<FormattedText, TooltipComponent>> elements,
		int x, int y,
		Font font,
		ItemStack itemStack)
	{
		Minecraft minecraft = Minecraft.getInstance();
		Screen screen = minecraft.screen;

		if (screen == null)
		{
			return stream.collect(collector);
		}

		List<? extends FormattedText> textElements = elements.stream().map(e -> e.map(text -> text, component -> null)).filter(e -> e != null).toList();
		return new ArrayList<>(Tooltips.gatherTooltipComponents(itemStack, textElements, itemStack.getTooltipImage(), x, screen.width, screen.height, null, screen.font, -1));
	}
}
