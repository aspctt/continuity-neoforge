package me.pepperbell.continuity.client.render;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
//? if <1.21.5
import net.neoforged.neoforge.client.ChunkRenderTypeSet;

/**
 * The quads a model produced for one block, bucketed the way NeoForge asks for them: by render type, then by the face
 * they are culled against.
 *
 * <p>A {@code null} render type means the caller wants everything at once, which is what the block breaking overlay
 * and other non-standard render paths do.
 */
public final class QuadCollection {
	public static final int NO_CULL_FACE_INDEX = 6;
	public static final int BUCKET_COUNT = 7;

	private static final List<BakedQuad> EMPTY = List.of();

	private final Map<RenderType, List<BakedQuad>[]> byLayer;
	private final List<BakedQuad>[] all;
	//? if <1.21.5
	private final ChunkRenderTypeSet renderTypes;

	//? if <1.21.5 {
	QuadCollection(Map<RenderType, List<BakedQuad>[]> byLayer, List<BakedQuad>[] all, ChunkRenderTypeSet renderTypes) {
		this.byLayer = byLayer;
		this.all = all;
		this.renderTypes = renderTypes;
	}
	//?} else {
	/*QuadCollection(Map<RenderType, List<BakedQuad>[]> byLayer, List<BakedQuad>[] all) {
		this.byLayer = byLayer;
		this.all = all;
	}
	*///?}

	/**
	 * {@return the quads grouped by the render type they belong to}
	 */
	public Map<RenderType, List<BakedQuad>[]> byLayer() {
		return byLayer;
	}

	public static int bucketIndex(@Nullable Direction cullFace) {
		return cullFace == null ? NO_CULL_FACE_INDEX : cullFace.ordinal();
	}

	public List<BakedQuad> getQuads(@Nullable Direction cullFace, @Nullable RenderType layer) {
		List<BakedQuad>[] buckets;
		if (layer == null) {
			buckets = all;
		} else {
			buckets = byLayer.get(layer);
			if (buckets == null) {
				return EMPTY;
			}
		}

		List<BakedQuad> quads = buckets[bucketIndex(cullFace)];
		return quads == null ? EMPTY : quads;
	}

	//? if <1.21.5 {
	public ChunkRenderTypeSet getRenderTypes() {
		return renderTypes;
	}
	//?}
}
