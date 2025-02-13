package com.anthonyhilyard.legendarytooltips.mixin;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;

@Mixin(value = ItemStack.class)
public class ItemStackMixin
{
	@Redirect(method = "addModifierTooltip",
				at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttributeBaseValue(Lnet/minecraft/core/Holder;)D"))
	public double getAttributeBaseValueProxy(Player player, Holder<Attribute> holder, Consumer<Component> consumer, @Nullable Player player2, Holder<Attribute> holder2, AttributeModifier attributeModifier)
	{
		if (LegendaryTooltipsConfig.getInstance().fixMC271840.get() && attributeModifier.is(Item.BASE_ATTACK_DAMAGE_ID))
		{
			ItemStack instance = (ItemStack)(Object)this;
			float f = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			ItemEnchantments itemEnchantments = (ItemEnchantments)instance.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
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
			return f;
		}
		else
		{
			return player.getAttributeBaseValue(holder);
		}
	}
}