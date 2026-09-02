package me.pepperbell.continuity.client.render;

import net.neoforged.neoforge.common.util.TriState;

/**
 * Immutable description of how a quad should be rendered. Instances are interned by
 * {@link MaterialFinder#find()}, so they can be compared by identity.
 */
public final class RenderMaterial {
	private final BlendMode blendMode;
	private final boolean emissive;
	private final boolean disableDiffuse;
	private final TriState ambientOcclusion;

	RenderMaterial(BlendMode blendMode, boolean emissive, boolean disableDiffuse, TriState ambientOcclusion) {
		this.blendMode = blendMode;
		this.emissive = emissive;
		this.disableDiffuse = disableDiffuse;
		this.ambientOcclusion = ambientOcclusion;
	}

	public BlendMode blendMode() {
		return blendMode;
	}

	public boolean emissive() {
		return emissive;
	}

	public boolean disableDiffuse() {
		return disableDiffuse;
	}

	public TriState ambientOcclusion() {
		return ambientOcclusion;
	}

	/**
	 * {@return the index of this material within the interning table}
	 */
	public int index() {
		return MaterialFinder.index(blendMode, emissive, disableDiffuse, ambientOcclusion);
	}

	@Override
	public String toString() {
		return "RenderMaterial[blendMode=" + blendMode + ", emissive=" + emissive + ", disableDiffuse=" + disableDiffuse + ", ambientOcclusion=" + ambientOcclusion + "]";
	}
}
