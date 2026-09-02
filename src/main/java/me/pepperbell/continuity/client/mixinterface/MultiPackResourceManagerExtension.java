package me.pepperbell.continuity.client.mixinterface;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;

public interface MultiPackResourceManagerExtension {
	@Nullable
	ResourceRedirectHandler continuity$getRedirectHandler();
}
