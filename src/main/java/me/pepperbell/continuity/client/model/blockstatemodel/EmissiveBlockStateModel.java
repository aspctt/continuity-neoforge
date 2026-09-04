package me.pepperbell.continuity.client.model.blockstatemodel;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.model.ModelObjectsContainer;
import me.pepperbell.continuity.client.render.BlendMode;
import me.pepperbell.continuity.client.render.MaterialFinder;
import me.pepperbell.continuity.client.render.MutableQuad;
import me.pepperbell.continuity.client.render.QuadCollection;
import me.pepperbell.continuity.client.render.QuadCollector;
import me.pepperbell.continuity.client.render.RenderMaterial;
import me.pepperbell.continuity.client.util.QuadUtil;
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
 * Adds a full-bright copy of any quad whose texture has an emissive counterpart, from 1.21.5 onwards.
 *
 * <p>Only block models are covered. Item models became a separate system in 1.21.4 and are not wrapped here, so
 * emissive textures on items are not applied on this band.
 */
public class EmissiveBlockStateModel extends ForwardingBlockStateModel {
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

	public EmissiveBlockStateModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts) {
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			super.collectParts(level, pos, state, random, parts);
			return;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getEmissiveTexturesState().isEnabled()) {
			super.collectParts(level, pos, state, random, parts);
			return;
		}

		List<BlockModelPart> sourceParts = new ObjectArrayList<>();
		super.collectParts(level, pos, state, random, sourceParts);
		if (sourceParts.isEmpty()) {
			return;
		}

		parts.addAll(sourceParts);

		QuadCollection emissive = collectEmissive(state, sourceParts, container);
		if (emissive == null) {
			return;
		}

		TriState ambientOcclusion = TriState.FALSE;
		TextureAtlasSprite particleIcon = sourceParts.get(0).particleIcon();
		for (Map.Entry<RenderType, List<BakedQuad>[]> entry : emissive.byRenderType().entrySet()) {
			parts.add(new ProcessedBlockModelPart(entry.getValue(), entry.getKey(), ambientOcclusion, particleIcon));
		}
	}

	@Nullable
	private QuadCollection collectEmissive(BlockState state, List<BlockModelPart> sourceParts, ModelObjectsContainer container) {
		QuadCollector collector = container.emissiveQuadCollector;
		MutableQuad workingQuad = container.emissiveWorkingQuad;
		collector.reset();

		boolean emitted = false;
		int partCount = sourceParts.size();
		for (int p = 0; p < partCount; p++) {
			BlockModelPart part = sourceParts.get(p);
			RenderType renderType = part.getRenderType(state);
			collector.prepare(renderType);

			for (int i = 0; i <= DIRECTIONS.length; i++) {
				Direction cullFace = i == DIRECTIONS.length ? null : DIRECTIONS[i];

				List<BakedQuad> quads = part.getQuads(cullFace);
				int amount = quads.size();
				for (int j = 0; j < amount; j++) {
					workingQuad.fromVanilla(quads.get(j), cullFace);

					TextureAtlasSprite sprite = workingQuad.sprite();
					TextureAtlasSprite emissiveSprite = sprite == null ? null : EmissiveSpriteApi.get().getEmissiveSprite(sprite);
					if (emissiveSprite == null) {
						continue;
					}

					// An emissive layer drawn over a solid one has to be at least cutout, or the alpha in the
					// emissive texture is ignored and the whole face lights up.
					BlendMode blendMode = BlendMode.fromRenderType(renderType);
					RenderMaterial emissiveMaterial;
					if (blendMode == BlendMode.DEFAULT || blendMode == BlendMode.SOLID) {
						emissiveMaterial = CUTOUT_MIPPED_EMISSIVE_MATERIAL;
					} else {
						emissiveMaterial = EMISSIVE_MATERIALS[blendMode.ordinal()];
					}

					workingQuad.material(emissiveMaterial);
					QuadUtil.interpolate(workingQuad, sprite, emissiveSprite);
					workingQuad.sprite(emissiveSprite);

					collector.copyFrom(workingQuad);
					collector.emit();
					emitted = true;
				}
			}
		}

		if (!emitted) {
			collector.reset();
			return null;
		}

		return collector.build();
	}
}
