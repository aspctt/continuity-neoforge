package me.pepperbell.continuity.client.model.blockstatemodel;

import java.util.Map;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wraps block state models so that connected and emissive textures apply to them.
 *
 * <p>From 1.21.5 the baking result is keyed by block state, so unlike the older band there is no model identifier to
 * resolve back to a state first.
 */
public class ModelWrappingHandler {
	private final boolean wrapCtm;
	private final boolean wrapEmissive;

	private ModelWrappingHandler(boolean wrapCtm, boolean wrapEmissive) {
		this.wrapCtm = wrapCtm;
		this.wrapEmissive = wrapEmissive;
	}

	@Nullable
	public static ModelWrappingHandler create(boolean wrapCtm, boolean wrapEmissive) {
		if (!wrapCtm && !wrapEmissive) {
			return null;
		}
		return new ModelWrappingHandler(wrapCtm, wrapEmissive);
	}

	public BlockStateModel wrap(BlockStateModel model, BlockState state) {
		if (wrapCtm) {
			model = new CtmBlockStateModel(model, state);
		}
		if (wrapEmissive) {
			model = new EmissiveBlockStateModel(model);
		}
		return model;
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
