package me.pepperbell.continuity.client.properties.overlay;

import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.properties.BasicConnectingCtmProperties;
import me.pepperbell.continuity.client.properties.PropertiesParsingHelper;
import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackResources;
import net.minecraft.resources.ResourceLocation;

public class StandardOverlayCtmProperties extends BasicConnectingCtmProperties implements OverlayPropertiesSection.Provider {
	protected OverlayPropertiesSection overlaySection;
	@Nullable
	protected Set<ResourceLocation> connectTilesSet;
	@Nullable
	protected Predicate<BlockState> connectBlocksPredicate;

	public StandardOverlayCtmProperties(Properties properties, ResourceLocation resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method) {
		super(properties, resourceId, pack, packPriority, resourceManager, method);
		overlaySection = new OverlayPropertiesSection(properties, resourceId, packId);
	}

	@Override
	public void init() {
		super.init();
		overlaySection.init();
		parseConnectTiles();
		parseConnectBlocks();
	}

	@Override
	public OverlayPropertiesSection getOverlayPropertiesSection() {
		return overlaySection;
	}

	protected void parseConnectTiles() {
		//? if <1.21.2 {
		connectTilesSet = PropertiesParsingHelper.parseMatchTiles(properties, "connectTiles", resourceId, packId, ResourceRedirectHandler.get(resourceManager));
		//?} else
		/*connectTilesSet = PropertiesParsingHelper.parseMatchTiles(properties, "connectTiles", resourceId, packId);*/
	}

	protected void parseConnectBlocks() {
		connectBlocksPredicate = PropertiesParsingHelper.parseBlockStates(properties, "connectBlocks", resourceId, packId);
	}

	@Nullable
	public Set<ResourceLocation> getConnectTilesSet() {
		return connectTilesSet;
	}

	@Nullable
	public Predicate<BlockState> getConnectBlocksPredicate() {
		return connectBlocksPredicate;
	}
}
