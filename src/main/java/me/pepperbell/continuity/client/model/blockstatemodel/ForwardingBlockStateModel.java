package me.pepperbell.continuity.client.model.blockstatemodel;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block state model that delegates everything to the model it wraps.
 */
public abstract class ForwardingBlockStateModel implements BlockStateModel {
	protected final BlockStateModel originalModel;

	protected ForwardingBlockStateModel(BlockStateModel originalModel) {
		this.originalModel = originalModel;
	}

	@Override
	public void collectParts(RandomSource random, List<BlockModelPart> parts) {
		originalModel.collectParts(random, parts);
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
		originalModel.collectParts(level, pos, state, random, parts);
	}

	@Override
	@Nullable
	public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		return originalModel.createGeometryKey(level, pos, state, random);
	}

	@Override
	public TextureAtlasSprite particleIcon() {
		return originalModel.particleIcon();
	}

	@Override
	public TextureAtlasSprite particleIcon(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		return originalModel.particleIcon(level, pos, state);
	}
}
