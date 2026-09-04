package me.pepperbell.continuity.client.model;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.render.BlendMode;
import me.pepperbell.continuity.client.render.ForwardingBakedModel;
import me.pepperbell.continuity.client.render.MaterialFinder;
import me.pepperbell.continuity.client.render.MutableQuad;
import me.pepperbell.continuity.client.render.QuadCollection;
import me.pepperbell.continuity.client.render.QuadCollector;
import me.pepperbell.continuity.client.render.RenderMaterial;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;

/**
 * Adds a full-bright copy of any quad whose texture has an emissive counterpart.
 *
 * <p>NeoForge carries no per-quad light override, so the copies are emitted as {@code EmissiveBakedQuad} and the light
 * value is forced when they reach the renderer.
 */
public class EmissiveBakedModel extends ForwardingBakedModel {
	/**
	 * Where the block quads for one position, emissive copies included, are handed to {@link #getQuads}.
	 */
	public static final ModelProperty<QuadCollection> EMISSIVE_QUADS = new ModelProperty<>();

	protected static final RenderMaterial[] EMISSIVE_MATERIALS;
	protected static final RenderMaterial DEFAULT_EMISSIVE_MATERIAL;
	protected static final RenderMaterial CUTOUT_MIPPED_EMISSIVE_MATERIAL;

	private static final Direction[] DIRECTIONS = Direction.values();

	static {
		BlendMode[] blendModes = BlendMode.VALUES;
		EMISSIVE_MATERIALS = new RenderMaterial[blendModes.length];
		for (BlendMode blendMode : blendModes) {
			EMISSIVE_MATERIALS[blendMode.ordinal()] = MaterialFinder.find(blendMode, true, true, TriState.FALSE);
		}

		DEFAULT_EMISSIVE_MATERIAL = EMISSIVE_MATERIALS[BlendMode.DEFAULT.ordinal()];
		CUTOUT_MIPPED_EMISSIVE_MATERIAL = EMISSIVE_MATERIALS[BlendMode.CUTOUT_MIPPED.ordinal()];
	}

	public EmissiveBakedModel(BakedModel wrapped) {
		super(wrapped);
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
		ModelData data = super.getModelData(level, pos, state, modelData);

		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			return data;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getEmissiveTexturesState().isEnabled()) {
			return data;
		}

		EmissiveBlockQuadTransform quadTransform = container.emissiveBlockQuadTransform;
		if (quadTransform.isActive()) {
			return data;
		}

		QuadCollection processed = process(state, data, container, quadTransform);
		if (processed == null) {
			return data;
		}
		return data.derive().with(EMISSIVE_QUADS, processed).build();
	}

	@Nullable
	private QuadCollection process(BlockState state, ModelData data, ModelObjectsContainer container, EmissiveBlockQuadTransform quadTransform) {
		RandomSource random = RandomSource.create();

		ChunkRenderTypeSet baseRenderTypes = originalModel.getRenderTypes(state, random, data);
		if (baseRenderTypes.isEmpty()) {
			return null;
		}

		QuadCollector collector = container.emissiveQuadCollector;
		MutableQuad workingQuad = container.emissiveWorkingQuad;
		collector.reset();

		quadTransform.prepare(collector, state);

		try {
			for (RenderType renderType : baseRenderTypes) {
				collector.prepare(renderType);

				for (int i = 0; i <= DIRECTIONS.length; i++) {
					Direction cullFace = i == DIRECTIONS.length ? null : DIRECTIONS[i];

					List<BakedQuad> quads = originalModel.getQuads(state, cullFace, random, data, renderType);
					int amount = quads.size();
					for (int j = 0; j < amount; j++) {
						BakedQuad quad = quads.get(j);
						collector.acceptVanilla(quad, cullFace, renderType);

						workingQuad.fromVanilla(quad, cullFace);
						quadTransform.transform(workingQuad, renderType);
					}
				}
			}
		} finally {
			quadTransform.reset();
		}

		if (!quadTransform.didEmit()) {
			collector.reset();
			return null;
		}

		return collector.build();
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
		QuadCollection processed = data.get(EMISSIVE_QUADS);
		if (processed != null) {
			return processed.getRenderTypes();
		}
		return super.getRenderTypes(state, rand, data);
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
		QuadCollection processed = data.get(EMISSIVE_QUADS);
		if (processed != null) {
			return processed.getQuads(side, renderType);
		}
		return super.getQuads(state, side, rand, data, renderType);
	}

	/**
	 * Item rendering has no model data to hang results on, so the emissive copies are appended here instead. Items are
	 * drawn one at a time rather than batched into a chunk, so the cost of doing it per call is acceptable.
	 */
	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
		List<BakedQuad> quads = super.getQuads(state, side, rand);

		if (state != null || quads.isEmpty() || !ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			return quads;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getEmissiveTexturesState().isEnabled()) {
			return quads;
		}

		EmissiveItemQuadTransform quadTransform = container.emissiveItemQuadTransform;
		if (quadTransform.isActive()) {
			return quads;
		}

		MutableQuad workingQuad = container.emissiveWorkingQuad;
		List<BakedQuad> emissiveQuads = null;

		quadTransform.prepare();
		try {
			int amount = quads.size();
			for (int i = 0; i < amount; i++) {
				BakedQuad quad = quads.get(i);
				workingQuad.fromVanilla(quad, side);
				BakedQuad emissiveQuad = quadTransform.transform(workingQuad);
				if (emissiveQuad != null) {
					if (emissiveQuads == null) {
						emissiveQuads = new ObjectArrayList<>(quads);
					}
					emissiveQuads.add(emissiveQuad);
				}
			}
		} finally {
			quadTransform.reset();
		}

		return emissiveQuads == null ? quads : emissiveQuads;
	}

	@Nullable
	private static TextureAtlasSprite getEmissiveSprite(MutableQuad quad) {
		TextureAtlasSprite sprite = quad.sprite();
		if (sprite == null) {
			return null;
		}
		return EmissiveSpriteApi.get().getEmissiveSprite(sprite);
	}

	/**
	 * Emits a full-bright copy of every block quad whose texture has an emissive counterpart.
	 */
	public static class EmissiveBlockQuadTransform {
		protected QuadCollector collector;
		protected BlockState state;

		protected boolean active;
		protected boolean didEmit;
		protected boolean calculateDefaultLayer;
		protected boolean isDefaultLayerSolid;

		public void transform(MutableQuad quad, RenderType renderType) {
			TextureAtlasSprite sprite = quad.sprite();
			TextureAtlasSprite emissiveSprite = getEmissiveSprite(quad);
			if (emissiveSprite == null) {
				return;
			}

			// An emissive layer drawn over a solid one has to be at least cutout, or the alpha in the emissive
			// texture is ignored and the whole face lights up.
			BlendMode blendMode = BlendMode.fromRenderType(renderType);
			RenderMaterial emissiveMaterial;
			if (blendMode == BlendMode.DEFAULT) {
				if (calculateDefaultLayer) {
					isDefaultLayerSolid = ItemBlockRenderTypes.getChunkRenderType(state) == RenderType.solid();
					calculateDefaultLayer = false;
				}

				if (isDefaultLayerSolid) {
					emissiveMaterial = CUTOUT_MIPPED_EMISSIVE_MATERIAL;
				} else {
					emissiveMaterial = DEFAULT_EMISSIVE_MATERIAL;
				}
			} else if (blendMode == BlendMode.SOLID) {
				emissiveMaterial = CUTOUT_MIPPED_EMISSIVE_MATERIAL;
			} else {
				emissiveMaterial = EMISSIVE_MATERIALS[blendMode.ordinal()];
			}

			quad.material(emissiveMaterial);
			QuadUtil.interpolate(quad, sprite, emissiveSprite);
			quad.sprite(emissiveSprite);

			collector.copyFrom(quad);
			collector.emit();
			didEmit = true;
		}

		public boolean isActive() {
			return active;
		}

		public boolean didEmit() {
			return didEmit;
		}

		public void prepare(QuadCollector collector, BlockState state) {
			this.collector = collector;
			this.state = state;

			active = true;
			didEmit = false;
			calculateDefaultLayer = true;
			isDefaultLayerSolid = false;
		}

		public void reset() {
			collector = null;
			state = null;

			active = false;
		}
	}

	/**
	 * The item equivalent, which hands back the finished quad rather than emitting it.
	 */
	public static class EmissiveItemQuadTransform {
		protected boolean active;

		@Nullable
		public BakedQuad transform(MutableQuad quad) {
			TextureAtlasSprite sprite = quad.sprite();
			TextureAtlasSprite emissiveSprite = getEmissiveSprite(quad);
			if (emissiveSprite == null) {
				return null;
			}

			quad.material(DEFAULT_EMISSIVE_MATERIAL);
			QuadUtil.interpolate(quad, sprite, emissiveSprite);
			quad.sprite(emissiveSprite);
			return quad.toBakedQuad();
		}

		public boolean isActive() {
			return active;
		}

		public void prepare() {
			active = true;
		}

		public void reset() {
			active = false;
		}
	}
}
