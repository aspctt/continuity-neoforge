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
    }
}
