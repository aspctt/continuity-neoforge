package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.pepperbell.continuity.client.util.SpriteCalculator;
//? if <26.1 {
import net.minecraft.client.renderer.block.BlockModelShaper;
//?} else
/*import net.minecraft.client.renderer.block.BlockStateModelSet;*/

//? if <26.1 {
@Mixin(BlockModelShaper.class)
//?} else
/*@Mixin(BlockStateModelSet.class)*/
abstract class BlockModelShaperMixin {
	//? if <26.1 {
	@Inject(method = "replaceCache(Ljava/util/Map;)V", at = @At("HEAD"))
	private void continuity$onHeadReplaceCache(CallbackInfo ci) {
	//?} else {
	/*// 26.1 builds a new set on every reload rather than replacing the cache inside one, so the moment a set
	// is built is the moment the sprites it was calculated from are stale.
	@Inject(method = "<init>(Ljava/util/Map;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;)V", at = @At("RETURN"))
	private void continuity$onHeadReplaceCache(CallbackInfo ci) {
	*///?}
		SpriteCalculator.clearCache();
	}
}
