package me.pepperbell.continuity.client.render;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Marker quad that must be rendered at full brightness.
 *
 * <p>NeoForge has no per-quad light override, so the light value is forced by
 * {@code ModelBlockRendererMixin} and {@code ItemRendererMixin} whenever they encounter a quad of this type.
 */
public class EmissiveBakedQuad extends BakedQuad {
	public EmissiveBakedQuad(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite sprite, boolean shade, boolean hasAmbientOcclusion) {
		super(vertices, tintIndex, direction, sprite, shade, hasAmbientOcclusion);
	}

	public static boolean isEmissive(BakedQuad quad) {
		return quad instanceof EmissiveBakedQuad;
	}
}
