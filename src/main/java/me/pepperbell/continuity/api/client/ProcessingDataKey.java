package me.pepperbell.continuity.api.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

@ApiStatus.NonExtendable
public interface ProcessingDataKey<T> {
	ResourceLocation getId();

	int getRawId();

	Supplier<T> getValueSupplier();

	@Nullable
	Consumer<T> getValueResetAction();
}
