package me.pepperbell.continuity.client.render;

import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.core.Direction;

/**
 * Base emitter that writes into itself and hands the finished quad to {@link #emitDirectly()}.
 */
public abstract class AbstractQuadEmitter extends MutableQuad implements QuadEmitter {
	/**
	 * Called by {@link #emit()} while the quad still holds the data to output.
	 */
	protected abstract void emitDirectly();

	@Override
	public QuadEmitter emit() {
		emitDirectly();
		clear();
		return this;
	}

	@Override
	public QuadEmitter square(Direction face, float left, float bottom, float right, float top, float depth) {
		nominalFace(face);
		cullFace(Math.abs(depth) < 0.00001f ? face : null);

		// Lay the rectangle out as a degenerate box, then read its corners back through the same table the model
		// baker uses. That keeps the winding and the vertex order identical to a face baked from a block model.
		//
		// The in-plane axes are assigned so that left/bottom/right/top are read as a viewer facing the block sees
		// them, which means one axis runs backwards on the three positive-facing sides.
		float[] shape = new float[Direction.values().length];
		float plane = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0f - depth : depth;

		switch (face) {
			case DOWN -> {
				shape[FaceInfo.Constants.MIN_Y] = plane;
				shape[FaceInfo.Constants.MAX_Y] = plane;
				shape[FaceInfo.Constants.MIN_X] = left;
				shape[FaceInfo.Constants.MAX_X] = right;
				shape[FaceInfo.Constants.MIN_Z] = bottom;
				shape[FaceInfo.Constants.MAX_Z] = top;
			}
			case UP -> {
				shape[FaceInfo.Constants.MIN_Y] = plane;
				shape[FaceInfo.Constants.MAX_Y] = plane;
				shape[FaceInfo.Constants.MIN_X] = left;
				shape[FaceInfo.Constants.MAX_X] = right;
				shape[FaceInfo.Constants.MIN_Z] = 1.0f - top;
				shape[FaceInfo.Constants.MAX_Z] = 1.0f - bottom;
			}
			case NORTH -> {
				shape[FaceInfo.Constants.MIN_Z] = plane;
				shape[FaceInfo.Constants.MAX_Z] = plane;
				shape[FaceInfo.Constants.MIN_X] = 1.0f - right;
				shape[FaceInfo.Constants.MAX_X] = 1.0f - left;
				shape[FaceInfo.Constants.MIN_Y] = bottom;
				shape[FaceInfo.Constants.MAX_Y] = top;
			}
			case SOUTH -> {
				shape[FaceInfo.Constants.MIN_Z] = plane;
				shape[FaceInfo.Constants.MAX_Z] = plane;
				shape[FaceInfo.Constants.MIN_X] = left;
				shape[FaceInfo.Constants.MAX_X] = right;
				shape[FaceInfo.Constants.MIN_Y] = bottom;
				shape[FaceInfo.Constants.MAX_Y] = top;
			}
			case WEST -> {
				shape[FaceInfo.Constants.MIN_X] = plane;
				shape[FaceInfo.Constants.MAX_X] = plane;
				shape[FaceInfo.Constants.MIN_Z] = left;
				shape[FaceInfo.Constants.MAX_Z] = right;
				shape[FaceInfo.Constants.MIN_Y] = bottom;
				shape[FaceInfo.Constants.MAX_Y] = top;
			}
			case EAST -> {
				shape[FaceInfo.Constants.MIN_X] = plane;
				shape[FaceInfo.Constants.MAX_X] = plane;
				shape[FaceInfo.Constants.MIN_Z] = 1.0f - right;
				shape[FaceInfo.Constants.MAX_Z] = 1.0f - left;
				shape[FaceInfo.Constants.MIN_Y] = bottom;
				shape[FaceInfo.Constants.MAX_Y] = top;
			}
		}

		FaceInfo faceInfo = FaceInfo.fromFacing(face);
		for (int i = 0; i < VERTEX_COUNT; i++) {
			FaceInfo.VertexInfo vertexInfo = faceInfo.getVertexInfo(i);
			pos(i, shape[vertexInfo.xFace], shape[vertexInfo.yFace], shape[vertexInfo.zFace]);
		}

		return this;
	}
}
