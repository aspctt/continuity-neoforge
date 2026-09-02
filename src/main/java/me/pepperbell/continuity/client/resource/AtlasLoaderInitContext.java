package me.pepperbell.continuity.client.resource;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

public interface AtlasLoaderInitContext {
	ThreadLocal<AtlasLoaderInitContext> THREAD_LOCAL = new ThreadLocal<>();

	@Nullable
	Set<ResourceLocation> getExtraIds();
}
