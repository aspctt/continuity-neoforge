package me.pepperbell.continuity.client.util;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.mixin.RenderRegionAccessor;
import me.pepperbell.continuity.client.render.BlendMode;
import me.pepperbell.continuity.client.render.MaterialFinder;
import me.pepperbell.continuity.client.render.RenderMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;

public final class RenderUtil {
	private static final ThreadLocal<MaterialFinder> MATERIAL_FINDER = ThreadLocal.withInitial(MaterialFinder::new);

	@Nullable
	private static volatile BlockColors blockColors;

	private RenderUtil() {
	}

	public static int getTintColor(@Nullable BlockState state, BlockAndTintGetter blockView, BlockPos pos, int tintIndex) {
		if (state == null || tintIndex == -1) {
			return -1;
		}
		//? if <26.1 {
		return 0xFF000000 | getBlockColors().getColor(state, blockView, pos, tintIndex);
		//?} else
		/*return 0xFF000000 | getBlockColors().getTintSource(state, tintIndex).colorInWorld(state, blockView, pos);*/
	}

	public static RenderMaterial findOverlayMaterial(BlendMode blendMode, @Nullable BlockState tintBlock) {
		MaterialFinder finder = getMaterialFinder();
		finder.blendMode(blendMode);
		if (tintBlock != null) {
			finder.ambientOcclusion(triState(canHaveAO(tintBlock)));
		} else {
			finder.ambientOcclusion(TriState.TRUE);
		}
		return finder.find();
	}

	public static boolean canHaveAO(BlockState state) {
		return state.getLightEmission() == 0;
	}

	public static TriState triState(boolean value) {
		return value ? TriState.TRUE : TriState.FALSE;
	}

	public static MaterialFinder getMaterialFinder() {
		return MATERIAL_FINDER.get().clear();
	}

	/**
	 * {@return the biome at {@code pos}, or {@code null} when no level can be reached}
	 *
	 * <p>Chunk rendering hands models a {@link RenderChunkRegion}, which holds a level but does not expose biomes, so
	 * the lookup goes through the region rather than the view interface.
	 *
	 * <p>Renderers that replace vanilla's chunk rendering, Sodium among them, pass a view of their own that is neither
	 * a level nor a region, and NeoForge puts no biome accessor on {@link BlockAndTintGetter} to ask through instead.
	 * Those fall back to the client level, which is the same object the region would have handed back. Vanilla already
	 * reads biomes off it from the chunk build threads when it resolves block tints, so the fallback is no less safe
	 * than the branch above it.
	 */
	@Nullable
	public static Biome getBiome(BlockAndTintGetter blockView, BlockPos pos) {
		LevelReader level;
		if (blockView instanceof LevelReader levelReader) {
			level = levelReader;
		} else if (blockView instanceof RenderChunkRegion region) {
			level = ((RenderRegionAccessor) region).getLevel();
		} else {
			level = Minecraft.getInstance().level;
			if (level == null) {
				return null;
			}
		}
		return level.getBiome(pos).value();
	}

	private static BlockColors getBlockColors() {
		BlockColors colors = blockColors;
		if (colors == null) {
			colors = Minecraft.getInstance().getBlockColors();
			blockColors = colors;
		}
		return colors;
	}
}
