package me.pepperbell.continuity.client.render;

/**
 * Converts between the ARGB colours used throughout Continuity and the ABGR byte order that the vanilla
 * {@code DefaultVertexFormat.BLOCK} vertex format stores.
 */
public final class ColorHelper {
	private ColorHelper() {
	}

	/**
	 * Swaps the red and blue channels, converting ARGB to ABGR or the other way around.
	 */
	public static int swapRedBlue(int color) {
		return (color & 0xFF00FF00) | ((color & 0x00FF0000) >>> 16) | ((color & 0x000000FF) << 16);
	}

	public static int argbToVanilla(int argb) {
		return swapRedBlue(argb);
	}

	public static int vanillaToArgb(int abgr) {
		return swapRedBlue(abgr);
	}
}
