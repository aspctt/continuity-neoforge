package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import me.pepperbell.continuity.client.render.EmissiveBakedQuad;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.neoforged.neoforge.client.model.lighting.QuadLighter;

/**
 * Draws emissive quads at full brightness under NeoForge's experimental light pipeline.
 *
 * <p>When that pipeline is switched on, {@code BlockRenderDispatcher} routes block rendering through
 * {@link QuadLighter} instead of {@code ModelBlockRenderer.putQuadData}, so the override there never runs. This is
 * the same substitution applied at the equivalent point.
 */
@Mixin(value = QuadLighter.class, remap = false)
abstract class QuadLighterMixin {
	private static final int[] CONTINUITY$FULL_BRIGHT_LIGHTMAP = {
			LightTexture.FULL_BRIGHT, LightTexture.FULL_BRIGHT, LightTexture.FULL_BRIGHT, LightTexture.FULL_BRIGHT
	};
	private static final float[] CONTINUITY$FULL_BRIGHTNESS = { 1.0f, 1.0f, 1.0f, 1.0f };

	@WrapOperation(method = "process", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V"))
	private void continuity$brightenEmissiveQuads(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue, float alpha, int[] lightmap, int packedOverlay, boolean readExistingColor, Operation<Void> original) {
		if (quad instanceof EmissiveBakedQuad) {
			brightness = CONTINUITY$FULL_BRIGHTNESS;
			lightmap = CONTINUITY$FULL_BRIGHT_LIGHTMAP;
		}
		original.call(consumer, pose, quad, brightness, red, green, blue, alpha, lightmap, packedOverlay, readExistingColor);
	}
}
