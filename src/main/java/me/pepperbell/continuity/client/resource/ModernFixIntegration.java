package me.pepperbell.continuity.client.resource;

import org.embeddedt.modernfix.api.entrypoint.ModernFixClientIntegration;

import me.pepperbell.continuity.client.ContinuityClient;

/**
 * Hears from ModernFix whether it bakes models on demand.
 *
 * <p>ModernFix reads this class out of the mod metadata and instantiates it itself, once at startup, and only calls
 * it when its dynamic resources option is on. Nothing in Continuity names it, so it stays unloaded when ModernFix is
 * not installed.
 */
public class ModernFixIntegration implements ModernFixClientIntegration {
	@Override
	public void onDynamicResourcesStatusChange(boolean enabled) {
		ModelReloadHandler.setModelsBakedOnDemand(enabled);
		if (enabled) {
			ContinuityClient.LOGGER.info("ModernFix bakes models on demand, so they will be wrapped as they are baked");
		}
	}
}
