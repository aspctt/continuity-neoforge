package me.pepperbell.continuity.client.mixin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pepperbell.continuity.client.resource.ModelReloadHandler;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Starts a Continuity reload alongside the model reload.
 *
 * <p>The atlas context has to be installed here rather than in an event, because the sprite sources are scheduled
 * synchronously from inside this method and the loader mixins pick the context up off the calling thread.
 */
@Mixin(ModelManager.class)
abstract class ModelManagerMixin {
	@Inject(method = "reload(Lnet/minecraft/server/packs/resources/PreparableReloadListener$PreparationBarrier;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/util/profiling/ProfilerFiller;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
	private void continuity$onHeadReload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		ModelReloadHandler.beginReload(resourceManager, backgroundExecutor).setContext();
	}

	@Inject(method = "reload(Lnet/minecraft/server/packs/resources/PreparableReloadListener$PreparationBarrier;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/util/profiling/ProfilerFiller;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
	private void continuity$onReturnReload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		ModelReloadHandler handler = ModelReloadHandler.getCurrent();
		if (handler != null) {
			handler.clearContext();
		}
	}

	/**
	 * Hands the stitched atlases over before the baking event fires, so sprite lookups can stay quiet about textures
	 * a pack failed to ship.
	 */
	@Inject(method = "loadModels", at = @At("HEAD"))
	private void continuity$onHeadLoadModels(ProfilerFiller profilerFiller, Map<ResourceLocation, AtlasSet.StitchResult> atlasPreparations, ModelBakery modelBakery, CallbackInfoReturnable<?> cir) {
		ModelReloadHandler handler = ModelReloadHandler.getCurrent();
		if (handler != null) {
			handler.setStitchResults(atlasPreparations);
		}
	}
}
