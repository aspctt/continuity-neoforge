package me.pepperbell.continuity.impl.client;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.ProcessingDataKey;
import me.pepperbell.continuity.api.client.ProcessingDataKeyRegistry;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.render.Mesh;
import me.pepperbell.continuity.client.render.MeshBuilder;
import me.pepperbell.continuity.client.render.QuadEmitter;

public class ProcessingContextImpl implements QuadProcessor.ProcessingContext {
	protected final List<Consumer<QuadEmitter>> emitterConsumers = new ObjectArrayList<>();
	protected final List<Mesh> meshes = new ObjectArrayList<>();
	protected final MeshBuilder meshBuilder = new MeshBuilder();
	protected final Object[] processingData = new Object[ProcessingDataKeyRegistry.get().getRegisteredAmount()];

	protected boolean hasExtraQuads;

	@Override
	public void addEmitterConsumer(Consumer<QuadEmitter> consumer) {
		emitterConsumers.add(consumer);
	}

	@Override
	public void addMesh(Mesh mesh) {
		meshes.add(mesh);
	}

	@Override
	public QuadEmitter getExtraQuadEmitter() {
		return meshBuilder.getEmitter();
	}

	@Override
	public void markHasExtraQuads() {
		hasExtraQuads = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getData(ProcessingDataKey<T> key) {
		int index = key.getRawId();
		T data = (T) processingData[index];
		if (data == null) {
			data = key.getValueSupplier().get();
			processingData[index] = data;
		}
		return data;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T getDataOrNull(ProcessingDataKey<T> key) {
		return (T) processingData[key.getRawId()];
	}

	/**
	 * {@return whether any processor added geometry of its own}
	 */
	public boolean producedQuads() {
		return hasExtraQuads || !emitterConsumers.isEmpty() || !meshes.isEmpty();
	}

	public void outputTo(QuadEmitter emitter) {
		if (!emitterConsumers.isEmpty()) {
			int amount = emitterConsumers.size();
			for (int i = 0; i < amount; i++) {
				emitterConsumers.get(i).accept(emitter);
			}
		}
		if (!meshes.isEmpty()) {
			int amount = meshes.size();
			for (int i = 0; i < amount; i++) {
				meshes.get(i).outputTo(emitter);
			}
		}
		if (hasExtraQuads) {
			meshBuilder.build().outputTo(emitter);
		}
	}

	public void prepare() {
		hasExtraQuads = false;
	}

	public void reset() {
		emitterConsumers.clear();
		meshes.clear();
		resetData();
	}

	protected void resetData() {
		List<ProcessingDataKey<?>> allResettable = ProcessingDataKeyRegistryImpl.INSTANCE.getAllResettable();
		int amount = allResettable.size();
		for (int i = 0; i < amount; i++) {
			resetData(allResettable.get(i));
		}
	}

	protected <T> void resetData(ProcessingDataKey<T> key) {
		T value = getDataOrNull(key);
		if (value != null) {
			key.getValueResetAction().accept(value);
		}
	}
}
