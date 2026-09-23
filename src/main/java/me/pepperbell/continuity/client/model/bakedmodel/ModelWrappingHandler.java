package me.pepperbell.continuity.client.model.bakedmodel;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMap;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.model.ForeignModelFilter;
import me.pepperbell.continuity.client.model.QuadProcessors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
//? if <1.21.2 {
import net.minecraft.client.resources.model.ModelBakery;
//?} else
/*import net.minecraft.client.resources.model.MissingBlockModel;*/
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Wraps baked models so that connected and emissive textures apply to them.
 *
 * <p>{@code ModelReloadHandler} drives this from the model baking event, which fires after every model is baked but
 * before the block state to model cache is built, so a wrapper put in place here is what the world actually renders.
 * It runs at the lowest priority so that other mods have already applied their own wrappers and ours sits outermost.
 */
public class ModelWrappingHandler {
	private static final Direction[] DIRECTIONS = Direction.values();

	private final boolean wrapCtm;
	private final boolean wrapEmissive;
	private final ForeignModelFilter foreignModelFilter;
	private final ImmutableMap<ModelResourceLocation, BlockState> blockStateModelIds;

	private ModelWrappingHandler(List<QuadProcessors.ProcessorHolder> processorHolders, boolean wrapEmissive) {
		wrapCtm = !processorHolders.isEmpty();
		this.wrapEmissive = wrapEmissive;
		foreignModelFilter = new ForeignModelFilter(processorHolders);
		blockStateModelIds = createBlockStateModelIdMap();
	}

	@Nullable
	public static ModelWrappingHandler create(List<QuadProcessors.ProcessorHolder> processorHolders, boolean wrapEmissive) {
		if (processorHolders.isEmpty() && !wrapEmissive) {
			return null;
		}
		return new ModelWrappingHandler(processorHolders, wrapEmissive);
	}

	private static ImmutableMap<ModelResourceLocation, BlockState> createBlockStateModelIdMap() {
		ImmutableMap.Builder<ModelResourceLocation, BlockState> builder = ImmutableMap.builder();
		// Match the code of ModelManager#loadModels, which resolves models for block states the same way.
		for (Block block : BuiltInRegistries.BLOCK) {
			ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
			for (BlockState state : block.getStateDefinition().getPossibleStates()) {
				ModelResourceLocation modelId = BlockModelShaper.stateToModelLocation(blockId, state);
				builder.put(modelId, state);
			}
		}
		return builder.build();
	}

	public BakedModel wrap(@Nullable BakedModel model, @Nullable ModelResourceLocation topLevelId) {
		//? if <1.21.2 {
		if (model == null || model.isCustomRenderer() || ModelBakery.MISSING_MODEL_VARIANT.equals(topLevelId)) {
		//?} elif <1.21.4 {
		/*if (model == null || model.isCustomRenderer() || MissingBlockModel.VARIANT.equals(topLevelId)) {
		*///?} else
		/*if (model == null || MissingBlockModel.VARIANT.equals(topLevelId)) {*/
			return model;
		}

		BlockState state = topLevelId == null ? null : blockStateModelIds.get(topLevelId);
		boolean wrapCtm = this.wrapCtm && state != null;
		boolean wrapEmissive = this.wrapEmissive;
		if (ForeignModelFilter.isForeign(model) && (state == null || !ForeignModelFilter.takesAppearance(state.getBlock()))) {
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
			model = new CtmBakedModel(model, state);
		}
		if (wrapEmissive) {
			model = new EmissiveBakedModel(model);
		}
		return model;
	}

	/**
	 * Collects the sprites a model draws when there is no level to ask, or {@code null} if the model cannot be asked
	 * at all, in which case it is left alone rather than risk wrapping one that is looked up and cast.
	 */
	@Nullable
	private static Set<TextureAtlasSprite> collectSprites(BakedModel model, @Nullable BlockState state) {
		RandomSource random = RandomSource.create();
		Set<TextureAtlasSprite> sprites = new ReferenceOpenHashSet<>();
		try {
			for (int i = 0; i <= DIRECTIONS.length; i++) {
				Direction cullFace = i == DIRECTIONS.length ? null : DIRECTIONS[i];
				random.setSeed(42L);
				for (BakedQuad quad : model.getQuads(state, cullFace, random, ModelData.EMPTY, null)) {
					sprites.add(quad.getSprite());
				}
			}
		} catch (RuntimeException e) {
			ContinuityClient.LOGGER.debug("Could not read the textures of model {} for {}, leaving it unwrapped", model, state, e);
			return null;
		}
		return sprites;
	}

	@ApiStatus.Internal
	public void wrapAll(Map<ModelResourceLocation, BakedModel> models) {
		for (Map.Entry<ModelResourceLocation, BakedModel> entry : models.entrySet()) {
			BakedModel wrapped = wrap(entry.getValue(), entry.getKey());
			if (wrapped != entry.getValue()) {
				entry.setValue(wrapped);
			}
		}
	}
}
