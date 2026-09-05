package me.pepperbell.continuity.client.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.model.ModelObjectsContainer;
import me.pepperbell.continuity.client.render.BlendMode;
import me.pepperbell.continuity.client.render.MaterialFinder;
import me.pepperbell.continuity.client.render.MutableQuad;
import me.pepperbell.continuity.client.render.RenderMaterial;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
//? if <1.21.10 {
import net.minecraft.world.entity.LivingEntity;
//?} else
/*import net.minecraft.world.entity.ItemOwner;*/

/**
 * Draws the emissive counterpart of an item texture on top of the item, from 1.21.5 onwards.
 *
 * <p>Item models stopped being baked models in 1.21.4, so the model layer that covers blocks on this band never sees
 * them. The copies are worked out once, the first time the model is drawn, and appended per draw after that, so
 * turning the setting off takes effect without a resource reload.
 */
@Mixin(BlockModelWrapper.class)
abstract class BlockModelWrapperMixin {
	@Unique
	private static final RenderMaterial CONTINUITY$EMISSIVE_MATERIAL = MaterialFinder.find(BlendMode.DEFAULT, true, true, TriState.FALSE);

	/** Distinguishes a render state this mixin contributed to from one it did not. */
	@Unique
	private static final Object CONTINUITY$EMISSIVE_MARKER = new Object();

	@Shadow
	@Final
	private List<BakedQuad> quads;

	@Unique
	@Nullable
	private List<BakedQuad> continuity$emissiveQuads;
	@Unique
	private boolean continuity$emissiveResolved;
	@Unique
	private boolean continuity$emissiveAnimated;

	//? if <1.21.10 {
	@Inject(method = "update(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/renderer/item/ItemModelResolver;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/LivingEntity;I)V", at = @At("TAIL"))
	private void continuity$onTailUpdate(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed, CallbackInfo ci, @Local ItemStackRenderState.LayerRenderState layer) {
	//?} else {
	/*@Inject(method = "update(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/renderer/item/ItemModelResolver;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/ItemOwner;I)V", at = @At("TAIL"))
	private void continuity$onTailUpdate(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed, CallbackInfo ci, @Local ItemStackRenderState.LayerRenderState layer) {
	*///?}
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			return;
		}
		if (!ModelObjectsContainer.get().featureStates.getEmissiveTexturesState().isEnabled()) {
			return;
		}

		List<BakedQuad> emissiveQuads = continuity$resolveEmissiveQuads();
		if (emissiveQuads == null) {
			return;
		}

		layer.prepareQuadList().addAll(emissiveQuads);
		//? if >=1.21.10 {
		/*renderState.appendModelIdentityElement(CONTINUITY$EMISSIVE_MARKER);
		if (continuity$emissiveAnimated) {
			renderState.setAnimated();
		}
		*///?}
	}

	/**
	 * {@return the emissive copies of this model's quads, or {@code null} if none of its textures has one}
	 *
	 * <p>Worked out on first use rather than at bake time, because the sprites are only linked to their emissive
	 * counterparts once the atlas has been stitched.
	 */
	@Unique
	@Nullable
	private List<BakedQuad> continuity$resolveEmissiveQuads() {
		if (continuity$emissiveResolved) {
			return continuity$emissiveQuads;
		}

		List<BakedQuad> emissiveQuads = null;
		MutableQuad workingQuad = new MutableQuad();
		int amount = quads.size();
		for (int i = 0; i < amount; i++) {
			BakedQuad quad = quads.get(i);
			workingQuad.fromVanilla(quad, null);

			TextureAtlasSprite sprite = workingQuad.sprite();
			TextureAtlasSprite emissiveSprite = sprite == null ? null : EmissiveSpriteApi.get().getEmissiveSprite(sprite);
			if (emissiveSprite == null) {
				continue;
			}

			workingQuad.material(CONTINUITY$EMISSIVE_MATERIAL);
			QuadUtil.interpolate(workingQuad, sprite, emissiveSprite);
			workingQuad.sprite(emissiveSprite);

			if (emissiveQuads == null) {
				emissiveQuads = new ObjectArrayList<>();
			}
			emissiveQuads.add(workingQuad.toBakedQuad());

			//? if >=1.21.10 {
			/*if (emissiveSprite.contents().isAnimated()) {
				continuity$emissiveAnimated = true;
			}
			*///?}
		}

		continuity$emissiveQuads = emissiveQuads;
		continuity$emissiveResolved = true;
		return emissiveQuads;
	}
}
