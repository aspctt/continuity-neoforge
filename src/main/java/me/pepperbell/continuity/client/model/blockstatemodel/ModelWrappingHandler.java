package me.pepperbell.continuity.client.model.blockstatemodel;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.model.ForeignModelFilter;
import me.pepperbell.continuity.client.model.QuadProcessors;
import me.pepperbell.continuity.client.render.MutableQuad;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
//? if <26.1
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wraps block state models so that connected and emissive textures apply to them.
 *
 * <p>From 1.21.5 the baking result is keyed by block state, so unlike the older band there is no model identifier to
 * resolve back to a state first.
 */
public class ModelWrappingHandler {
	private static final Direction[] DIRECTIONS = Direction.values();
	//? if <26.1 {
	private static final BlockAndTintGetter EMPTY_LEVEL = EmptyBlockAndTintGetter.INSTANCE;
	//?} else
	/*private static final BlockAndTintGetter EMPTY_LEVEL = BlockAndTintGetter.EMPTY;*/

	private final boolean wrapCtm;
	private final boolean wrapEmissive;
	private final ForeignModelFilter foreignModelFilter;
	private final MutableQuad workingQuad = new MutableQuad();

	private ModelWrappingHandler(List<QuadProcessors.ProcessorHolder> processorHolders, boolean wrapEmissive) {
		wrapCtm = !processorHolders.isEmpty();
		this.wrapEmissive = wrapEmissive;
		foreignModelFilter = new ForeignModelFilter(processorHolders);
	}

	@Nullable
	public static ModelWrappingHandler create(List<QuadProcessors.ProcessorHolder> processorHolders, boolean wrapEmissive) {
		if (processorHolders.isEmpty() && !wrapEmissive) {
			return null;
		}
		return new ModelWrappingHandler(processorHolders, wrapEmissive);
	}

	public BlockStateModel wrap(BlockStateModel model, BlockState state) {
		boolean wrapCtm = this.wrapCtm;
		boolean wrapEmissive = this.wrapEmissive;
		if (ForeignModelFilter.isForeign(model) && !ForeignModelFilter.takesAppearance(state.getBlock())) {
			Set<TextureAtlasSprite> sprites = collectSprites(model, state);
			if (sprites == null) {
				return model;
			}
			wrapCtm = wrapCtm && foreignModelFilter.hasConnectedTextures(state, sprites);
			// A connected texture can have an emissive counterpart of its own, so the emissive wrapper has to stay
			// whenever the connected one does.
			wrapEmissive = wrapEmissive && (wrapCtm || ForeignModelFilter.hasEmissive(sprites));
		}

		if (wrapCtm) {
			model = new CtmBlockStateModel(model, state);
		}
		if (wrapEmissive) {
			model = new EmissiveBlockStateModel(model);
		}
		return model;
	}

	/**
	 * Collects the sprites a model draws when there is no level to ask, or {@code null} if the model cannot be asked
	 * at all, in which case it is left alone rather than risk wrapping one that is looked up and cast.
	 */
	@Nullable
	private Set<TextureAtlasSprite> collectSprites(BlockStateModel model, BlockState state) {
		List<BlockModelPart> parts = new ObjectArrayList<>();
		Set<TextureAtlasSprite> sprites = new ReferenceOpenHashSet<>();
		try {
			// The overload without a level hands a dynamic model air as its state, so the real one is passed instead.
			model.collectParts(EMPTY_LEVEL, BlockPos.ZERO, state, RandomSource.create(42L), parts);
			for (BlockModelPart part : parts) {
				for (int i = 0; i <= DIRECTIONS.length; i++) {
					Direction cullFace = i == DIRECTIONS.length ? null : DIRECTIONS[i];
					for (BakedQuad quad : part.getQuads(cullFace)) {
						TextureAtlasSprite sprite = workingQuad.fromVanilla(quad, cullFace).sprite();
						if (sprite != null) {
							sprites.add(sprite);
						}
					}
				}
			}
		} catch (RuntimeException | LinkageError e) {
			// This runs another mod's code during the resource reload, where anything thrown fails the reload and
			// takes every resource pack with it. A model reaching for a mod that is not installed throws an error
			// rather than an exception.
			ContinuityClient.LOGGER.debug("Could not read the textures of model {} for {}, leaving it unwrapped", model, state, e);
			return null;
		}
		return sprites;
	}

	@ApiStatus.Internal
	public void wrapAll(Map<BlockState, BlockStateModel> models) {
		for (Map.Entry<BlockState, BlockStateModel> entry : models.entrySet()) {
			BlockStateModel wrapped = wrap(entry.getValue(), entry.getKey());
			if (wrapped != entry.getValue()) {
				entry.setValue(wrapped);
			}
		}
	}
}
