package com.anthonyhilyard.legendarytooltips.mixin;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class MixinConfig implements IMixinConfigPlugin
{
	private Collection<ModContainer> loadingModList = null;

	@Override
	public void onLoad(String mixinPackage) { }

	@Override
	public String getRefMapperConfig() { return null; }

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
		if (loadingModList == null)
		{
			loadingModList = FabricLoader.getInstance().getAllMods();
		}

		// Only apply mixins with "roughlyenoughitems" in the name if the mod "roughlyenoughitems" is present.
		if (mixinClassName.toLowerCase().contains("roughlyenoughitems"))
		{
			return loadingModList.stream().anyMatch(modContainer -> modContainer.getMetadata().getId().contentEquals("roughlyenoughitems"));
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

	@Override
	public List<String> getMixins() { return null; }

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}