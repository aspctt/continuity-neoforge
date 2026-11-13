package me.pepperbell.continuity.client.render;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;

/**
 * Defines how a quad is blended when it is rendered. Mirrors the OptiFine "layer" property and the blend modes of the
 * Fabric Rendering API so that resource packs written for either behave identically.
 *
 * <p>On NeoForge every blend mode maps onto one of the chunk render types. {@link #DEFAULT} means "whatever layer the
 * block itself uses", which can only be resolved once the block state is known.
 */
public enum BlendMode {
	DEFAULT,
	SOLID,
	CUTOUT_MIPPED,
	CUTOUT,
	TRANSLUCENT;

	public static final BlendMode[] VALUES = values();

	/**
	 * {@return the render type this blend mode maps to, or {@code null} for {@link #DEFAULT}}
	 */
	@Nullable
	public RenderType getLayer() {
		return switch (this) {
			case DEFAULT -> null;
			case SOLID -> RenderType.solid();
			// The two cutout layers merged in 1.21.11 into a single mipped one. A directive rather than a
			// replacement, because a replacement that folds two names into one cannot be undone when Stonecutter
			// switches back, and would turn every mipped layer into the plain one.
			//? if <1.21.11 {
			case CUTOUT_MIPPED -> RenderType.cutoutMipped();
			//?} else
			//case CUTOUT_MIPPED -> RenderType.cutout();
			case CUTOUT -> RenderType.cutout();
			case TRANSLUCENT -> RenderType.translucent();
		};
	}

	public static BlendMode fromLayer(@Nullable RenderType layer) {
		if (layer == null) {
			return DEFAULT;
		}
		if (layer == RenderType.solid()) {
			return SOLID;
		}
		//? if <1.21.11 {
		if (layer == RenderType.cutoutMipped()) {
			return CUTOUT_MIPPED;
		}
		//?}
		if (layer == RenderType.cutout()) {
			//? if <1.21.11 {
			return CUTOUT;
			//?} else
			//return CUTOUT_MIPPED; // The two cutout layers merged, and what is left behaves like the mipped one.
		}
		if (layer == RenderType.translucent()) {
			return TRANSLUCENT;
		}
		return DEFAULT;
	}
}
