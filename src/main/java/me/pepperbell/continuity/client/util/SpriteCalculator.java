package me.pepperbell.continuity.client.util;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
//? if <26.1
import net.minecraft.client.renderer.block.BlockModelShaper;
//? if <1.21.5
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

public final class SpriteCalculator {
	//? if <26.1 {
	private static final BlockModelShaper MODELS = Minecraft.getInstance().getModelManager().getBlockModelShaper();
	//?} else {
	/*private static final net.minecraft.client.renderer.block.BlockStateModelSet MODELS = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
	*///?}

	private static final EnumMap<Direction, SpriteCache> SPRITE_CACHES = new EnumMap<>(Direction.class);
	static {
		for (Direction direction : Direction.values()) {
			SPRITE_CACHES.put(direction, new SpriteCache(direction));
		}
	}

	public static TextureAtlasSprite getSprite(BlockState state, Direction face) {
		return SPRITE_CACHES.get(face).getSprite(state);
	}

	public static TextureAtlasSprite calculateSprite(BlockState state, Direction face, Supplier<RandomSource> randomSupplier) {
		//? if <1.21.5 {
		BakedModel model = MODELS.getBlockModel(state);
		//?} elif <26.1 {
		/*net.minecraft.client.renderer.block.model.BlockStateModel model = MODELS.getBlockModel(state);
		*///?} else
		/*net.minecraft.client.renderer.block.dispatch.BlockStateModel model = MODELS.get(state);*/
		try {
			//? if <1.21.5 {
			List<BakedQuad> quads = model.getQuads(state, face, randomSupplier.get());
			//?} else
			/*List<BakedQuad> quads = me.pepperbell.continuity.client.model.blockstatemodel.PartQuads.collect(model, randomSupplier.get(), face);*/
			if (!quads.isEmpty()) {
				//? if <1.21.5 {
				return quads.get(0).getSprite();
				//?} elif <26.1 {
				/*return quads.get(0).sprite();
				*///?} else
				/*return quads.get(0).materialInfo().sprite();*/
			}
			//? if <1.21.5 {
			quads = model.getQuads(state, null, randomSupplier.get());
			//?} else
			/*quads = me.pepperbell.continuity.client.model.blockstatemodel.PartQuads.collect(model, randomSupplier.get(), null);*/
			if (!quads.isEmpty()) {
				int amount = quads.size();
				for (int i = 0; i < amount; i++) {
					BakedQuad quad = quads.get(i);
					//? if <1.21.5 {
					if (quad.getDirection() == face) {
						return quad.getSprite();
					//?} elif <26.1 {
					/*if (quad.direction() == face) {
						return quad.sprite();
					*///?} else {
					/*if (quad.direction() == face) {
						return quad.materialInfo().sprite();
					*///?}
					}
				}
			}
		} catch (Exception e) {
			//
		}
		//? if <1.21.5 {
		return model.getParticleIcon();
		//?} elif <26.1 {
		/*return model.particleIcon();
		*///?} else
		/*return model.particleMaterial().sprite();*/
	}

	public static void clearCache() {
		for (SpriteCache cache : SPRITE_CACHES.values()) {
			cache.clear();
		}
	}

	private static class SpriteCache {
		private final Direction face;
		private final Reference2ReferenceOpenHashMap<BlockState, TextureAtlasSprite> sprites = new Reference2ReferenceOpenHashMap<>();
		private final Supplier<RandomSource> randomSupplier = new Supplier<>() {
			private final RandomSource random = RandomSource.create();

			@Override
			public RandomSource get() {
				// Use item rendering seed for consistency
				random.setSeed(42L);
				return random;
			}
		};
		private final StampedLock lock = new StampedLock();

		public SpriteCache(Direction face) {
			this.face = face;
		}

		public TextureAtlasSprite getSprite(BlockState state) {
			TextureAtlasSprite sprite;

			long optimisticReadStamp = lock.tryOptimisticRead();
			if (optimisticReadStamp != 0L) {
				try {
					// This map read could happen at the same time as a map write, so catch any exceptions.
					// This is safe due to the map implementation used, which is guaranteed to not mutate the map during
					// a read.
					sprite = sprites.get(state);
					if (sprite != null && lock.validate(optimisticReadStamp)) {
						return sprite;
					}
				} catch (Exception e) {
					//
				}
			}

			long readStamp = lock.readLock();
			try {
				sprite = sprites.get(state);
			} finally {
				lock.unlockRead(readStamp);
			}

			if (sprite == null) {
				long writeStamp = lock.writeLock();
				try {
					sprite = sprites.get(state);
					if (sprite == null) {
						sprite = calculateSprite(state, face, randomSupplier);
						sprites.put(state, sprite);
					}
				} finally {
					lock.unlockWrite(writeStamp);
				}
			}

			return sprite;
		}

		public void clear() {
			long writeStamp = lock.writeLock();
			try {
				sprites.clear();
			} finally {
				lock.unlockWrite(writeStamp);
			}
		}
	}
}
