package me.pepperbell.continuity.client.render;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Writable view of a single quad. All setters return {@code this} so calls can be chained.
 */
public interface MutableQuadView extends QuadView {
	MutableQuadView sprite(@Nullable TextureAtlasSprite sprite);

	MutableQuadView material(RenderMaterial material);

	MutableQuadView colorIndex(int colorIndex);

	MutableQuadView cullFace(@Nullable Direction face);

	MutableQuadView nominalFace(@Nullable Direction face);

	MutableQuadView tag(int tag);

	MutableQuadView pos(int vertexIndex, float x, float y, float z);

	/**
	 * Sets the vertex colour, given in ARGB order.
	 */
	MutableQuadView color(int vertexIndex, int color);

	/**
	 * Sets the colour of all four vertices at once, each given in ARGB order.
	 */
	MutableQuadView color(int c0, int c1, int c2, int c3);

	MutableQuadView uv(int vertexIndex, float u, float v);

	MutableQuadView lightmap(int vertexIndex, int lightmap);

	MutableQuadView lightmap(int l0, int l1, int l2, int l3);

	MutableQuadView normal(int vertexIndex, float x, float y, float z);

	/**
	 * Replaces all state of this quad with that of {@code source}.
	 */
	MutableQuadView copyFrom(QuadView source);

	/**
	 * Loads this quad from a baked quad, treating {@code cullFace} as the face the quad was retrieved for.
	 */
	MutableQuadView fromVanilla(BakedQuad quad, @Nullable Direction cullFace);
}
