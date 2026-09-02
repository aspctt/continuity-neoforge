package me.pepperbell.continuity.api.client;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import me.pepperbell.continuity.client.render.Mesh;
import me.pepperbell.continuity.client.render.MutableQuadView;
import me.pepperbell.continuity.client.render.QuadEmitter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;

public interface QuadProcessor {
	ProcessingResult processQuad(MutableQuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, int pass, ProcessingContext context);

	interface ProcessingContext extends ProcessingDataProvider {
		void addEmitterConsumer(Consumer<QuadEmitter> consumer);

		void addMesh(Mesh mesh);

		QuadEmitter getExtraQuadEmitter();

		void markHasExtraQuads();
	}

	enum ProcessingResult {
		NEXT_PROCESSOR,
		NEXT_PASS,
		STOP,
		DISCARD;
	}

	interface Factory<T extends CtmProperties> {
		QuadProcessor createProcessor(T properties, Function<Material, TextureAtlasSprite> textureGetter);
	}
}
