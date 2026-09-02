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
	public RenderType getRenderType() {
		return switch (this) {
			case DEFAULT -> null;
			case SOLID -> RenderType.solid();
			case CUTOUT_MIPPED -> RenderType.cutoutMipped();
			case CUTOUT -> RenderType.cutout();
			case TRANSLUCENT -> RenderType.translucent();
		};
	}

	public static BlendMode fromRenderType(@Nullable RenderType renderType) {
		if (renderType == null) {
			return DEFAULT;
		}
		if (renderType == RenderType.solid()) {
			return SOLID;
		}
		if (renderType == RenderType.cutoutMipped()) {
			return CUTOUT_MIPPED;
		}
		if (renderType == RenderType.cutout()) {
			return CUTOUT;
		}
		if (renderType == RenderType.translucent()) {
			return TRANSLUCENT;
		}
		return DEFAULT;
	}
}
