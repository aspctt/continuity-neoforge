package me.pepperbell.continuity.client.render;

import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.core.Direction;

/**
 * Base emitter that writes into itself and hands the finished quad to {@link #emitDirectly()}.
 */
public abstract class AbstractQuadEmitter extends MutableQuad implements QuadEmitter {
	// Indices into the shape array below. 1.21.11 turned these from plain constants into an enum, but kept the
	// order, so the ordinals stand in for the old values.
	//? if <1.21.11 {
	private static final int MIN_X = FaceInfo.Constants.MIN_X;
	private static final int MIN_Y = FaceInfo.Constants.MIN_Y;
	private static final int MIN_Z = FaceInfo.Constants.MIN_Z;
	private static final int MAX_X = FaceInfo.Constants.MAX_X;
	private static final int MAX_Y = FaceInfo.Constants.MAX_Y;
	private static final int MAX_Z = FaceInfo.Constants.MAX_Z;
	//?} else {
	/*private static final int MIN_X = FaceInfo.Extent.MIN_X.ordinal();
	private static final int MIN_Y = FaceInfo.Extent.MIN_Y.ordinal();
	private static final int MIN_Z = FaceInfo.Extent.MIN_Z.ordinal();
	private static final int MAX_X = FaceInfo.Extent.MAX_X.ordinal();
	private static final int MAX_Y = FaceInfo.Extent.MAX_Y.ordinal();
	private static final int MAX_Z = FaceInfo.Extent.MAX_Z.ordinal();
	*///?}

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
				shape[MIN_Y] = plane;
				shape[MAX_Y] = plane;
				shape[MIN_X] = left;
				shape[MAX_X] = right;
				shape[MIN_Z] = bottom;
				shape[MAX_Z] = top;
			}
			case UP -> {
				shape[MIN_Y] = plane;
				shape[MAX_Y] = plane;
				shape[MIN_X] = left;
				shape[MAX_X] = right;
				shape[MIN_Z] = 1.0f - top;
				shape[MAX_Z] = 1.0f - bottom;
			}
			case NORTH -> {
				shape[MIN_Z] = plane;
				shape[MAX_Z] = plane;
				shape[MIN_X] = 1.0f - right;
				shape[MAX_X] = 1.0f - left;
				shape[MIN_Y] = bottom;
				shape[MAX_Y] = top;
			}
			case SOUTH -> {
				shape[MIN_Z] = plane;
				shape[MAX_Z] = plane;
				shape[MIN_X] = left;
				shape[MAX_X] = right;
				shape[MIN_Y] = bottom;
				shape[MAX_Y] = top;
			}
			case WEST -> {
				shape[MIN_X] = plane;
				shape[MAX_X] = plane;
				shape[MIN_Z] = left;
				shape[MAX_Z] = right;
				shape[MIN_Y] = bottom;
				shape[MAX_Y] = top;
			}
			case EAST -> {
				shape[MIN_X] = plane;
				shape[MAX_X] = plane;
				shape[MIN_Z] = 1.0f - right;
				shape[MAX_Z] = 1.0f - left;
				shape[MIN_Y] = bottom;
				shape[MAX_Y] = top;
			}
		}

		FaceInfo faceInfo = FaceInfo.fromFacing(face);
		for (int i = 0; i < VERTEX_COUNT; i++) {
			FaceInfo.VertexInfo vertexInfo = faceInfo.getVertexInfo(i);
			//? if <1.21.11 {
			pos(i, shape[vertexInfo.xFace], shape[vertexInfo.yFace], shape[vertexInfo.zFace]);
			//?} else
			/*pos(i, shape[vertexInfo.xFace().ordinal()], shape[vertexInfo.yFace().ordinal()], shape[vertexInfo.zFace().ordinal()]);*/
		}

		return this;
	}
}
