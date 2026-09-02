package me.pepperbell.continuity.api.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.impl.client.ProcessingDataKeyRegistryImpl;
import net.minecraft.resources.ResourceLocation;

@ApiStatus.NonExtendable
public interface ProcessingDataKeyRegistry {
	static ProcessingDataKeyRegistry get() {
		return ProcessingDataKeyRegistryImpl.INSTANCE;
	}

	default <T> ProcessingDataKey<T> registerKey(ResourceLocation id, Supplier<T> valueSupplier) {
		return registerKey(id, valueSupplier, null);
	}

	<T> ProcessingDataKey<T> registerKey(ResourceLocation id, Supplier<T> valueSupplier, Consumer<T> valueResetAction);

	@Nullable
	ProcessingDataKey<?> getKey(ResourceLocation id);

	int getRegisteredAmount();
}
