package me.pepperbell.continuity.client.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.pepperbell.continuity.client.mixinterface.MultiPackResourceManagerExtension;
import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.ResourceLocation;

@Mixin(MultiPackResourceManager.class)
abstract class MultiPackResourceManagerMixin implements MultiPackResourceManagerExtension {
	@Unique
	private ResourceRedirectHandler continuity$redirectHandler;

	@Override
	@Nullable
	public ResourceRedirectHandler continuity$getRedirectHandler() {
		return continuity$redirectHandler;
	}

	@Inject(method = "<init>(Lnet/minecraft/server/packs/PackType;Ljava/util/List;)V", at = @At("TAIL"))
	private void continuity$onTailInit(PackType type, List<PackResources> packs, CallbackInfo ci) {
		if (type == PackType.CLIENT_RESOURCES) {
			continuity$redirectHandler = new ResourceRedirectHandler();
		}
	}

	@ModifyVariable(method = "getResource(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;", at = @At("HEAD"), argsOnly = true)
	private ResourceLocation continuity$redirectGetResourceId(ResourceLocation id) {
		if (continuity$redirectHandler != null) {
			return continuity$redirectHandler.redirect(id);
		}
		return id;
	}

	@ModifyVariable(method = "getResourceStack(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/List;", at = @At("HEAD"), argsOnly = true)
	private ResourceLocation continuity$redirectGetResourceStackId(ResourceLocation id) {
		if (continuity$redirectHandler != null) {
			return continuity$redirectHandler.redirect(id);
		}
		return id;
	}
}
