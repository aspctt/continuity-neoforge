package me.pepperbell.continuity.client.model.blockstatemodel;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One render type worth of processed quads, in the form the renderer asks for.
 *
 * <p>A part carries a single render type, so a block whose overlays land in several layers becomes several parts.
 * That is the same split the older model data pipeline expressed through {@code getRenderTypes}, just spelled out as
 * separate objects.
 */
public class ProcessedModelPart implements BlockModelPart {
	private static final List<BakedQuad> EMPTY = List.of();

	private final List<BakedQuad>[] quadsByCullFace;
	private final TriState ambientOcclusion;
	//? if <26.1 {
	private final RenderType layer;
	private final TextureAtlasSprite particleIcon;
	//?} else {
	/*private final int materialFlags;
	private final Material.Baked particleMaterial;
	*///?}

	//? if <26.1 {
	public ProcessedModelPart(List<BakedQuad>[] quadsByCullFace, RenderType layer, TriState ambientOcclusion, TextureAtlasSprite particleIcon) {
		this.quadsByCullFace = quadsByCullFace;
		this.layer = layer;
		this.ambientOcclusion = ambientOcclusion;
		this.particleIcon = particleIcon;
	}
	//?} else {
	/*// 26.1 moved the chunk layer onto the quads, so a part covers every layer at once and only has to summarise
	// what its quads contain.
	public ProcessedModelPart(List<BakedQuad>[] quadsByCullFace, TriState ambientOcclusion, Material.Baked particleMaterial) {
		this.quadsByCullFace = quadsByCullFace;
		this.ambientOcclusion = ambientOcclusion;
		this.particleMaterial = particleMaterial;
		this.materialFlags = flagsOf(quadsByCullFace);
	}

	private static int flagsOf(List<BakedQuad>[] quadsByCullFace) {
		int flags = 0;
		for (List<BakedQuad> quads : quadsByCullFace) {
			if (quads == null) {
				continue;
			}
			int amount = quads.size();
			for (int i = 0; i < amount; i++) {
				flags |= quads.get(i).materialInfo().flags();
			}
		}
		return flags;
	}
	*///?}

	@Override
	public List<BakedQuad> getQuads(@Nullable Direction direction) {
		List<BakedQuad> quads = quadsByCullFace[direction == null ? Direction.values().length : direction.ordinal()];
		return quads == null ? EMPTY : quads;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return ambientOcclusion != TriState.FALSE;
	}

	@Override
	public TriState ambientOcclusion() {
		return ambientOcclusion;
	}

	//? if <26.1 {
	@Override
	public RenderType getRenderType(BlockState state) {
		return layer;
	}

	@Override
	public TextureAtlasSprite particleIcon() {
		return particleIcon;
	}
	//?} else {
	/*@Override
	public Material.Baked particleMaterial() {
		return particleMaterial;
	}

	@Override
	public int materialFlags() {
		return materialFlags;
	}
	*///?}
}
