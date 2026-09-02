package me.pepperbell.continuity.client.render;

import net.minecraft.core.Direction;

/**
 * A quad that can be written and then handed onwards. Emitting outputs the current state and resets the emitter for
 * the next quad, so a single emitter can produce any number of quads in sequence.
 */
public interface QuadEmitter extends MutableQuadView {
	/**
	 * Outputs the current quad and resets this emitter.
	 */
	QuadEmitter emit();

	/**
	 * Fills in the geometry of an axis-aligned rectangle on the given face.
	 *
	 * <p>The coordinates are given in the plane of the face, with {@code depth} measured inwards from it. A quad at
	 * zero depth lies flush with the block face and is culled against it; anything deeper is never culled. Vertex
	 * order matches vanilla, so UVs assigned afterwards land the same way they would on a baked model face.
	 */
	QuadEmitter square(Direction face, float left, float bottom, float right, float top, float depth);
}
