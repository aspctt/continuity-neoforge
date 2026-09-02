package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pepperbell.continuity.client.resource.InvalidIdentifierStateHolder;
import net.minecraft.resources.ResourceLocation;

@Mixin(ResourceLocation.class)
abstract class ResourceLocationMixin {
	@Inject(method = "isValidPath(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
	private static void continuity$onIsPathValid(CallbackInfoReturnable<Boolean> cir) {
		if (InvalidIdentifierStateHolder.get().isEnabled()) {
			cir.setReturnValue(true);
		}
	}
}
