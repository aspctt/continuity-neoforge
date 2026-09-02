package me.pepperbell.continuity.client.render;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;

/**
 * Terminal emitter that bakes everything it receives into a {@link QuadCollection}.
 *
 * <p>Quads that no processor touched are forwarded by reference through {@link #acceptVanilla}, so a block with
 * connected textures on one face does not pay to rebuild the other five.
 */
public final class QuadCollector extends AbstractQuadEmitter {
	private final Reference2ReferenceLinkedOpenHashMap<RenderType, List<BakedQuad>[]> byRenderType = new Reference2ReferenceLinkedOpenHashMap<>();
	private final List<BakedQuad>[] all = createBuckets();

	private RenderType defaultRenderType = RenderType.solid();

	@SuppressWarnings("unchecked")
	private static List<BakedQuad>[] createBuckets() {
		return new List[QuadCollection.BUCKET_COUNT];
	}

	/**
	 * Sets the render type that {@link BlendMode#DEFAULT} resolves to, which is the layer the block itself uses.
	 */
	public void prepare(RenderType defaultRenderType) {
		this.defaultRenderType = defaultRenderType;
	}

	/**
	 * Adds an already baked quad without copying it.
	 */
	public void acceptVanilla(BakedQuad quad, @Nullable Direction cullFace, RenderType renderType) {
		add(quad, cullFace, renderType);
	}

	@Override
	protected void emitDirectly() {
		RenderType renderType = material().blendMode().getRenderType();
		if (renderType == null) {
			renderType = defaultRenderType;
		}
		add(toBakedQuad(), cullFace(), renderType);
	}

	private void add(BakedQuad quad, @Nullable Direction cullFace, RenderType renderType) {
		int index = QuadCollection.bucketIndex(cullFace);

		List<BakedQuad>[] buckets = byRenderType.get(renderType);
		if (buckets == null) {
			buckets = createBuckets();
			byRenderType.put(renderType, buckets);
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
		return byRenderType.isEmpty();
	}

	/**
	 * {@return everything collected so far, and resets this collector}
	 */
	public QuadCollection build() {
		ChunkRenderTypeSet renderTypes = ChunkRenderTypeSet.of(byRenderType.keySet().toArray(RenderType[]::new));
		QuadCollection collection = new QuadCollection(new Reference2ReferenceLinkedOpenHashMap<>(byRenderType), all.clone(), renderTypes);
		reset();
		return collection;
	}

	public void reset() {
		byRenderType.clear();
		for (int i = 0; i < all.length; i++) {
			all[i] = null;
		}
		clear();
	}
}
