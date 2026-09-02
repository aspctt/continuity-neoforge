package me.pepperbell.continuity.client.processor.overlay;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.processor.ProcessingPredicate;
import me.pepperbell.continuity.client.processor.simple.SimpleQuadProcessor;
import me.pepperbell.continuity.client.processor.simple.SpriteProvider;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import me.pepperbell.continuity.client.properties.overlay.OverlayPropertiesSection;
import me.pepperbell.continuity.client.util.QuadUtil;
import me.pepperbell.continuity.client.util.RenderUtil;
import me.pepperbell.continuity.client.util.TextureUtil;
import me.pepperbell.continuity.client.render.BlendMode;
import me.pepperbell.continuity.client.render.RenderMaterial;
import me.pepperbell.continuity.client.render.MutableQuadView;
import me.pepperbell.continuity.client.render.QuadEmitter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;

public class SimpleOverlayQuadProcessor extends SimpleQuadProcessor {
	protected int tintIndex;
	@Nullable
	protected BlockState tintBlock;
	protected RenderMaterial material;

	public SimpleOverlayQuadProcessor(SpriteProvider spriteProvider, ProcessingPredicate processingPredicate, int tintIndex, @Nullable BlockState tintBlock, BlendMode layer) {
		super(spriteProvider, processingPredicate);
		this.tintIndex = tintIndex;
		this.tintBlock = tintBlock;
		material = RenderUtil.findOverlayMaterial(layer, this.tintBlock);
	}

	@Override
	public ProcessingResult processQuad(MutableQuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, int pass, ProcessingContext context) {
		if (processingPredicate.shouldProcessQuad(quad, sprite, blockView, appearanceState, state, pos, context)) {
			TextureAtlasSprite newSprite = spriteProvider.getSprite(quad, sprite, blockView, appearanceState, state, pos, randomSupplier, context);
			if (newSprite != null && !TextureUtil.isMissingSprite(newSprite)) {
				OverlayEmitter emitter = context.getData(ProcessingDataKeys.SIMPLE_OVERLAY_EMITTER_POOL).get();
				emitter.prepare(quad.lightFace(), newSprite, RenderUtil.getTintColor(tintBlock, blockView, pos, tintIndex), material);
				context.addEmitterConsumer(emitter);
			}
		}
		return ProcessingResult.NEXT_PROCESSOR;
	}

	public static class OverlayEmitter implements Consumer<QuadEmitter> {
		protected Direction face;
		protected TextureAtlasSprite sprite;
		protected int color;
		protected RenderMaterial material;

		@Override
		public void accept(QuadEmitter emitter) {
			QuadUtil.emitOverlayQuad(emitter, face, sprite, color, material);
		}

		public void prepare(Direction face, TextureAtlasSprite sprite, int color, RenderMaterial material) {
			this.face = face;
			this.sprite = sprite;
			this.color = color;
			this.material = material;
		}
	}

	public static class OverlayEmitterPool {
		protected final List<OverlayEmitter> list = new ObjectArrayList<>();
		protected int nextIndex = 0;

		public OverlayEmitter get() {
			if (nextIndex >= list.size()) {
				list.add(new OverlayEmitter());
			}
			OverlayEmitter emitter = list.get(nextIndex);
			nextIndex++;
			return emitter;
		}

		public void reset() {
			nextIndex = 0;
		}
	}

	public static class Factory<T extends BaseCtmProperties & OverlayPropertiesSection.Provider> extends SimpleQuadProcessor.Factory<T> {
		public Factory(SpriteProvider.Factory<? super T> spriteProviderFactory) {
			super(spriteProviderFactory);
		}

		@Override
		public QuadProcessor createProcessor(T properties, TextureAtlasSprite[] sprites) {
			OverlayPropertiesSection overlaySection = properties.getOverlayPropertiesSection();
			return new SimpleOverlayQuadProcessor(spriteProviderFactory.createSpriteProvider(sprites, properties), OverlayProcessingPredicate.fromProperties(properties), overlaySection.getTintIndex(), overlaySection.getTintBlock(), overlaySection.getLayer());
		}

		@Override
		public boolean supportsNullSprites(T properties) {
			return false;
		}
	}
}
