package me.pepperbell.continuity.client.mixin;

//? if <26.1
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import me.pepperbell.continuity.client.resource.ModelReloadHandler;
import net.minecraft.client.resources.model.ModelBakery;
//? if <26.1 {
import me.pepperbell.continuity.client.model.bakedmodel.ModelWrappingHandler;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
//?} else {
/*import me.pepperbell.continuity.client.model.blockstatemodel.ModelWrappingHandler;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.world.level.block.state.BlockState;
*///?}

/**
 * Wraps each model as it is baked, when ModernFix bakes them on demand.
 *
 * <p>With its dynamic resources option on, ModernFix bakes a model the first time something asks for it, and may drop
 * it again and bake it anew later. Most are baked long after the baking event has fired, so wrapping them there would
 * mean baking every model up front. The lambda targeted here is the one both the game and ModernFix bake each model
 * through, so a model is wrapped however and whenever it comes to be baked. When models are baked all at once, there
 * is no handler and this changes nothing.
 */
@Mixin(ModelBakery.class)
abstract class ModelBakeryMixin {
	//? if <26.1 {
	@WrapOperation(method = "lambda$bakeModels$6", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object continuity$wrapBakedModel(Map<Object, Object> bakedModels, Object location, Object model, Operation<Object> original) {
		ModelWrappingHandler handler = ModelReloadHandler.getOnDemandWrappingHandler();
		if (handler != null) {
			model = handler.wrap((BakedModel) model, (ModelResourceLocation) location);
		}
		return original.call(bakedModels, location, model);
	}
	//?} else {
	/*@WrapOperation(method = "lambda$bakeModels$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel$UnbakedRoot;bake(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/resources/model/ModelBaker;)Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;"))
	private static BlockStateModel continuity$wrapBakedModel(BlockStateModel.UnbakedRoot unbaked, BlockState state, ModelBaker baker, Operation<BlockStateModel> original) {
		BlockStateModel model = original.call(unbaked, state, baker);
		ModelWrappingHandler handler = ModelReloadHandler.getOnDemandWrappingHandler();
		return handler == null ? model : handler.wrap(model, state);
	}
	*///?}
}
