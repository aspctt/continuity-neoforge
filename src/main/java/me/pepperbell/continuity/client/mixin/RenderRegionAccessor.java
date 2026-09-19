package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
//? if <26.1 {
import net.minecraft.world.level.Level;
//?} else
/*import net.minecraft.client.multiplayer.ClientLevel;*/

@Mixin(RenderChunkRegion.class)
public interface RenderRegionAccessor {
	//? if <26.1 {
	@Accessor("level")
	Level getLevel();
	//?} else {
	/*// 26.1 narrowed the field to the client level, and an accessor has to name the field's type exactly. Asking
	// for the wider type fails to apply, which fails the class load along with it.
	@Accessor("level")
	ClientLevel getLevel();
	*///?}
}
