package me.pepperbell.continuity.api.client;

import java.util.Collection;
import java.util.Properties;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.resources.model.Material;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackResources;
import net.minecraft.resources.ResourceLocation;

public interface CtmProperties extends Comparable<CtmProperties> {
	Collection<Material> getTextureDependencies();

	interface Factory<T extends CtmProperties> {
		@Nullable
		T createProperties(Properties properties, ResourceLocation resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method);
	}
}
