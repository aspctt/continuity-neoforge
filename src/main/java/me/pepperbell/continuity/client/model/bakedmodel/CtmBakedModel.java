package me.pepperbell.continuity.client.model.bakedmodel;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.model.QuadProcessors;
import me.pepperbell.continuity.client.model.ModelObjectsContainer;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.render.MutableQuad;
import me.pepperbell.continuity.client.render.QuadCollection;
import me.pepperbell.continuity.client.render.QuadCollector;
import me.pepperbell.continuity.impl.client.ProcessingContextImpl;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * Applies connected textures to a block model.
 *
 * <p>All of the work happens in {@link #getModelData}, which NeoForge calls once per block per chunk rebuild with the
 * level and position in hand. The resulting quads are stashed on the model data, and the later {@code getRenderTypes}
 * and {@code getQuads} calls just read them back. Doing it this way means no thread local has to be smuggled past
 * {@code getQuads}, and it works under any chunk renderer that honours the NeoForge model pipeline.
 */
public class CtmBakedModel extends ForwardingBakedModel {
	public static final int PASSES = 4;

	/**
	 * Where the processed quads for one block are handed from {@link #getModelData} to {@link #getQuads}.
	 */
	public static final ModelProperty<QuadCollection> PROCESSED_QUADS = new ModelProperty<>();

	private static final Direction[] DIRECTIONS = Direction.values();

	protected final BlockState defaultState;
	protected volatile Function<TextureAtlasSprite, QuadProcessors.Slice> defaultSliceFunc;

	public CtmBakedModel(BakedModel wrapped, BlockState defaultState) {
		super(wrapped);
		this.defaultState = defaultState;
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
		ModelData data = super.getModelData(level, pos, state, modelData);

		if (!ContinuityConfig.INSTANCE.connectedTextures.get()) {
			return data;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getConnectedTexturesState().isEnabled()) {
			return data;
		}

		CtmQuadTransform quadTransform = container.ctmQuadTransform;
		if (quadTransform.isActive()) {
			// A wrapped model asked us to build its data while we were already building ours. Leave it to the
			// outer call.
			return data;
		}

		// The correct way to get the appearance of the origin state is to call getAppearance on the state actually in
		// the world, passing the adjacent block as the source. Only the first constant of the enum is queried: asking
		// for all six sides would be more correct but costs six lookups per block, and the appearance state has to be
		// resolved before a slice can be chosen because the slice decides which processors run at all.
		BlockState appearanceState = state.getAppearance(level, pos, Direction.DOWN, state, pos);

		QuadCollection processed = process(level, pos, state, appearanceState, data, container, quadTransform);
		if (processed == null) {
			return data;
		}
		return data.derive().with(PROCESSED_QUADS, processed).build();
	}

	@Nullable
	private QuadCollection process(BlockAndTintGetter level, BlockPos pos, BlockState state, BlockState appearanceState, ModelData data, ModelObjectsContainer container, CtmQuadTransform quadTransform) {
		Function<TextureAtlasSprite, QuadProcessors.Slice> sliceFunc = getSliceFunc(appearanceState);

		RandomSource random = RandomSource.create();
		long seed = state.getSeed(pos);
		Supplier<RandomSource> randomSupplier = () -> {
			random.setSeed(seed);
			return random;
		};

		random.setSeed(seed);
		ChunkRenderTypeSet baseRenderTypes = originalModel.getRenderTypes(state, random, data);
		if (baseRenderTypes.isEmpty()) {
			return null;
		}

		boolean useManualCulling = ContinuityConfig.INSTANCE.useManualCulling.get();
		QuadCollector collector = container.ctmQuadCollector;
		MutableQuad workingQuad = container.workingQuad;
		collector.reset();

		quadTransform.prepare(level, appearanceState, state, pos, randomSupplier, useManualCulling, sliceFunc);

		boolean processedAnything = false;
		try {
			BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

			for (RenderType layer : baseRenderTypes) {
				collector.prepare(layer);

				for (int i = 0; i <= DIRECTIONS.length; i++) {
					Direction cullFace = i == DIRECTIONS.length ? null : DIRECTIONS[i];

					if (useManualCulling && cullFace != null) {
						neighborPos.setWithOffset(pos, cullFace);
						//? if <1.21.2 {
						if (!Block.shouldRenderFace(state, level, pos, cullFace, neighborPos)) {
						//?} else
						/*if (!Block.shouldRenderFace(level, pos, state, level.getBlockState(neighborPos), cullFace)) {*/
							continue;
						}
					}

					random.setSeed(seed);
					List<BakedQuad> quads = originalModel.getQuads(state, cullFace, random, data, layer);
					int amount = quads.size();
					for (int j = 0; j < amount; j++) {
						BakedQuad quad = quads.get(j);
						workingQuad.fromVanilla(quad, cullFace);

						if (quadTransform.transform(workingQuad)) {
							if (workingQuad.isDirty()) {
								collector.acceptVanilla(workingQuad.toBakedQuad(), workingQuad.cullFace(), layer);
								processedAnything = true;
							} else {
								collector.acceptVanilla(quad, cullFace, layer);
							}
						} else {
							processedAnything = true;
						}
					}
				}
			}

			// Overlays and any other geometry the processors added go out in the layer the block itself uses, unless
			// they asked for a specific one.
			collector.prepare(ItemBlockRenderTypes.getChunkRenderType(state));
			quadTransform.processingContext.outputTo(collector);
			processedAnything |= quadTransform.processingContext.producedQuads();
		} finally {
			quadTransform.reset();
		}

		if (!processedAnything) {
			// Nothing changed, so let the wrapped model answer getQuads directly and skip the copies entirely.
			// Manual culling has already paid off by this point: it kept the processors off faces that are hidden.
			collector.reset();
			return null;
		}

		return collector.build();
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		QuadCollection processed = data.get(PROCESSED_QUADS);
		if (processed != null) {
			return processed.getRenderTypes();
		}
		return super.getRenderTypes(state, rand, data);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType layer) {
		QuadCollection processed = data.get(PROCESSED_QUADS);
		if (processed != null) {
			return processed.getQuads(side, layer);
		}
		return super.getQuads(state, side, rand, data, layer);
	}

	protected Function<TextureAtlasSprite, QuadProcessors.Slice> getSliceFunc(BlockState state) {
		if (state == defaultState) {
			Function<TextureAtlasSprite, QuadProcessors.Slice> sliceFunc = defaultSliceFunc;
			if (sliceFunc == null) {
				synchronized (this) {
					sliceFunc = defaultSliceFunc;
					if (sliceFunc == null) {
						sliceFunc = QuadProcessors.getCache(state);
						defaultSliceFunc = sliceFunc;
					}
				}
			}
			return sliceFunc;
		}
		return QuadProcessors.getCache(state);
	}

	/**
	 * Runs the processor chain over one quad at a time.
	 */
	public static class CtmQuadTransform {
		public final ProcessingContextImpl processingContext = new ProcessingContextImpl();

		protected BlockAndTintGetter blockView;
		protected BlockState appearanceState;
		protected BlockState state;
		protected BlockPos pos;
		protected Supplier<RandomSource> randomSupplier;
		protected boolean useManualCulling;
		protected Function<TextureAtlasSprite, QuadProcessors.Slice> sliceFunc;

		protected boolean active;

		/**
		 * {@return false if the quad should be dropped}
		 */
		public boolean transform(MutableQuad quad) {
			for (int pass = 0; pass < PASSES; pass++) {
				Boolean result = transformOnce(quad, pass);
				if (result != null) {
					return result;
				}
			}

			return true;
		}

		@Nullable
		protected Boolean transformOnce(MutableQuad quad, int pass) {
			TextureAtlasSprite sprite = quad.sprite();
			if (sprite == null) {
				return true;
			}

			QuadProcessors.Slice slice = sliceFunc.apply(sprite);
			QuadProcessor[] processors = pass == 0 ? slice.processors() : slice.multipassProcessors();
			for (QuadProcessor processor : processors) {
				QuadProcessor.ProcessingResult result = processor.processQuad(quad, sprite, blockView, appearanceState, state, pos, randomSupplier, pass, processingContext);
				if (result == QuadProcessor.ProcessingResult.NEXT_PROCESSOR) {
					continue;
				}
				if (result == QuadProcessor.ProcessingResult.NEXT_PASS) {
					return null;
				}
				if (result == QuadProcessor.ProcessingResult.STOP) {
					return true;
				}
				if (result == QuadProcessor.ProcessingResult.DISCARD) {
					return false;
				}
			}
			return true;
		}

		public boolean isActive() {
			return active;
		}

		public void prepare(BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, boolean useManualCulling, Function<TextureAtlasSprite, QuadProcessors.Slice> sliceFunc) {
			this.blockView = blockView;
			this.appearanceState = appearanceState;
			this.state = state;
			this.pos = pos;
			this.randomSupplier = randomSupplier;
			this.useManualCulling = useManualCulling;
			this.sliceFunc = sliceFunc;

			active = true;

			processingContext.prepare();
		}

		public void reset() {
			blockView = null;
			appearanceState = null;
			state = null;
			pos = null;
			randomSupplier = null;
			useManualCulling = false;
			sliceFunc = null;

			active = false;

			processingContext.reset();
		}
	}
}
