package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.Level;

@Mixin(RenderChunkRegion.class)
public interface RenderRegionAccessor {
	@Accessor("level")
	Level getLevel();
}
