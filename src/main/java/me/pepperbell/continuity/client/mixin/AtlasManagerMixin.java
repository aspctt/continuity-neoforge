package me.pepperbell.continuity.client.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pepperbell.continuity.client.resource.ModelReloadHandler;
import net.minecraft.server.packs.resources.PreparableReloadListener;
//? if <26.1 {
import net.minecraft.client.resources.model.AtlasManager;
//?} else
/*import net.minecraft.client.resources.model.sprite.AtlasManager;*/

/**
 * Starts a Continuity reload and installs the atlas context, from 1.21.11 onwards.
 *
 * <p>Stitching used to be scheduled from inside the model reload, so installing the context at the head of
 * {@code ModelManager.reload} was early enough. It is its own reload listener now, registered ahead of the model
 * one, so by the time the model reload begins every atlas has already been handed to {@code SpriteLoader} and the
 * sprites Continuity needs are not among them. The context therefore has to go in here, one listener earlier.
 *
 * <p>The listener only schedules the work and returns, so the window between these two injections covers every
 * {@code loadAndStitch} call and nothing else.
 */
@Mixin(AtlasManager.class)
abstract class AtlasManagerMixin {
	@Inject(method = "reload", at = @At("HEAD"))
	private void continuity$onHeadReload(PreparableReloadListener.SharedState sharedState, Executor backgroundExecutor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor gameExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		ModelReloadHandler.beginReload(sharedState.resourceManager(), backgroundExecutor).setContext();
	}

	@Inject(method = "reload", at = @At("RETURN"))
	private void continuity$onReturnReload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		ModelReloadHandler handler = ModelReloadHandler.getCurrent();
		if (handler != null) {
			handler.clearContext();
		}
	}
}
