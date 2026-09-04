package me.pepperbell.continuity.client.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
//? if <1.21.5 {
import me.pepperbell.continuity.client.model.bakedmodel.ModelWrappingHandler;
//?} else
/*import me.pepperbell.continuity.client.model.blockstatemodel.ModelWrappingHandler;*/
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.model.QuadProcessors;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.client.resources.model.Material;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Drives everything Continuity has to do during a resource reload: reading the CTM properties, feeding the extra
 * sprites into the atlas, building the quad processors once the atlas is stitched, and wrapping the baked models.
 *
 * <p>The work is spread across three points of the model reload. {@link ModelManager#reload} kicks off the property
 * load and installs the atlas context; {@link ModelEvent.ModifyBakingResult} builds the processors and wraps the
 * models, since it fires after stitching but before the block state cache is filled; and
 * {@link ModelEvent.BakingCompleted} publishes the processors to the renderer.
 */
public class ModelReloadHandler {
	@Nullable
	private static volatile ModelReloadHandler current;

	private final CompletableFuture<CtmPropertiesLoader.LoadingResult> ctmLoadingResultFuture;
	private final AtomicBoolean wrapEmissiveModels = new AtomicBoolean();
	private final SpriteLoaderLoadContextImpl spriteLoaderLoadContext;
	private volatile List<QuadProcessors.ProcessorHolder> processorHolders;
	@Nullable
	private volatile Map<ResourceLocation, AtlasSet.StitchResult> stitchResults;

	public ModelReloadHandler(ResourceManager resourceManager, Executor prepareExecutor) {
		ctmLoadingResultFuture = CompletableFuture.supplyAsync(() -> CtmPropertiesLoader.loadAllWithState(resourceManager), prepareExecutor);
		spriteLoaderLoadContext = new SpriteLoaderLoadContextImpl(ctmLoadingResultFuture.thenApply(CtmPropertiesLoader.LoadingResult::getTextureDependencies), wrapEmissiveModels);
		EmissiveSuffixLoader.load(resourceManager);
	}

	public void setContext() {
		SpriteLoaderLoadContext.THREAD_LOCAL.set(spriteLoaderLoadContext);
	}

	public void clearContext() {
		SpriteLoaderLoadContext.THREAD_LOCAL.set(null);
	}

	/**
	 * Records the stitched atlases so sprites can be looked up while processors are built.
	 *
	 * <p>Called from {@code ModelManagerMixin} rather than read off the baking event, because the texture getter the
	 * event hands out logs a warning and a stack trace for every sprite it cannot find. A CTM properties file naming a
	 * texture the pack does not ship is a normal, handled condition here, and it should not read as a Continuity fault
	 * in someone's log.
	 */
	public void setStitchResults(Map<ResourceLocation, AtlasSet.StitchResult> stitchResults) {
		this.stitchResults = stitchResults;
	}

	@Nullable
	public ModelWrappingHandler beforeBaking(Function<Material, TextureAtlasSprite> fallbackTextureGetter) {
		CtmPropertiesLoader.LoadingResult result = ctmLoadingResultFuture.join();

		Map<ResourceLocation, AtlasSet.StitchResult> stitchResults = this.stitchResults;
		Function<Material, TextureAtlasSprite> textureGetter;
		if (stitchResults == null) {
			// The mixin did not run, so take the event's getter and accept the extra logging over having no sprites.
			textureGetter = fallbackTextureGetter;
		} else {
			textureGetter = material -> {
				AtlasSet.StitchResult stitchResult = stitchResults.get(material.atlasLocation());
				if (stitchResult == null) {
					return fallbackTextureGetter.apply(material);
				}
				TextureAtlasSprite sprite = stitchResult.getSprite(material.texture());
				return sprite != null ? sprite : stitchResult.missing();
			};
		}

		List<QuadProcessors.ProcessorHolder> processorHolders = result.createProcessorHolders(textureGetter);
		this.processorHolders = processorHolders;

		return ModelWrappingHandler.create(!processorHolders.isEmpty(), wrapEmissiveModels.get());
	}

	public void apply() {
		List<QuadProcessors.ProcessorHolder> processorHolders = this.processorHolders;
		if (processorHolders != null) {
			QuadProcessors.reload(processorHolders);
			ContinuityClient.LOGGER.debug("Loaded {} connected texture processors, emissive textures {}", processorHolders.size(), wrapEmissiveModels.get() ? "present" : "absent");
		}
	}

	@ApiStatus.Internal
	public static void init(IEventBus modBus) {
		modBus.addListener(EventPriority.LOWEST, ModelReloadHandler::onModifyBakingResult);
		modBus.addListener(ModelReloadHandler::onBakingCompleted);
	}

	/**
	 * Called from {@code ModelManagerMixin} at the head of a model reload.
	 */
	@ApiStatus.Internal
	public static ModelReloadHandler beginReload(ResourceManager resourceManager, Executor prepareExecutor) {
		ModelReloadHandler handler = new ModelReloadHandler(resourceManager, prepareExecutor);
		current = handler;
		return handler;
	}

	@Nullable
	public static ModelReloadHandler getCurrent() {
		return current;
	}

	private static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		ModelReloadHandler handler = current;
		if (handler == null) {
			return;
		}

		ModelWrappingHandler wrappingHandler = handler.beforeBaking(event.getTextureGetter());
		if (wrappingHandler != null) {
			//? if <1.21.4 {
			wrappingHandler.wrapAll(event.getModels());
			//?} elif <1.21.5 {
			/*wrappingHandler.wrapAll(event.getBakingResult().blockStateModels());
			*///?} else
			/*wrappingHandler.wrapAll(event.getBakingResult().blockStateModels());*/
		}
	}

	private static void onBakingCompleted(ModelEvent.BakingCompleted event) {
		ModelReloadHandler handler = current;
		if (handler != null) {
			handler.apply();
			current = null;
		}
	}

	private static class SpriteLoaderLoadContextImpl implements SpriteLoaderLoadContext {
		private final CompletableFuture<Map<ResourceLocation, Set<ResourceLocation>>> allExtraIdsFuture;
		private final Map<ResourceLocation, CompletableFuture<Set<ResourceLocation>>> extraIdsFutures = new Object2ObjectOpenHashMap<>();
		private final EmissiveControl blockAtlasEmissiveControl;

		public SpriteLoaderLoadContextImpl(CompletableFuture<Map<ResourceLocation, Set<ResourceLocation>>> allExtraIdsFuture, AtomicBoolean blockAtlasHasEmissivesHolder) {
			this.allExtraIdsFuture = allExtraIdsFuture;
			blockAtlasEmissiveControl = new EmissiveControlImpl(blockAtlasHasEmissivesHolder);
		}

		@Override
		public CompletableFuture<@Nullable Set<ResourceLocation>> getExtraIdsFuture(ResourceLocation atlasId) {
			return extraIdsFutures.computeIfAbsent(atlasId, id -> allExtraIdsFuture.thenApply(allExtraIds -> allExtraIds.get(id)));
		}

		@Override
		@Nullable
		public EmissiveControl getEmissiveControl(ResourceLocation atlasId) {
			if (atlasId.equals(TextureAtlas.LOCATION_BLOCKS)) {
				return blockAtlasEmissiveControl;
			}
			return null;
		}

		private static class EmissiveControlImpl implements EmissiveControl {
			@Nullable
			private volatile Map<ResourceLocation, ResourceLocation> emissiveIdMap;
			private final AtomicBoolean hasEmissivesHolder;

			public EmissiveControlImpl(AtomicBoolean hasEmissivesHolder) {
				this.hasEmissivesHolder = hasEmissivesHolder;
			}

			@Override
			@Nullable
			public Map<ResourceLocation, ResourceLocation> getEmissiveIdMap() {
				return emissiveIdMap;
			}

			@Override
			public void setEmissiveIdMap(Map<ResourceLocation, ResourceLocation> emissiveIdMap) {
				this.emissiveIdMap = emissiveIdMap;
			}

			@Override
			public void markHasEmissives() {
				hasEmissivesHolder.set(true);
			}
		}
	}
}
