package me.pepperbell.continuity.client.processor;

import java.util.List;
import java.util.function.Function;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import me.pepperbell.continuity.client.util.TextureUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;

public abstract class AbstractQuadProcessorFactory<T extends BaseCtmProperties> implements QuadProcessor.Factory<T> {
	@Override
	public QuadProcessor createProcessor(T properties, Function<Material, TextureAtlasSprite> textureGetter) {
		int textureAmount = getTextureAmount(properties);
		List<Material> spriteIds = properties.getSpriteIds();
		int provided = spriteIds.size();
		int max = provided;

		if (provided > textureAmount) {
			ContinuityClient.LOGGER.warn("Method '" + properties.getMethod() + "' requires " + textureAmount + " tiles but " + provided + " were provided in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
			max = textureAmount;
		}

		TextureAtlasSprite[] sprites = new TextureAtlasSprite[textureAmount];
		TextureAtlasSprite missingSprite = textureGetter.apply(TextureUtil.MISSING_SPRITE_ID);
		boolean supportsNullSprites = supportsNullSprites(properties);
		for (int i = 0; i < max; i++) {
			TextureAtlasSprite sprite;
			Material spriteId = spriteIds.get(i);
			if (spriteId.equals(BaseCtmProperties.SPECIAL_SKIP_SPRITE_ID)) {
				sprite = missingSprite;
			} else if (spriteId.equals(BaseCtmProperties.SPECIAL_DEFAULT_SPRITE_ID)) {
				sprite = supportsNullSprites ? null : missingSprite;
			} else {
				sprite = textureGetter.apply(spriteId);
			}
			sprites[i] = sprite;
		}

		if (provided < textureAmount) {
			ContinuityClient.LOGGER.error("Method '" + properties.getMethod() + "' requires " + textureAmount + " tiles but only " + provided + " were provided in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
			for (int i = provided; i < textureAmount; i++) {
				sprites[i] = missingSprite;
			}
		}

		return createProcessor(properties, sprites);
	}

	public abstract QuadProcessor createProcessor(T properties, TextureAtlasSprite[] sprites);

	public abstract int getTextureAmount(T properties);

	public boolean supportsNullSprites(T properties) {
		return true;
	}
}
