package me.pepperbell.continuity.client.mixin;

import java.util.List;

//? if <1.21.2
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if <1.21.2
import me.pepperbell.continuity.client.mixinterface.MultiPackResourceManagerExtension;
import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

/**
 * Points sprite ids reserved for CTM tiles back at the files they stand for.
 *
 * <p>Before 1.21.2 the mapping had to be kept in a per-manager table, because the real path was not a legal sprite
 * id. From 1.21.2 the id carries the path directly and the rewrite is a pure function, so nothing needs storing.
 */
@Mixin(MultiPackResourceManager.class)
//? if <1.21.2 {
abstract class MultiPackResourceManagerMixin implements MultiPackResourceManagerExtension {
//?} else
/*abstract class MultiPackResourceManagerMixin {*/
	@Unique
	//? if <1.21.2 {
	private ResourceRedirectHandler continuity$redirectHandler;

	@Override
	@Nullable
	public ResourceRedirectHandler continuity$getRedirectHandler() {
		return continuity$redirectHandler;
	}
	//?} else
	/*private boolean continuity$useRedirects;*/

	@Inject(method = "<init>(Lnet/minecraft/server/packs/PackType;Ljava/util/List;)V", at = @At("TAIL"))
	private void continuity$onTailInit(PackType type, List<PackResources> packs, CallbackInfo ci) {
		//? if <1.21.2 {
		if (type == PackType.CLIENT_RESOURCES) {
			continuity$redirectHandler = new ResourceRedirectHandler();
		}
		//?} else
		/*continuity$useRedirects = type == PackType.CLIENT_RESOURCES;*/
	}

	@ModifyVariable(method = "getResource(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;", at = @At("HEAD"), argsOnly = true)
	private ResourceLocation continuity$redirectGetResourceId(ResourceLocation id) {
		//? if <1.21.2 {
		if (continuity$redirectHandler != null) {
			return continuity$redirectHandler.redirect(id);
		}
		//?} else {
		/*if (continuity$useRedirects) {
			return ResourceRedirectHandler.redirect(id);
		}
		*///?}
		return id;
	}

	@ModifyVariable(method = "getResourceStack(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/List;", at = @At("HEAD"), argsOnly = true)
	private ResourceLocation continuity$redirectGetResourceStackId(ResourceLocation id) {
		//? if <1.21.2 {
		if (continuity$redirectHandler != null) {
			return continuity$redirectHandler.redirect(id);
		}
		//?} else {
		/*if (continuity$useRedirects) {
			return ResourceRedirectHandler.redirect(id);
		}
		*///?}
		return id;
	}
}
