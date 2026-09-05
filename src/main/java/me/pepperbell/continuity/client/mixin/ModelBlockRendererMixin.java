package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import me.pepperbell.continuity.client.render.EmissiveQuads;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;

/**
 * Draws emissive quads at full brightness.
 *
 * <p>NeoForge has no per-quad light override, so the quads Continuity adds for emissive textures are marked with
 * their own type and their light is replaced here, at the one point both the smooth and the flat lighting paths pass
 * through.
 */
@Mixin(ModelBlockRenderer.class)
abstract class ModelBlockRendererMixin {
	private static final int[] CONTINUITY$FULL_BRIGHT_LIGHTMAP = {
			LightTexture.FULL_BRIGHT, LightTexture.FULL_BRIGHT, LightTexture.FULL_BRIGHT, LightTexture.FULL_BRIGHT
	};
	private static final float[] CONTINUITY$FULL_BRIGHTNESS = { 1.0f, 1.0f, 1.0f, 1.0f };

	//? if <1.21.11 {
	@WrapOperation(method = "putQuadData", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V"))
	private void continuity$brightenEmissiveQuads(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue, float alpha, int[] lightmap, int packedOverlay, boolean readExistingColor, Operation<Void> original) {
	//?} else {
	/*@WrapOperation(method = "putQuadData", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[II)V"))
	private void continuity$brightenEmissiveQuads(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue, float alpha, int[] lightmap, int packedOverlay, Operation<Void> original) {
	*///?}
		if (EmissiveQuads.isEmissive(quad)) {
			brightness = CONTINUITY$FULL_BRIGHTNESS;
			lightmap = CONTINUITY$FULL_BRIGHT_LIGHTMAP;
		}
		//? if <1.21.11 {
		original.call(consumer, pose, quad, brightness, red, green, blue, alpha, lightmap, packedOverlay, readExistingColor);
		//?} else
		/*original.call(consumer, pose, quad, brightness, red, green, blue, alpha, lightmap, packedOverlay);*/
	}
}
