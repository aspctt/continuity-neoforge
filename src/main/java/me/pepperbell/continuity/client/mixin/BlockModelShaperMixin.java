package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.pepperbell.continuity.client.util.SpriteCalculator;
import net.minecraft.client.renderer.block.BlockModelShaper;

@Mixin(BlockModelShaper.class)
abstract class BlockModelShaperMixin {
	@Inject(method = "replaceCache(Ljava/util/Map;)V", at = @At("HEAD"))
	private void continuity$onHeadReplaceCache(CallbackInfo ci) {
		SpriteCalculator.clearCache();
	}
}
