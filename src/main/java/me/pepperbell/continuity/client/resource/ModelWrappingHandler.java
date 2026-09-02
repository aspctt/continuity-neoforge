package me.pepperbell.continuity.client.resource;

import java.util.Map;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMap;

import me.pepperbell.continuity.client.model.CtmBakedModel;
import me.pepperbell.continuity.client.model.EmissiveBakedModel;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wraps baked models so that connected and emissive textures apply to them.
 *
 * <p>{@link ModelReloadHandler} drives this from the model baking event, which fires after every model is baked but
 * before the block state to model cache is built, so a wrapper put in place here is what the world actually renders.
 * It runs at the lowest priority so that other mods have already applied their own wrappers and ours sits outermost.
 */
public class ModelWrappingHandler {
	private final boolean wrapCtm;
	private final boolean wrapEmissive;
	private final ImmutableMap<ModelResourceLocation, BlockState> blockStateModelIds;

	private ModelWrappingHandler(boolean wrapCtm, boolean wrapEmissive) {
		this.wrapCtm = wrapCtm;
		this.wrapEmissive = wrapEmissive;
		blockStateModelIds = createBlockStateModelIdMap();
	}

	@Nullable
	public static ModelWrappingHandler create(boolean wrapCtm, boolean wrapEmissive) {
		if (!wrapCtm && !wrapEmissive) {
			return null;
		}
		return new ModelWrappingHandler(wrapCtm, wrapEmissive);
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
		if (model == null || model.isCustomRenderer() || ModelBakery.MISSING_MODEL_VARIANT.equals(topLevelId)) {
			return model;
		}

		if (wrapCtm && topLevelId != null) {
			BlockState state = blockStateModelIds.get(topLevelId);
			if (state != null) {
				model = new CtmBakedModel(model, state);
			}
		}
		if (wrapEmissive) {
			model = new EmissiveBakedModel(model);
		}
		return model;
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
