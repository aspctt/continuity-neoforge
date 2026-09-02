package me.pepperbell.continuity.client.resource;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

public interface AtlasLoaderLoadContext {
	ThreadLocal<AtlasLoaderLoadContext> THREAD_LOCAL = new ThreadLocal<>();

	void setEmissiveIdMap(@Nullable Map<ResourceLocation, ResourceLocation> map);
}
