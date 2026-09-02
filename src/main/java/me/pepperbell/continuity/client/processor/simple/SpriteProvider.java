package me.pepperbell.continuity.client.processor.simple;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import me.pepperbell.continuity.client.render.QuadView;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;

public interface SpriteProvider {
	@Nullable
	TextureAtlasSprite getSprite(QuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, ProcessingDataProvider dataProvider);

	interface Factory<T extends BaseCtmProperties> {
		SpriteProvider createSpriteProvider(TextureAtlasSprite[] sprites, T properties);

		int getTextureAmount(T properties);
	}
}
