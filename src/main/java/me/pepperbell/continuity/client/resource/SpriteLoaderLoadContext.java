package me.pepperbell.continuity.client.resource;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

public interface SpriteLoaderLoadContext {
	ThreadLocal<SpriteLoaderLoadContext> THREAD_LOCAL = new ThreadLocal<>();

	CompletableFuture<@Nullable Set<ResourceLocation>> getExtraIdsFuture(ResourceLocation atlasId);

	@Nullable
	EmissiveControl getEmissiveControl(ResourceLocation atlasId);

	interface EmissiveControl {
		@Nullable
		Map<ResourceLocation, ResourceLocation> getEmissiveIdMap();

		void setEmissiveIdMap(Map<ResourceLocation, ResourceLocation> emissiveIdMap);

		void markHasEmissives();
	}
}
