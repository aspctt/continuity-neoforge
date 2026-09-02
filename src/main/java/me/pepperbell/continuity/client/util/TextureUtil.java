package me.pepperbell.continuity.client.util;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

public final class TextureUtil {
	public static final Material MISSING_SPRITE_ID = toSpriteId(MissingTextureAtlasSprite.getLocation());

	public static Material toSpriteId(ResourceLocation id) {
		return new Material(TextureAtlas.LOCATION_BLOCKS, id);
	}

	public static boolean isMissingSprite(TextureAtlasSprite sprite) {
		return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation());
	}
}
