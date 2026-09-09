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

        string(current.parsed >= "1.21.9") {
            // The metadata section argument of loadAndStitch narrowed from a collection to a set, which changes
            // the descriptor the sprite loader injections match on.
            replace("ILjava/util/concurrent/Executor;Ljava/util/Collection;)", "ILjava/util/concurrent/Executor;Ljava/util/Set;)")
        }

        string(current.parsed >= "1.21.11") {
            // ResourceLocation became Identifier. The mod has its own InvalidIdentifierStateHolder, but no bare
            // Identifier of its own, so these patterns cannot collide with it in either direction.
            replace("import net.minecraft.resources.ResourceLocation;", "import net.minecraft.resources.Identifier;")
            replace("import net.minecraft.ResourceLocationException;", "import net.minecraft.IdentifierException;")
            replace("Lnet/minecraft/resources/ResourceLocation;", "Lnet/minecraft/resources/Identifier;")
            replace("ResourceLocationException ", "IdentifierException ")
            replace("ResourceLocation ", "Identifier ")
            replace("ResourceLocation,", "Identifier,")
            replace("ResourceLocation>", "Identifier>")
            replace("ResourceLocation.", "Identifier.")

            // The sprite suppliers gained names of their own rather than being spelled out as functions.
            replace("SpriteSource.SpriteSupplier", "SpriteSource.DiscardableLoader")
            replace("Function<SpriteResourceLoader, SpriteContents>", "SpriteSource.Loader")

            // The mipped and unmipped cutout chunk layers merged into a single mipped one.
            replace("ChunkSectionLayer.CUTOUT_MIPPED", "ChunkSectionLayer.CUTOUT")

            // RenderType moved down a package. Only ItemBlockRenderTypesMixin names it, once as a fully
            // qualified type and once inside an injection descriptor; every other file reaches the type through
            // an import that the 1.21.6 block rewrites to the chunk layer.
            replace("net.minecraft.client.renderer.RenderType", "net.minecraft.client.renderer.rendertype.RenderType")
            replace("Lnet/minecraft/client/renderer/RenderType;", "Lnet/minecraft/client/renderer/rendertype/RenderType;")
        }

        string(current.parsed >= "26.1") {
            // 26.1 reorganised the client model packages. The types keep their shape, so these are import
            // moves rather than API changes; only the model part was renamed as well.
            replace("import net.minecraft.client.renderer.block.model.BakedQuad;", "import net.minecraft.client.resources.model.geometry.BakedQuad;")
            replace("import net.minecraft.client.renderer.block.model.BlockStateModel;", "import net.minecraft.client.renderer.block.dispatch.BlockStateModel;")
            replace("import net.minecraft.client.renderer.block.model.BlockModelPart;", "import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;")
            replace("import net.minecraft.client.resources.model.Material;", "import net.minecraft.client.resources.model.sprite.Material;")
            replace("import net.minecraft.world.level.BlockAndTintGetter;", "import net.minecraft.client.renderer.block.BlockAndTintGetter;")

            // BlockModelPart became BlockStateModelPart. This mod's own part is named ProcessedModelPart so
            // that these patterns cannot catch it.
            replace("BlockModelPart ", "BlockStateModelPart ")
            replace("BlockModelPart>", "BlockStateModelPart>")
        }
    }
}
