package com.anthonyhilyard.legendarytooltips.mixin;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;

@Mixin(ItemStack.class)
public class ItemStackMixin
{

	@Inject(method = "addAttributeTooltips", at = @At("HEAD"), cancellable = true)
	private void injectBaseAttackDamageFix(Consumer<Component> consumer, TooltipDisplay tooltipDisplay, @Nullable Player player, CallbackInfo ci)
	{
		if (player == null) return;
		if (!LegendaryTooltipsConfig.getInstance().fixMC271840.get()) return;

		ci.cancel();

		if (!tooltipDisplay.shows(DataComponents.ATTRIBUTE_MODIFIERS)) return;

		for (EquipmentSlotGroup equipmentSlotGroup : EquipmentSlotGroup.values())
		{
			MutableBoolean mutableBoolean = new MutableBoolean(true);

			((ItemStack)(Object)this).forEachModifier(equipmentSlotGroup, (holder, attributeModifier, display) -> {
				if (display != ItemAttributeModifiers.Display.hidden())
				{
					modifyFormatting(mutableBoolean, equipmentSlotGroup, consumer, attributeModifier, player, display, holder);
				}
			});
		}
	}

	private void modifyFormatting(MutableBoolean mutableBoolean, EquipmentSlotGroup equipmentSlotGroup, Consumer<Component> consumer,
								  AttributeModifier attributeModifier, Player player, ItemAttributeModifiers.Display display, Holder<Attribute> holder)
	{
		if (mutableBoolean.isTrue())
		{
			consumer.accept(CommonComponents.EMPTY);
			consumer.accept(Component.translatable("item.modifiers." + equipmentSlotGroup.getSerializedName()).withStyle(ChatFormatting.GRAY));
			mutableBoolean.setFalse();
		}

		AttributeModifier finalModifier = attributeModifier;

		if (attributeModifier.is(Item.BASE_ATTACK_DAMAGE_ID))
		{
			ItemStack instance = (ItemStack)(Object)this;
			float f = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			ItemEnchantments itemEnchantments = instance.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
			for (var entry : itemEnchantments.entrySet())
			{
				Enchantment enchantment = entry.getKey().value();
				List<ConditionalEffect<EnchantmentValueEffect>> effects = enchantment.getEffects(EnchantmentEffectComponents.DAMAGE);

				if (effects.isEmpty())
				{
					continue;
				}

				for (ConditionalEffect<EnchantmentValueEffect> effect : effects)
				{
					if (effect.requirements().isEmpty())
					{
						f = effect.effect().process(entry.getIntValue(), player.getRandom(), f);
					}
				}
			}

			double bonus = f - player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
			finalModifier = new AttributeModifier(attributeModifier.id(), attributeModifier.amount() + bonus, attributeModifier.operation());
		}

		display.apply(consumer, player, holder, finalModifier);
	}
}