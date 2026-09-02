package me.pepperbell.continuity.client.render;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Read-only view of a single quad.
 *
 * <p>The geometry is stored in the vanilla {@code DefaultVertexFormat.BLOCK} layout, so a view can wrap the vertex
 * array of a {@link net.minecraft.client.renderer.block.model.BakedQuad} without copying it.
 */
public interface QuadView {
	int VERTEX_STRIDE = 8;
	int VERTEX_COUNT = 4;
	int QUAD_STRIDE = VERTEX_STRIDE * VERTEX_COUNT;

	int OFFSET_X = 0;
	int OFFSET_Y = 1;
	int OFFSET_Z = 2;
	int OFFSET_COLOR = 3;
	int OFFSET_U = 4;
	int OFFSET_V = 5;
	int OFFSET_LIGHTMAP = 6;
	int OFFSET_NORMAL = 7;

	/**
	 * {@return the sprite this quad samples, or {@code null} if it is not known}
	 */
	@Nullable
	TextureAtlasSprite sprite();

	/**
	 * {@return the material describing how this quad is rendered}
	 */
	RenderMaterial material();

	/**
	 * {@return the block colour tint index, or {@code -1} when the quad is untinted}
	 */
	int colorIndex();

	/**
	 * {@return the face this quad is culled against, or {@code null} when it is never culled}
	 */
	@Nullable
	Direction cullFace();

	/**
	 * {@return the face used to orient this quad, which is the closest axis-aligned direction to its normal}
	 */
	Direction lightFace();

	/**
	 * {@return the face this quad nominally belongs to; equal to {@link #lightFace()} unless explicitly overridden}
	 */
	Direction nominalFace();

	float x(int vertexIndex);

	float y(int vertexIndex);

	float z(int vertexIndex);

	/**
	 * {@return the position component of a vertex, where {@code coordinateIndex} is 0 for X, 1 for Y and 2 for Z}
	 */
	float posByIndex(int vertexIndex, int coordinateIndex);

	/**
	 * {@return the vertex colour in ARGB order}
	 */
	int color(int vertexIndex);

	float u(int vertexIndex);

	float v(int vertexIndex);

	int lightmap(int vertexIndex);

	boolean hasNormal(int vertexIndex);

	float normalX(int vertexIndex);

	float normalY(int vertexIndex);

	float normalZ(int vertexIndex);

	/**
	 * {@return the arbitrary integer tag attached to this quad by a processor}
	 */
	int tag();

	/**
	 * Copies this quad into {@code target}, replacing all of its state.
	 */
	void copyTo(MutableQuadView target);
}
