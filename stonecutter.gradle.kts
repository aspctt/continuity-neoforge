plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1"

stonecutter parameters {
    // Available to source files as `//$ minecraft` swaps and in `//? if` conditions.
    swaps["minecraft"] = "\"${node.metadata.version}\";"

    // Pure renames only. Anything that changes arity, arguments or semantics is handled with an inline
    // `//? if` directive instead, so the difference is visible where it matters.
    replacements {
        string(current.parsed >= "1.21.2") {
            // Registry lookups returning the value directly moved to getValue; get now returns a Holder.
            replace("BuiltInRegistries.BLOCK.get(", "BuiltInRegistries.BLOCK.getValue(")
            replace("biomeRegistry.get(", "biomeRegistry.getValue(")
            replace(".registryOrThrow(", ".lookupOrThrow(")
        }

        string(current.parsed >= "1.21.5") {
            // TriState moved out of NeoForge and into vanilla.
            replace("import net.neoforged.neoforge.common.util.TriState;", "import net.minecraft.util.TriState;")
            // BakedQuad became a record in 1.21.5, but its accessor names collide with this mod's own quad
            // API, so those call sites use directives rather than a replacement that would rewrite both.
        }

        string(current.parsed >= "1.21.6") {
            // Chunk layers left RenderType for an enum of their own. The layers themselves did not change, so
            // this is a rename, but RenderType still exists for everything that is not a chunk layer. The
            // patterns below only match the type in a declaration or a factory call; identifiers such as
            // getRenderType, ChunkRenderTypeSet and ItemBlockRenderTypes are deliberately left alone.
            replace("import net.minecraft.client.renderer.RenderType;", "import net.minecraft.client.renderer.chunk.ChunkSectionLayer;")
            replace("RenderType.solid()", "ChunkSectionLayer.SOLID")
            replace("RenderType.cutoutMipped()", "ChunkSectionLayer.CUTOUT_MIPPED")
            replace("RenderType.cutout()", "ChunkSectionLayer.CUTOUT")
            replace("RenderType.translucent()", "ChunkSectionLayer.TRANSLUCENT")
            replace("CallbackInfoReturnable<RenderType>", "CallbackInfoReturnable<ChunkSectionLayer>")
            replace("RenderType ", "ChunkSectionLayer ")
            replace("RenderType,", "ChunkSectionLayer,")

            // The chunk render region was renamed alongside them.
            replace("RenderChunkRegion", "RenderSectionRegion")
        }
    }
}
