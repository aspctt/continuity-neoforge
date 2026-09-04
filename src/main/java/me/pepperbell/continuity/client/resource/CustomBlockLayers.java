package me.pepperbell.continuity.client.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.properties.PropertiesParsingHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.neoforged.bus.api.IEventBus;
//? if <1.21.4 {
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
//?} else
/*import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;*/

public final class CustomBlockLayers {
	public static final ResourceLocation LOCATION = ResourceLocation.withDefaultNamespace("optifine/block.properties");

	@SuppressWarnings("unchecked")
	private static final Predicate<BlockState>[] EMPTY_LAYER_PREDICATES = new Predicate[BlockLayer.VALUES.length];

	@SuppressWarnings("unchecked")
	private static final Predicate<BlockState>[] LAYER_PREDICATES = new Predicate[BlockLayer.VALUES.length];

	private static boolean empty;

	private static boolean disableSolidCheck;

	public static boolean isEmpty() {
		return empty;
	}

	@Nullable
	public static RenderType getLayer(BlockState state) {
		if (!disableSolidCheck) {
			//? if <1.21.2 {
			if (state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
			//?} else
			/*if (state.isSolidRender()) {*/
				return null;
			}
		}

		for (int i = 0; i < BlockLayer.VALUES.length; i++) {
			Predicate<BlockState> predicate = LAYER_PREDICATES[i];
			if (predicate != null) {
				if (predicate.test(state)) {
					return BlockLayer.VALUES[i].getLayer();
				}
			}
		}
		return null;
	}

	private static void reload(ResourceManager manager) {
		empty = true;
		System.arraycopy(EMPTY_LAYER_PREDICATES, 0, LAYER_PREDICATES, 0, EMPTY_LAYER_PREDICATES.length);
		disableSolidCheck = false;

		Optional<Resource> optionalResource = manager.getResource(LOCATION);
		if (optionalResource.isPresent()) {
			Resource resource = optionalResource.get();
			try (InputStream inputStream = resource.open()) {
				Properties properties = new Properties();
				properties.load(inputStream);
				reload(properties, LOCATION, resource.sourcePackId());
			} catch (IOException e) {
				ContinuityClient.LOGGER.error("Failed to load custom block layers from file '" + LOCATION + "' from pack '" + resource.sourcePackId() + "'", e);
			}
		}
	}

	private static void reload(Properties properties, ResourceLocation fileLocation, String packId) {
		for (BlockLayer blockLayer : BlockLayer.VALUES) {
			String propertyKey = "layer." + blockLayer.getKey();
			Predicate<BlockState> predicate = PropertiesParsingHelper.parseBlockStates(properties, propertyKey, fileLocation, packId);
			if (predicate != null && predicate != PropertiesParsingHelper.EMPTY_BLOCK_STATE_PREDICATE) {
				LAYER_PREDICATES[blockLayer.ordinal()] = predicate;
				empty = false;
			}
		}

		String disableSolidCheckStr = properties.getProperty("disableSolidCheck");
		if (disableSolidCheckStr != null) {
			disableSolidCheck = Boolean.parseBoolean(disableSolidCheckStr.trim());
		}
	}

	public static class ReloadListener implements PreparableReloadListener {
		public static final ResourceLocation ID = ContinuityClient.asId("custom_block_layers");
		private static final ReloadListener INSTANCE = new ReloadListener();

		public static void init(IEventBus modBus) {
			//? if <1.21.4 {
			modBus.addListener(RegisterClientReloadListenersEvent.class, event -> event.registerReloadListener(INSTANCE));
			//?} else
			/*modBus.addListener(AddClientReloadListenersEvent.class, event -> event.addListener(ID, INSTANCE));*/
		}

		@Override
		//? if <1.21.2 {
		public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
		//?} else
		/*public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, Executor backgroundExecutor, Executor gameExecutor) {*/
			return CompletableFuture.supplyAsync(() -> manager, backgroundExecutor)
					.thenCompose(barrier::wait)
					.thenAcceptAsync(CustomBlockLayers::reload, gameExecutor);
		}

		@Override
		public String getName() {
			return ID.toString();
		}
	}

	private enum BlockLayer {
		SOLID(RenderType.solid()),
		CUTOUT(RenderType.cutout()),
		CUTOUT_MIPPED(RenderType.cutoutMipped()),
		TRANSLUCENT(RenderType.translucent());

		public static final BlockLayer[] VALUES = values();

		private final RenderType layer;
		private final String key;

		BlockLayer(RenderType layer) {
			this.layer = layer;
			key = name().toLowerCase(Locale.ROOT);
		}

		public RenderType getLayer() {
			return layer;
		}

		public String getKey() {
			return key;
		}
	}
}
