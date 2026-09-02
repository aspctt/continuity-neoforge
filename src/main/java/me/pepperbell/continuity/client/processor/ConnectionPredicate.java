package me.pepperbell.continuity.client.processor;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;

public interface ConnectionPredicate {
	boolean shouldConnect(BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, BlockState otherAppearanceState, BlockState otherState, BlockPos otherPos, Direction face, TextureAtlasSprite quadSprite);

	default boolean shouldConnect(BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, BlockPos otherPos, Direction face, TextureAtlasSprite quadSprite) {
		BlockState otherState = blockView.getBlockState(otherPos);
		BlockState otherAppearanceState = otherState.getAppearance(blockView, otherPos, face, state, pos);
		return shouldConnect(blockView, appearanceState, state, pos, otherAppearanceState, otherState, otherPos, face, quadSprite);
	}

	default boolean shouldConnect(BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, BlockPos.MutableBlockPos otherPos, Direction face, TextureAtlasSprite quadSprite, boolean innerSeams) {
		if (shouldConnect(blockView, appearanceState, state, pos, otherPos, face, quadSprite)) {
			if (innerSeams) {
				otherPos.move(face);
				return !shouldConnect(blockView, appearanceState, state, pos, otherPos, face, quadSprite);
			} else {
				return true;
			}
		}
		return false;
	}
}
