package me.pepperbell.continuity.client.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Accumulates emitted quads into a {@link Mesh}.
 *
 * <p>The backing quads are pooled and reused between builds, so {@link #build()} takes a snapshot rather than handing
 * out the working copies. Instances are not thread safe.
 */
public final class MeshBuilder {
	private final ObjectArrayList<MutableQuad> pool = new ObjectArrayList<>();
	private final Emitter emitter = new Emitter();
	private int size;

	public QuadEmitter getEmitter() {
		return emitter;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * {@return the quads emitted since the last build, and resets this builder}
	 */
	public Mesh build() {
		if (size == 0) {
			return Mesh.EMPTY;
		}

		MutableQuad[] quads = new MutableQuad[size];
		for (int i = 0; i < size; i++) {
			MutableQuad copy = new MutableQuad();
			copy.copyFrom(pool.get(i));
			quads[i] = copy;
		}
		size = 0;
		return new Mesh(quads);
	}

	public void clear() {
		size = 0;
	}

	private class Emitter extends AbstractQuadEmitter {
		@Override
		protected void emitDirectly() {
			if (size >= pool.size()) {
				pool.add(new MutableQuad());
			}
			pool.get(size).copyFrom(this);
			size++;
		}
	}
}
