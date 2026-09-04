package me.pepperbell.continuity.client.render;

//? if <1.21.5
import me.pepperbell.continuity.client.model.bakedmodel.EmissiveBakedQuad;
import net.minecraft.client.renderer.block.model.BakedQuad;

/**
 * Tells the renderer which quads Continuity added for emissive textures.
 *
 * <p>How that is marked depends on the version. Before 1.21.5 a marker subclass carries it. From 1.21.5 BakedQuad
 * is a record and cannot be extended, so the light emission the record already carries is used instead. Nothing in
 * the game reads that field for rendering, so it is free to carry the intent.
 */
public final class EmissiveQuads {
	/**
	 * The light emission value used to mark a quad as emissive from 1.21.5.
	 */
	public static final int EMISSIVE_LIGHT = 15;

	private EmissiveQuads() {
	}

	public static boolean isEmissive(BakedQuad quad) {
		//? if <1.21.5 {
		return quad instanceof EmissiveBakedQuad;
		//?} else
		/*return quad.lightEmission() >= EMISSIVE_LIGHT;*/
	}
}
