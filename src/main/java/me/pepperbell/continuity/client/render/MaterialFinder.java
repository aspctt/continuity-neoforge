package me.pepperbell.continuity.client.render;

import net.neoforged.neoforge.common.util.TriState;

/**
 * Mutable builder for {@link RenderMaterial}. Every distinct combination is created once and reused, so materials
 * returned by {@link #find()} can be compared by identity.
 *
 * <p>Instances are not thread safe; {@code RenderUtil#getMaterialFinder()} hands out a cleared finder per thread.
 */
public final class MaterialFinder {
	private static final TriState[] AO_VALUES = TriState.values();
	private static final int AO_COUNT = AO_VALUES.length;
	private static final RenderMaterial[] MATERIALS = new RenderMaterial[BlendMode.VALUES.length * 2 * 2 * AO_COUNT];

	static {
		for (BlendMode blendMode : BlendMode.VALUES) {
			for (int emissive = 0; emissive < 2; emissive++) {
				for (int disableDiffuse = 0; disableDiffuse < 2; disableDiffuse++) {
					for (TriState ao : AO_VALUES) {
						int index = index(blendMode, emissive != 0, disableDiffuse != 0, ao);
						MATERIALS[index] = new RenderMaterial(blendMode, emissive != 0, disableDiffuse != 0, ao);
					}
				}
			}
		}
	}

	public static final RenderMaterial DEFAULT_MATERIAL = MATERIALS[index(BlendMode.DEFAULT, false, false, TriState.DEFAULT)];

	private BlendMode blendMode = BlendMode.DEFAULT;
	private boolean emissive;
	private boolean disableDiffuse;
	private TriState ambientOcclusion = TriState.DEFAULT;

	static int index(BlendMode blendMode, boolean emissive, boolean disableDiffuse, TriState ambientOcclusion) {
		int index = blendMode.ordinal();
		index = index * 2 + (emissive ? 1 : 0);
		index = index * 2 + (disableDiffuse ? 1 : 0);
		return index * AO_COUNT + ambientOcclusion.ordinal();
	}

	public static RenderMaterial byIndex(int index) {
		return MATERIALS[index];
	}

	/**
	 * {@return the interned material for this exact combination}
	 */
	public static RenderMaterial find(BlendMode blendMode, boolean emissive, boolean disableDiffuse, TriState ambientOcclusion) {
		return MATERIALS[index(blendMode, emissive, disableDiffuse, ambientOcclusion)];
	}

	public MaterialFinder clear() {
		blendMode = BlendMode.DEFAULT;
		emissive = false;
		disableDiffuse = false;
		ambientOcclusion = TriState.DEFAULT;
		return this;
	}

	public MaterialFinder blendMode(BlendMode blendMode) {
		this.blendMode = blendMode;
		return this;
	}

	public MaterialFinder emissive(boolean emissive) {
		this.emissive = emissive;
		return this;
	}

	public MaterialFinder disableDiffuse(boolean disableDiffuse) {
		this.disableDiffuse = disableDiffuse;
		return this;
	}

	public MaterialFinder ambientOcclusion(TriState ambientOcclusion) {
		this.ambientOcclusion = ambientOcclusion;
		return this;
	}

	public MaterialFinder copyFrom(RenderMaterial material) {
		blendMode = material.blendMode();
		emissive = material.emissive();
		disableDiffuse = material.disableDiffuse();
		ambientOcclusion = material.ambientOcclusion();
		return this;
	}

	public RenderMaterial find() {
		return MATERIALS[index(blendMode, emissive, disableDiffuse, ambientOcclusion)];
	}
}
