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
    }
}
