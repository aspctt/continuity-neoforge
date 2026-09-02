package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.resource.CustomBlockLayers;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;

/**
 * Applies the layers a pack declares in {@code optifine/block.properties}.
 *
 * <p>Three entry points are covered. Chunk rendering asks {@code getRenderLayers}, since NeoForge lets a block sit in
 * several layers at once; the two single-layer methods are deprecated upstream but still reached from mod code and
 * from Continuity itself when resolving what the default blend mode means for a block.
 */
@Mixin(ItemBlockRenderTypes.class)
abstract class ItemBlockRenderTypesMixin {
	@Inject(method = "getRenderLayers(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/neoforged/neoforge/client/ChunkRenderTypeSet;", at = @At("HEAD"), cancellable = true, remap = false)
	private static void continuity$onHeadGetRenderLayers(BlockState state, CallbackInfoReturnable<ChunkRenderTypeSet> cir) {
		RenderType layer = continuity$getCustomLayer(state);
		if (layer != null) {
			cir.setReturnValue(ChunkRenderTypeSet.of(layer));
		}
	}

	@Inject(method = "getChunkRenderType(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true)
	private static void continuity$onHeadGetChunkRenderType(BlockState state, CallbackInfoReturnable<RenderType> cir) {
		RenderType layer = continuity$getCustomLayer(state);
		if (layer != null) {
			cir.setReturnValue(layer);
		}
	}

	@Inject(method = "getMovingBlockRenderType(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true)
	private static void continuity$onHeadGetMovingBlockRenderType(BlockState state, CallbackInfoReturnable<RenderType> cir) {
		RenderType layer = continuity$getCustomLayer(state);
		if (layer != null) {
			cir.setReturnValue(layer == RenderType.translucent() ? RenderType.translucentMovingBlock() : layer);
		}
	}

	@Unique
	private static RenderType continuity$getCustomLayer(BlockState state) {
		if (CustomBlockLayers.isEmpty() || !ContinuityConfig.INSTANCE.customBlockLayers.get()) {
			return null;
		}
		return CustomBlockLayers.getLayer(state);
	}
}
