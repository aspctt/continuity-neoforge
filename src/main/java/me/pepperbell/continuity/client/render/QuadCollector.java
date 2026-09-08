package me.pepperbell.continuity.client.render;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
//? if <1.21.5
import net.neoforged.neoforge.client.ChunkRenderTypeSet;

/**
 * Terminal emitter that bakes everything it receives into a {@link QuadCollection}.
 *
 * <p>Quads that no processor touched are forwarded by reference through {@link #acceptVanilla}, so a block with
 * connected textures on one face does not pay to rebuild the other five.
 */
public final class QuadCollector extends AbstractQuadEmitter {
	private final Reference2ReferenceLinkedOpenHashMap<RenderType, List<BakedQuad>[]> byLayer = new Reference2ReferenceLinkedOpenHashMap<>();
	private final List<BakedQuad>[] all = createBuckets();

	private RenderType defaultLayer = RenderType.solid();

	@SuppressWarnings("unchecked")
	private static List<BakedQuad>[] createBuckets() {
		return new List[QuadCollection.BUCKET_COUNT];
	}

	/**
	 * Sets the render type that {@link BlendMode#DEFAULT} resolves to, which is the layer the block itself uses.
	 */
	public void prepare(RenderType defaultLayer) {
		this.defaultLayer = defaultLayer;
	}

	/**
	 * Adds an already baked quad without copying it.
	 */
	public void acceptVanilla(BakedQuad quad, @Nullable Direction cullFace, RenderType layer) {
		add(quad, cullFace, layer);
	}

	@Override
	protected void emitDirectly() {
		//? if <26.1 {
		RenderType layer = material().blendMode().getLayer();
		if (layer == null) {
			layer = defaultLayer;
		}
		add(toBakedQuad(), cullFace(), layer);
		//?} else {
		/*// From 26.1 the layer belongs to the quad, so a quad copied from another keeps the layer it came from
		// and only a quad with no history at all falls back to the one this collector was prepared with.
		ChunkSectionLayer resolved = material().blendMode().getLayer();
		if (resolved == null) {
			resolved = this.layer != null ? this.layer : defaultLayer;
		}
		layer(resolved);
		add(toBakedQuad(), cullFace(), resolved);
		*///?}
	}

	private void add(BakedQuad quad, @Nullable Direction cullFace, RenderType layer) {
		int index = QuadCollection.bucketIndex(cullFace);

		List<BakedQuad>[] buckets = byLayer.get(layer);
		if (buckets == null) {
			buckets = createBuckets();
			byLayer.put(layer, buckets);
		}
		bucket(buckets, index).add(quad);
		bucket(all, index).add(quad);
	}

	private static List<BakedQuad> bucket(List<BakedQuad>[] buckets, int index) {
		List<BakedQuad> bucket = buckets[index];
		if (bucket == null) {
			bucket = new ObjectArrayList<>();
			buckets[index] = bucket;
		}
		return bucket;
	}

	public boolean isEmpty() {
		return byLayer.isEmpty();
	}

	/**
	 * {@return everything collected so far, and resets this collector}
	 */
	public QuadCollection build() {
		//? if <1.21.5 {
		ChunkRenderTypeSet renderTypes = ChunkRenderTypeSet.of(byLayer.keySet().toArray(RenderType[]::new));
		QuadCollection collection = new QuadCollection(new Reference2ReferenceLinkedOpenHashMap<>(byLayer), all.clone(), renderTypes);
		//?} else
		/*QuadCollection collection = new QuadCollection(new Reference2ReferenceLinkedOpenHashMap<>(byLayer), all.clone());*/
		reset();
		return collection;
	}

	public void reset() {
		byLayer.clear();
		for (int i = 0; i < all.length; i++) {
			all[i] = null;
		}
		clear();
	}
}
