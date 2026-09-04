package me.pepperbell.continuity.client.mixin;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import me.pepperbell.continuity.client.resource.ModelReloadHandler;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
//? if <1.21.2
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Starts a Continuity reload alongside the model reload.
 *
 * <p>The atlas context has to be installed here rather than in an event, because the sprite sources are scheduled
 * synchronously from inside this method and the loader mixins pick the context up off the calling thread.
 *
 * <p>Both injections match by method name rather than by descriptor. Every one of these signatures changed at least
 * once across 1.21.x, and loadModels also turned static, so only the argument that matters is captured.
 */
@Mixin(ModelManager.class)
abstract class ModelManagerMixin {
	@Inject(method = "reload", at = @At("HEAD"))
	//? if <1.21.2 {
	private void continuity$onHeadReload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
	//?} else
	/*private void continuity$onHeadReload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, Executor backgroundExecutor, Executor gameExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {*/
		ModelReloadHandler.beginReload(resourceManager, backgroundExecutor).setContext();
	}

	@Inject(method = "reload", at = @At("RETURN"))
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
	//? if <1.21.4 {
	private void continuity$onHeadLoadModels(CallbackInfoReturnable<?> cir, @Local(argsOnly = true) Map<ResourceLocation, AtlasSet.StitchResult> stitchResults) {
	//?} else
	/*private static void continuity$onHeadLoadModels(CallbackInfoReturnable<?> cir, @Local(argsOnly = true) Map<ResourceLocation, AtlasSet.StitchResult> stitchResults) {*/
		ModelReloadHandler handler = ModelReloadHandler.getCurrent();
		if (handler != null) {
			handler.setStitchResults(stitchResults);
		}
	}
}
