package me.pepperbell.continuity.client.util.biome;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class BiomeHolder {
	private final ResourceLocation id;
	private Biome biome;

	BiomeHolder(ResourceLocation id) {
		this.id = id;
	}

	public ResourceLocation getId() {
		return id;
	}

	@Nullable
	public Biome getBiome() {
		return biome;
	}

	void refresh(Registry<Biome> biomeRegistry, Map<ResourceLocation, ResourceLocation> compactIdMap) {
		ResourceLocation id = compactIdMap.get(this.id);
		if (id == null) {
			id = this.id;
		}
		if (biomeRegistry.containsKey(id)) {
			biome = biomeRegistry.get(id);
		} else {
			ContinuityClient.LOGGER.warn("Unknown biome '" + this.id + "'");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BiomeHolder that = (BiomeHolder) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
