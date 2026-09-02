package me.pepperbell.continuity.client.properties;

import java.util.Properties;

import me.pepperbell.continuity.client.processor.OrientationMode;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackResources;
import net.minecraft.resources.ResourceLocation;

public class OrientedConnectingCtmProperties extends ConnectingCtmProperties {
	protected OrientationMode orientationMode;

	public OrientedConnectingCtmProperties(Properties properties, ResourceLocation resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method, OrientationMode defaultOrientationMode) {
		super(properties, resourceId, pack, packPriority, resourceManager, method);
		orientationMode = defaultOrientationMode;
	}

	public OrientedConnectingCtmProperties(Properties properties, ResourceLocation resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method) {
		this(properties, resourceId, pack, packPriority, resourceManager, method, OrientationMode.TEXTURE);
	}

	@Override
	public void init() {
		super.init();
		parseOrient();
	}

	protected void parseOrient() {
		OrientationMode orientationMode = PropertiesParsingHelper.parseOrientationMode(properties, "orient", resourceId, packId);
		if (orientationMode != null) {
			this.orientationMode = orientationMode;
		}
	}

	public OrientationMode getOrientationMode() {
		return orientationMode;
	}
}
