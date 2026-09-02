package me.pepperbell.continuity.client.properties.overlay;

import java.util.Properties;

import me.pepperbell.continuity.client.processor.OrientationMode;
import me.pepperbell.continuity.client.properties.OrientedConnectingCtmProperties;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackResources;
import net.minecraft.resources.ResourceLocation;

public class OrientedConnectingOverlayCtmProperties extends OrientedConnectingCtmProperties implements OverlayPropertiesSection.Provider {
	protected OverlayPropertiesSection overlaySection;

	public OrientedConnectingOverlayCtmProperties(Properties properties, ResourceLocation resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method, OrientationMode defaultOrientationMode) {
		super(properties, resourceId, pack, packPriority, resourceManager, method, defaultOrientationMode);
		overlaySection = new OverlayPropertiesSection(properties, resourceId, packId);
	}

	public OrientedConnectingOverlayCtmProperties(Properties properties, ResourceLocation resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method) {
		this(properties, resourceId, pack, packPriority, resourceManager, method, OrientationMode.NONE);
	}

	@Override
	public void init() {
		super.init();
		overlaySection.init();
	}

	@Override
	public OverlayPropertiesSection getOverlayPropertiesSection() {
		return overlaySection;
	}
}
