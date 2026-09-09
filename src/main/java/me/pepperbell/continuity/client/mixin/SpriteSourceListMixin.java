package me.pepperbell.continuity.client.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.client.resource.AtlasLoaderInitContext;
import me.pepperbell.continuity.client.resource.AtlasLoaderLoadContext;
import me.pepperbell.continuity.client.resource.EmissiveSuffixLoader;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

@Mixin(SpriteSourceList.class)
abstract class SpriteSourceListMixin {
	/**
	 * Adds the textures that CTM properties reference but no model does, so that they end up on the atlas. They go in
	 * front of the pack's own sources, which means a pack that defines the same texture itself still wins.
	 */
	@ModifyVariable(method = "<init>(Ljava/util/List;)V", at = @At(value = "LOAD", ordinal = 0), argsOnly = true, ordinal = 0)
	private List<SpriteSource> continuity$modifySources(List<SpriteSource> sources) {
		AtlasLoaderInitContext context = AtlasLoaderInitContext.THREAD_LOCAL.get();
		if (context != null) {
			Set<ResourceLocation> extraIds = context.getExtraIds();
			if (extraIds != null && !extraIds.isEmpty()) {
				List<SpriteSource> extraSources = new ObjectArrayList<>();
				for (ResourceLocation extraId : extraIds) {
					extraSources.add(new SingleFile(extraId, Optional.empty()));
				}

				if (sources instanceof ArrayList) {
					sources.addAll(0, extraSources);
				} else {
					List<SpriteSource> mutableSources = new ArrayList<>(extraSources);
					mutableSources.addAll(sources);
					return mutableSources;
				}
			}
		}
		return sources;
	}

	/**
	 * Pairs every sprite with its emissive counterpart, adding the counterpart to the atlas when the pack ships one.
	 *
	 * <p>From 1.21.9 the single argument overload only delegates, so the injection has to target the two argument
	 * one that actually collects the suppliers.
	 */
	//? if <1.21.9 {
	@Inject(method = "list(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/List;", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;builder()Lcom/google/common/collect/ImmutableList$Builder;", remap = false))
	private void continuity$afterRunSources(ResourceManager resourceManager, CallbackInfoReturnable<List<Function<SpriteResourceLoader, SpriteContents>>> cir, @Local Map<ResourceLocation, SpriteSource.SpriteSupplier> suppliers) {
	//?} else {
	/*@Inject(method = "list(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/Set;)Ljava/util/List;", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;builder()Lcom/google/common/collect/ImmutableList$Builder;", remap = false))
	private void continuity$afterRunSources(ResourceManager resourceManager, java.util.Set<?> additionalMetadata, CallbackInfoReturnable<List<Function<SpriteResourceLoader, SpriteContents>>> cir, @Local Map<ResourceLocation, SpriteSource.SpriteSupplier> suppliers) {
	*///?}
		AtlasLoaderLoadContext context = AtlasLoaderLoadContext.THREAD_LOCAL.get();
		if (context == null) {
			return;
		}

		String emissiveSuffix = EmissiveSuffixLoader.getEmissiveSuffix();
		if (emissiveSuffix == null) {
			return;
		}

		Map<ResourceLocation, SpriteSource.SpriteSupplier> emissiveSuppliers = new Object2ObjectOpenHashMap<>();
		Map<ResourceLocation, ResourceLocation> emissiveIdMap = new Object2ObjectOpenHashMap<>();
		suppliers.forEach((id, supplier) -> {
			if (!id.getPath().endsWith(emissiveSuffix)) {
				ResourceLocation emissiveId = id.withPath(id.getPath() + emissiveSuffix);
				if (!suppliers.containsKey(emissiveId)) {
					ResourceLocation emissiveLocation = emissiveId.withPath("textures/" + emissiveId.getPath() + ".png");
					Optional<Resource> optionalResource = resourceManager.getResource(emissiveLocation);
					if (optionalResource.isPresent()) {
						Resource resource = optionalResource.get();
						emissiveSuppliers.put(emissiveId, opener -> opener.loadSprite(emissiveId, resource));
						emissiveIdMap.put(id, emissiveId);
					}
				} else {
					emissiveIdMap.put(id, emissiveId);
				}
			}
		});
		suppliers.putAll(emissiveSuppliers);
		if (!emissiveIdMap.isEmpty()) {
			context.setEmissiveIdMap(emissiveIdMap);
		}
	}
}
