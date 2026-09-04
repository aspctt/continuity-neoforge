package me.pepperbell.continuity.client.model;

import me.pepperbell.continuity.client.render.MeshBuilder;
import me.pepperbell.continuity.client.render.MutableQuad;
import me.pepperbell.continuity.client.render.QuadCollector;
//? if <1.21.5 {
import me.pepperbell.continuity.client.model.bakedmodel.CtmBakedModel;
import me.pepperbell.continuity.client.model.bakedmodel.EmissiveBakedModel;
//?} else
/*import me.pepperbell.continuity.client.model.blockstatemodel.CtmBlockStateModel;*/
import me.pepperbell.continuity.impl.client.ContinuityFeatureStatesImpl;

/**
 * The per-thread scratch space model wrappers work in.
 *
 * <p>Chunks are built on several threads at once and every one of them runs the same processing, so none of this can
 * be shared. Everything here is reused between blocks rather than reallocated.
 */
public class ModelObjectsContainer {
	public static final ThreadLocal<ModelObjectsContainer> THREAD_LOCAL = ThreadLocal.withInitial(ModelObjectsContainer::new);

	//? if <1.21.5 {
	public final CtmBakedModel.CtmQuadTransform ctmQuadTransform = new CtmBakedModel.CtmQuadTransform();
	public final EmissiveBakedModel.EmissiveBlockQuadTransform emissiveBlockQuadTransform = new EmissiveBakedModel.EmissiveBlockQuadTransform();
	public final EmissiveBakedModel.EmissiveItemQuadTransform emissiveItemQuadTransform = new EmissiveBakedModel.EmissiveItemQuadTransform();
	//?} else
	/*public final CtmBlockStateModel.CtmQuadTransform ctmQuadTransform = new CtmBlockStateModel.CtmQuadTransform();*/

	public final ContinuityFeatureStatesImpl featureStates = new ContinuityFeatureStatesImpl();
	public final MeshBuilder meshBuilder = new MeshBuilder();
	public final QuadCollector ctmQuadCollector = new QuadCollector();
	public final QuadCollector emissiveQuadCollector = new QuadCollector();
	public final MutableQuad workingQuad = new MutableQuad();
	public final MutableQuad emissiveWorkingQuad = new MutableQuad();

	public static ModelObjectsContainer get() {
		return THREAD_LOCAL.get();
	}
}
