package com.anthonyhilyard.legendarytooltips.neoforge.mixin;

import com.anthonyhilyard.legendarytooltips.config.LegendaryTooltipsConfig;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.function.Consumer;

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
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.common.util.AttributeUtil;

@Mixin(AttributeUtil.class)
public class AttributeUtilMixin
{
	@Redirect(method = "applyTextFor", remap = false, require = 0,
				at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAttributeBaseValue(Lnet/minecraft/core/Holder;)D"))
	private static double getAttributeBaseValueProxy(Player player, Holder<Attribute> holder, ItemStack stack, Consumer<Component> tooltip, Multimap<Holder<Attribute>, AttributeModifier> modifierMap, AttributeTooltipContext ctx)
	{
		if (LegendaryTooltipsConfig.getInstance().fixMC271840.get() && holder.value().getBaseId() == Item.BASE_ATTACK_DAMAGE_ID)
		{
			float f = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
			ItemEnchantments itemEnchantments = (ItemEnchantments)stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
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