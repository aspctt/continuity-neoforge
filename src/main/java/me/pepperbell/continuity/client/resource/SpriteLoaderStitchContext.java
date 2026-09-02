package me.pepperbell.continuity.client.resource;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;

public interface SpriteLoaderStitchContext {
	ThreadLocal<SpriteLoaderStitchContext> THREAD_LOCAL = new ThreadLocal<>();

	Map<ResourceLocation, ResourceLocation> getEmissiveIdMap();

	void markHasEmissives();
}
