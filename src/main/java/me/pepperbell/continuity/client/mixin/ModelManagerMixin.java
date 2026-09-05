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
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//? if <1.21.10
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
	//?} elif <1.21.10 {
	/*private void continuity$onHeadReload(PreparableReloadListener.PreparationBarrier preparationBarrier, ResourceManager resourceManager, Executor backgroundExecutor, Executor gameExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
	*///?} else
	/*private void continuity$onHeadReload(PreparableReloadListener.SharedState sharedState, Executor backgroundExecutor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor gameExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {*/
		//? if >=1.21.10
		/*ResourceManager resourceManager = sharedState.resourceManager();*/
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
	 * Hands a silent sprite lookup over before the baking event fires.
	 *
	 * <p>The getter the event supplies logs a warning and a stack trace for every sprite it cannot find. A CTM
	 * properties file naming a texture the pack does not ship is a normal, handled condition, and it should not read
	 * as a Continuity fault in someone's log. From 1.21.10 the stitched atlases are no longer passed around as an
	 * atlas set, so the block atlas preparations are captured instead.
	 */
	@Inject(method = "loadModels", at = @At("HEAD"))
	//? if <1.21.4 {
	private void continuity$onHeadLoadModels(CallbackInfoReturnable<?> cir, @Local(argsOnly = true) Map<ResourceLocation, AtlasSet.StitchResult> stitchResults) {
	//?} elif <1.21.10 {
	/*private static void continuity$onHeadLoadModels(CallbackInfoReturnable<?> cir, @Local(argsOnly = true) Map<ResourceLocation, AtlasSet.StitchResult> stitchResults) {
	*///?} else
	/*private static void continuity$onHeadLoadModels(CallbackInfoReturnable<?> cir, @Local(argsOnly = true) SpriteLoader.Preparations preparations) {*/
		ModelReloadHandler handler = ModelReloadHandler.getCurrent();
		if (handler == null) {
			return;
		}

		//? if <1.21.10 {
		handler.setSilentTextureGetter(material -> {
			AtlasSet.StitchResult stitchResult = stitchResults.get(material.atlasLocation());
			if (stitchResult == null) {
				return null;
			}
			TextureAtlasSprite sprite = stitchResult.getSprite(material.texture());
			return sprite != null ? sprite : stitchResult.missing();
		});
		//?} else {
		/*handler.setSilentTextureGetter(material -> {
			if (!material.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
				return null;
			}
			TextureAtlasSprite sprite = preparations.getSprite(material.texture());
			return sprite != null ? sprite : preparations.missing();
		});
		*///?}
	}
}
