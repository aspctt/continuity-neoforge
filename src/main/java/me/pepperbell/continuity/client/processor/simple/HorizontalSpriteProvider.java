package me.pepperbell.continuity.client.processor.simple;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import me.pepperbell.continuity.client.processor.DirectionMaps;
import me.pepperbell.continuity.client.processor.OrientationMode;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.properties.OrientedConnectingCtmProperties;
import me.pepperbell.continuity.client.render.QuadView;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;

public class HorizontalSpriteProvider implements SpriteProvider {
	// Indices for this array are formed from these bit values:
	// 1   *   2
	protected static final int[] SPRITE_INDEX_MAP = new int[] {
			3, 2, 0, 1,
	};

	protected TextureAtlasSprite[] sprites;
	protected ConnectionPredicate connectionPredicate;
	protected boolean innerSeams;
	protected OrientationMode orientationMode;

	public HorizontalSpriteProvider(TextureAtlasSprite[] sprites, ConnectionPredicate connectionPredicate, boolean innerSeams, OrientationMode orientationMode) {
		this.sprites = sprites;
		this.connectionPredicate = connectionPredicate;
		this.innerSeams = innerSeams;
		this.orientationMode = orientationMode;
	}

	@Override
	@Nullable
	public TextureAtlasSprite getSprite(QuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, ProcessingDataProvider dataProvider) {
		Direction[] directions = DirectionMaps.getDirections(orientationMode, quad, appearanceState);
		BlockPos.MutableBlockPos mutablePos = dataProvider.getData(ProcessingDataKeys.MUTABLE_POS);
		int connections = getConnections(directions, mutablePos, blockView, appearanceState, state, pos, quad.lightFace(), sprite);
		return sprites[SPRITE_INDEX_MAP[connections]];
	}

	protected int getConnections(Direction[] directions, BlockPos.MutableBlockPos mutablePos, BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, Direction face, TextureAtlasSprite quadSprite) {
		int connections = 0;
		for (int i = 0; i < 2; i++) {
			mutablePos.setWithOffset(pos, directions[i * 2]);
			if (connectionPredicate.shouldConnect(blockView, appearanceState, state, pos, mutablePos, face, quadSprite, innerSeams)) {
				connections |= 1 << i;
			}
		}
		return connections;
	}

	public static class Factory implements SpriteProvider.Factory<OrientedConnectingCtmProperties> {
		@Override
		public SpriteProvider createSpriteProvider(TextureAtlasSprite[] sprites, OrientedConnectingCtmProperties properties) {
			return new HorizontalSpriteProvider(sprites, properties.getConnectionPredicate(), properties.getInnerSeams(), properties.getOrientationMode());
		}

		@Override
		public int getTextureAmount(OrientedConnectingCtmProperties properties) {
			return 4;
		}
	}
}
