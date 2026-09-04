package me.pepperbell.continuity.client.render;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
		return originalModel.getQuads(state, side, rand, data, renderType);
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
	public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
		return originalModel.useAmbientOcclusion(state, data, renderType);
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
	//?}

	//? if <1.21.2 {
	@Override
	public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
		return originalModel.getOverrides();
	}
	//?}
}
