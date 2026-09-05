package me.pepperbell.continuity.client.model.blockstatemodel;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.model.ModelObjectsContainer;
import me.pepperbell.continuity.client.model.QuadProcessors;
import me.pepperbell.continuity.client.render.MutableQuad;
import me.pepperbell.continuity.client.render.QuadCollection;
import me.pepperbell.continuity.client.render.QuadCollector;
import me.pepperbell.continuity.impl.client.ProcessingContextImpl;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Applies connected textures to a block model, from 1.21.5 onwards.
 *
 * <p>This is the same processing the older {@code CtmBakedModel} performs, expressed against the model API that
 * replaced baked models. NeoForge hands the level and position straight to {@code collectParts}, so the results no
 * longer have to be routed through model data: the parts this returns are what the chunk builder draws.
 *
 * <p>A part carries one render type, so overlays that ask for a different layer come back as additional parts
 * rather than as extra render types on a single one.
 */
public class CtmBlockStateModel extends ForwardingBlockStateModel {
	public static final int PASSES = 4;

	private static final Direction[] DIRECTIONS = Direction.values();

	protected final BlockState defaultState;
	protected volatile Function<TextureAtlasSprite, QuadProcessors.Slice> defaultSliceFunc;

	public CtmBlockStateModel(BlockStateModel wrapped, BlockState defaultState) {
		super(wrapped);
		this.defaultState = defaultState;
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
		if (!ContinuityConfig.INSTANCE.connectedTextures.get()) {
			super.collectParts(level, pos, state, random, parts);
			return;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getConnectedTexturesState().isEnabled()) {
			super.collectParts(level, pos, state, random, parts);
			return;
		}

		CtmQuadTransform quadTransform = container.ctmQuadTransform;
		if (quadTransform.isActive()) {
			super.collectParts(level, pos, state, random, parts);
			return;
		}

		List<BlockModelPart> sourceParts = new ObjectArrayList<>();
		super.collectParts(level, pos, state, random, sourceParts);
		if (sourceParts.isEmpty()) {
			return;
		}

		// See CtmBakedModel for why only the first constant of the enum is queried here.
		BlockState appearanceState = state.getAppearance(level, pos, Direction.DOWN, state, pos);

		QuadCollection processed = process(level, pos, state, appearanceState, random, sourceParts, container, quadTransform);
		if (processed == null) {
			parts.addAll(sourceParts);
			return;
		}

		TriState ambientOcclusion = sourceParts.get(0).ambientOcclusion();
		TextureAtlasSprite particleIcon = sourceParts.get(0).particleIcon();
		for (Map.Entry<RenderType, List<BakedQuad>[]> entry : processed.byLayer().entrySet()) {
			parts.add(new ProcessedBlockModelPart(entry.getValue(), entry.getKey(), ambientOcclusion, particleIcon));
		}
	}

	@Nullable
	private QuadCollection process(BlockAndTintGetter level, BlockPos pos, BlockState state, BlockState appearanceState, RandomSource random, List<BlockModelPart> sourceParts, ModelObjectsContainer container, CtmQuadTransform quadTransform) {
		Function<TextureAtlasSprite, QuadProcessors.Slice> sliceFunc = getSliceFunc(appearanceState);

		long seed = state.getSeed(pos);
		Supplier<RandomSource> randomSupplier = () -> {
			random.setSeed(seed);
			return random;
		};

		QuadCollector collector = container.ctmQuadCollector;
		MutableQuad workingQuad = container.workingQuad;
		collector.reset();

		quadTransform.prepare(level, appearanceState, state, pos, randomSupplier, false, sliceFunc);

		boolean processedAnything = false;
		try {
			int partCount = sourceParts.size();
			for (int p = 0; p < partCount; p++) {
				BlockModelPart part = sourceParts.get(p);
				collector.prepare(part.getRenderType(state));

				for (int i = 0; i <= DIRECTIONS.length; i++) {
					Direction cullFace = i == DIRECTIONS.length ? null : DIRECTIONS[i];

					List<BakedQuad> quads = part.getQuads(cullFace);
					int amount = quads.size();
					for (int j = 0; j < amount; j++) {
						BakedQuad quad = quads.get(j);
						workingQuad.fromVanilla(quad, cullFace);

						if (quadTransform.transform(workingQuad)) {
							if (workingQuad.isDirty()) {
								collector.acceptVanilla(workingQuad.toBakedQuad(), workingQuad.cullFace(), part.getRenderType(state));
								processedAnything = true;
							} else {
								collector.acceptVanilla(quad, cullFace, part.getRenderType(state));
							}
						} else {
							processedAnything = true;
						}
					}
				}
			}

			collector.prepare(ItemBlockRenderTypes.getChunkRenderType(state));
			quadTransform.processingContext.outputTo(collector);
			processedAnything |= quadTransform.processingContext.producedQuads();
		} finally {
			quadTransform.reset();
		}

		if (!processedAnything) {
			collector.reset();
			return null;
		}

		return collector.build();
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
