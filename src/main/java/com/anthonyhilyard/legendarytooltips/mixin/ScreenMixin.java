package com.anthonyhilyard.legendarytooltips.mixin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.anthonyhilyard.iceberg.Loader;
import com.anthonyhilyard.iceberg.util.Tooltips;
import com.anthonyhilyard.iceberg.util.Tooltips.TooltipInfo;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig;
import com.anthonyhilyard.legendarytooltips.LegendaryTooltipsConfig.ColorType;
import com.mojang.blaze3d.matrix.MatrixStack;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraftforge.fml.client.gui.GuiUtils;

@Mixin(Screen.class)
public class ScreenMixin
{
	@Shadow
	protected FontRenderer font;

	@Unique
	private static ItemStack getTooltipStack()
	{
		ItemStack result = ItemStack.EMPTY;
		try
		{
			Field cachedTooltipStackField = GuiUtils.class.getDeclaredField("cachedTooltipStack");
			cachedTooltipStackField.setAccessible(true);
			result = (ItemStack) cachedTooltipStackField.get(null);
			cachedTooltipStackField.setAccessible(false);
		}
		catch (Exception e)
		{
			Loader.LOGGER.error(ExceptionUtils.getStackTrace(e));
		}
		return result;
	}

	@Redirect(method = "renderWrappedToolTip", remap = false,
		at = @At(value = "INVOKE", remap = false,
		target = "Lnet/minecraftforge/fml/client/gui/GuiUtils;drawHoveringText(Lcom/mojang/blaze3d/matrix/MatrixStack;Ljava/util/List;IIIIILnet/minecraft/client/gui/FontRenderer;)V"))
	public void drawHoveringTextProxy(MatrixStack matrixStack, List<? extends ITextProperties> textLines, int mouseX, int mouseY, int screenWidth, int screenHeight, int maxTextWidth, FontRenderer font)
	{
		ItemStack tooltipStack = getTooltipStack();

		// If we're rendering an item tooltip, take over.
		if (tooltipStack != ItemStack.EMPTY)
		{
			Rectangle2d rect = Tooltips.calculateRect(tooltipStack, matrixStack, textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, font, LegendaryTooltipsConfig.INSTANCE.enforceMinimumWidth.get() ? 48 : 0, LegendaryTooltipsConfig.INSTANCE.centeredTitle.get());

			Tooltips.renderItemTooltip(tooltipStack, matrixStack, new TooltipInfo(textLines, font), rect, screenWidth, screenHeight,
				LegendaryTooltipsConfig.defaultColors.get(ColorType.BG_START).getValue(), LegendaryTooltipsConfig.defaultColors.get(ColorType.BG_END).getValue(),
				LegendaryTooltipsConfig.defaultColors.get(ColorType.BORDER_START).getValue(), LegendaryTooltipsConfig.defaultColors.get(ColorType.BORDER_END).getValue(),
				false, true, LegendaryTooltipsConfig.INSTANCE.centeredTitle.get(), 0);
		}
		else
		{
			GuiUtils.drawHoveringText(matrixStack, textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, font);
		}
	}

	// @Redirect(method = "renderTooltip(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/item/ItemStack;II)V", remap = false,
	// 	at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/fml/client/gui/GuiUtils;preItemToolTip(Lnet/minecraft/item/ItemStack;)V"))
	// public void preItemToolTipProxy(ItemStack itemStack)
	// {
	// 	tooltipStack = itemStack;
	// 	GuiUtils.preItemToolTip(itemStack);
	// }

	// @Redirect(method = "renderTooltip(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/item/ItemStack;II)V", remap = false,
	// 	at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/fml/client/gui/GuiUtils;postItemToolTip()V"))
	// public void postItemToolTipProxy()
	// {
	// 	tooltipStack = ItemStack.EMPTY;
	// 	GuiUtils.postItemToolTip();
	// }

	@Redirect(method = "Lnet/minecraft/client/gui/screen/Screen;renderTooltip(Lcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/item/ItemStack;II)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;getTooltipFromItem(Lnet/minecraft/item/ItemStack;)Ljava/util/List;"))
	private List<ITextComponent> getTooltipFromItem(Screen self, ItemStack itemStack)
	{
		List<ITextComponent> tooltips = self.getTooltipFromItem(itemStack);

		// If we're not centering, don't do anything.
		if (LegendaryTooltipsConfig.INSTANCE.centeredTitle.get())
		{
			return tooltips;
		}
		// Otherwise, return a mutable version of the component list.
		else
		{
			return new ArrayList<>(tooltips);
		}
	}
}