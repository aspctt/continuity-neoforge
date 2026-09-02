package me.pepperbell.continuity.client.render;

/**
 * An immutable batch of quads, produced by {@link MeshBuilder} and replayed into an emitter later.
 */
public final class Mesh {
	static final Mesh EMPTY = new Mesh(new MutableQuad[0]);

	private final MutableQuad[] quads;

	Mesh(MutableQuad[] quads) {
		this.quads = quads;
	}

	public boolean isEmpty() {
		return quads.length == 0;
	}

	public int size() {
		return quads.length;
	}

	/**
	 * Replays every quad in this mesh into {@code emitter}.
	 */
	public void outputTo(QuadEmitter emitter) {
		for (MutableQuad quad : quads) {
			emitter.copyFrom(quad);
			emitter.emit();
		}
	}
}
