package me.pepperbell.continuity.client.model.blockstatemodel;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

/**
 * Flattens a block state model down to the quads for one face, which is all the sprite lookup needs.
 *
 * <p>From 1.21.5 a model is collected as parts rather than queried for quads directly, so callers that only want to
 * know which texture a face uses go through here instead.
 */
public final class PartQuads {
	private PartQuads() {
	}

	public static List<BakedQuad> collect(BlockStateModel model, RandomSource random, @Nullable Direction face) {
		List<BlockModelPart> parts = new ObjectArrayList<>();
		model.collectParts(random, parts);

		List<BakedQuad> quads = new ObjectArrayList<>();
		int amount = parts.size();
		for (int i = 0; i < amount; i++) {
			quads.addAll(parts.get(i).getQuads(face));
		}
		return quads;
	}
}
