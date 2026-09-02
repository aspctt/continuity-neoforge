package me.pepperbell.continuity.api.client;

import java.util.function.Function;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;

public interface CachingPredicates {
	boolean affectsSprites();

	boolean affectsSprite(TextureAtlasSprite sprite);

	boolean affectsBlockStates();

	boolean affectsBlockState(BlockState state);

	boolean isValidForMultipass();

	interface Factory<T extends CtmProperties> {
		CachingPredicates createPredicates(T properties, Function<Material, TextureAtlasSprite> textureGetter);
	}
}
