package me.pepperbell.continuity.client.mixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pepperbell.continuity.client.mixinterface.SpriteExtension;
import me.pepperbell.continuity.client.resource.AtlasLoaderInitContext;
import me.pepperbell.continuity.client.resource.AtlasLoaderLoadContext;
import me.pepperbell.continuity.client.resource.SpriteLoaderLoadContext;
import me.pepperbell.continuity.client.resource.SpriteLoaderStitchContext;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.ResourceLocation;

@Mixin(SpriteLoader.class)
abstract class SpriteLoaderMixin {
	@Shadow
	@Final
	private ResourceLocation location;

	@ModifyArg(method = "loadAndStitch(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceLocation;ILjava/util/concurrent/Executor;Ljava/util/Collection;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", ordinal = 0), index = 0)
	private Supplier<List<Function<SpriteResourceLoader, SpriteContents>>> continuity$modifySupplier(Supplier<List<Function<SpriteResourceLoader, SpriteContents>>> supplier) {
		SpriteLoaderLoadContext context = SpriteLoaderLoadContext.THREAD_LOCAL.get();
		if (context != null) {
			CompletableFuture<@Nullable Set<ResourceLocation>> extraIdsFuture = context.getExtraIdsFuture(location);
			SpriteLoaderLoadContext.EmissiveControl emissiveControl = context.getEmissiveControl(location);
			if (emissiveControl != null) {
				return () -> {
					AtlasLoaderInitContext.THREAD_LOCAL.set(extraIdsFuture::join);
					AtlasLoaderLoadContext.THREAD_LOCAL.set(emissiveControl::setEmissiveIdMap);
					List<Function<SpriteResourceLoader, SpriteContents>> list = supplier.get();
					AtlasLoaderInitContext.THREAD_LOCAL.set(null);
					AtlasLoaderLoadContext.THREAD_LOCAL.set(null);
					return list;
				};
			}
			return () -> {
				AtlasLoaderInitContext.THREAD_LOCAL.set(extraIdsFuture::join);
				List<Function<SpriteResourceLoader, SpriteContents>> list = supplier.get();
				AtlasLoaderInitContext.THREAD_LOCAL.set(null);
				return list;
			};
		}
		return supplier;
	}

	@ModifyArg(method = "loadAndStitch(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/ResourceLocation;ILjava/util/concurrent/Executor;Ljava/util/Collection;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenApply(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;", ordinal = 0), index = 0)
	private Function<List<SpriteContents>, SpriteLoader.Preparations> continuity$modifyFunction(Function<List<SpriteContents>, SpriteLoader.Preparations> function) {
		SpriteLoaderLoadContext context = SpriteLoaderLoadContext.THREAD_LOCAL.get();
		if (context != null) {
			SpriteLoaderLoadContext.EmissiveControl emissiveControl = context.getEmissiveControl(location);
			if (emissiveControl != null) {
				return spriteContentsList -> {
					Map<ResourceLocation, ResourceLocation> emissiveIdMap = emissiveControl.getEmissiveIdMap();
					if (emissiveIdMap != null) {
						SpriteLoaderStitchContext.THREAD_LOCAL.set(new SpriteLoaderStitchContext() {
							@Override
							public Map<ResourceLocation, ResourceLocation> getEmissiveIdMap() {
								return emissiveIdMap;
							}

							@Override
							public void markHasEmissives() {
								emissiveControl.markHasEmissives();
							}
						});
						SpriteLoader.Preparations result = function.apply(spriteContentsList);
						SpriteLoaderStitchContext.THREAD_LOCAL.set(null);
						return result;
					}
					return function.apply(spriteContentsList);
				};
			}
		}
		return function;
	}

	@Inject(method = "stitch(Ljava/util/List;ILjava/util/concurrent/Executor;)Lnet/minecraft/client/renderer/texture/SpriteLoader$Preparations;", at = @At("RETURN"))
	private void continuity$onReturnStitch(List<SpriteContents> spriteContentsList, int mipmapLevels, Executor executor, CallbackInfoReturnable<SpriteLoader.Preparations> cir) {
		SpriteLoaderStitchContext context = SpriteLoaderStitchContext.THREAD_LOCAL.get();
		if (context != null) {
			Map<ResourceLocation, ResourceLocation> emissiveIdMap = context.getEmissiveIdMap();
			Map<ResourceLocation, TextureAtlasSprite> sprites = cir.getReturnValue().regions();
			emissiveIdMap.forEach((id, emissiveId) -> {
				TextureAtlasSprite sprite = sprites.get(id);
				if (sprite != null) {
					TextureAtlasSprite emissiveSprite = sprites.get(emissiveId);
					if (emissiveSprite != null) {
						((SpriteExtension) sprite).continuity$setEmissiveSprite(emissiveSprite);
						context.markHasEmissives();
					}
				}
			});
		}
	}
}
