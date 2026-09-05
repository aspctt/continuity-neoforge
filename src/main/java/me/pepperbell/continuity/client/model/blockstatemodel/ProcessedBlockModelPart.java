package me.pepperbell.continuity.client.model.blockstatemodel;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
public class ProcessedBlockModelPart implements BlockModelPart {
	private static final List<BakedQuad> EMPTY = List.of();

	private final List<BakedQuad>[] quadsByCullFace;
	private final RenderType layer;
	private final TriState ambientOcclusion;
	private final TextureAtlasSprite particleIcon;

	public ProcessedBlockModelPart(List<BakedQuad>[] quadsByCullFace, RenderType layer, TriState ambientOcclusion, TextureAtlasSprite particleIcon) {
		this.quadsByCullFace = quadsByCullFace;
		this.layer = layer;
		this.ambientOcclusion = ambientOcclusion;
		this.particleIcon = particleIcon;
	}

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

	@Override
	public RenderType getRenderType(BlockState state) {
		return layer;
	}

	@Override
	public TextureAtlasSprite particleIcon() {
		return particleIcon;
	}
}
