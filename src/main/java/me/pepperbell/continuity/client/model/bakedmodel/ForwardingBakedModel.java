package me.pepperbell.continuity.client.model.bakedmodel;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;

/**
 * A baked model that delegates everything to the model it wraps.
 *
 * <p>NeoForge used to provide {@code BakedModelWrapper} for this, but it was removed in 1.21.4 along with the rest of
 * the item model rework. Keeping our own means the wrappers extend the same base on every version, and the handful of
 * interface methods that come and go are dealt with here instead of in each subclass.
 */
public abstract class ForwardingBakedModel implements BakedModel {
	protected final BakedModel originalModel;

	protected ForwardingBakedModel(BakedModel originalModel) {
		this.originalModel = originalModel;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
		return originalModel.getQuads(state, side, rand);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType layer) {
		return originalModel.getQuads(state, side, rand, data, layer);
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
		return originalModel.getModelData(level, pos, state, modelData);
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		return originalModel.getRenderTypes(state, rand, data);
	}

	@Override
	public boolean useAmbientOcclusion() {
		return originalModel.useAmbientOcclusion();
	}

	@Override
	public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType layer) {
		return originalModel.useAmbientOcclusion(state, data, layer);
	}

	@Override
	public boolean isGui3d() {
		return originalModel.isGui3d();
	}

	@Override
	public boolean usesBlockLight() {
		return originalModel.usesBlockLight();
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return originalModel.getParticleIcon();
	}

	@Override
	public TextureAtlasSprite getParticleIcon(ModelData data) {
		return originalModel.getParticleIcon(data);
	}

	@Override
	public ItemTransforms getTransforms() {
		return originalModel.getTransforms();
	}

	//? if <1.21.4 {
	@Override
	public boolean isCustomRenderer() {
		return originalModel.isCustomRenderer();
	}

	/**
	 * Wraps a model that the wrapped one hands out to be drawn in its place, such as the one it draws for a display
	 * context. Wrappers that only matter for blocks leave it as it is.
	 */
	protected BakedModel rewrap(BakedModel model) {
		return model;
	}

	// A model can draw a different model for each display context, as NeoForge's separate transforms loader does for an
	// item held in hand, so the choice has to be left to it. The default would apply our transforms to ourselves and
	// draw the base model in every context.
	@Override
	public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean applyLeftHandTransform) {
		BakedModel transformed = originalModel.applyTransform(context, poseStack, applyLeftHandTransform);
		return transformed == originalModel ? this : rewrap(transformed);
	}

	// Composite and layered item models draw in several passes, each a model of its own, where the default would draw
	// just us in one.
	@Override
	public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
		List<BakedModel> passes = originalModel.getRenderPasses(itemStack, fabulous);
		if (passes.size() == 1 && passes.get(0) == originalModel) {
			return List.of(this);
		}

		List<BakedModel> wrappedPasses = new ObjectArrayList<>(passes.size());
		for (BakedModel pass : passes) {
			wrappedPasses.add(pass == originalModel ? this : rewrap(pass));
		}
		return wrappedPasses;
	}

	// The render type a model names in its JSON is only known to the model itself.
	@Override
	public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
		return originalModel.getRenderTypes(itemStack, fabulous);
	}
	//?}

	//? if <1.21.2 {
	@Override
	public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
		return originalModel.getOverrides();
	}
	//?}
}
