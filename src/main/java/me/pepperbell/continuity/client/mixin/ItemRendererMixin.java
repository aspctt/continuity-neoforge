package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import me.pepperbell.continuity.client.render.EmissiveQuads;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;

/**
 * The item counterpart of {@code ModelBlockRendererMixin}: draws emissive quads on item models at full brightness.
 */
@Mixin(ItemRenderer.class)
abstract class ItemRendererMixin {
	//? if <1.21.11 {
	@WrapOperation(method = "renderQuadList", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V"))
	//?} else
	/*@WrapOperation(method = "renderQuadList", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFII)V"))*/
	//? if <1.21.4 {
	private void continuity$brightenEmissiveQuads(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay, boolean readExistingColor, Operation<Void> original) {
	//?} elif <1.21.11 {
	/*private static void continuity$brightenEmissiveQuads(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay, boolean readExistingColor, Operation<Void> original) {
	*///?} else
	/*private static void continuity$brightenEmissiveQuads(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay, Operation<Void> original) {*/
		if (EmissiveQuads.isEmissive(quad)) {
			packedLight = LightTexture.FULL_BRIGHT;
		}
		//? if <1.21.11 {
		original.call(consumer, pose, quad, red, green, blue, alpha, packedLight, packedOverlay, readExistingColor);
		//?} else
		/*original.call(consumer, pose, quad, red, green, blue, alpha, packedLight, packedOverlay);*/
	}
}
