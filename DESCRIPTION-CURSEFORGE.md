<h1 style="text-align: center;"> Continuity (NeoForge) </h1>

<p style="text-align: center;">
	<img src="https://img.shields.io/badge/Available_for-1.21.1_--_26.2-blue" alt="Version">
	<img src="https://img.shields.io/badge/Requires-Nothing-brightgreen" alt="Requires">
	<img src="https://img.shields.io/badge/License-LGPL--3.0--only-red" alt="License">
</p>

<p style="text-align: center;">
	<img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg" alt="NeoForge">
	<img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/unsupported/forge_vector.svg" alt="Forge">
</p>

<p style="text-align: center;">
	<a href="https://github.com/aspctt/continuity-neoforge"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/github_vector.svg" alt="Available on GitHub"></a>
	<a href="https://modrinth.com/mod/continuity-neoforged"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/modrinth_vector.svg" alt="Available on Modrinth"></a>
	<a href="https://www.curseforge.com/minecraft/mc-mods/continuity-neoforged"><img src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact-minimal/available/curseforge_vector.svg" alt="Available on CurseForge"></a>
</p>

<p>Continuity makes resource packs that use the OptiFine connected textures, emissive textures, and custom block layers formats work without OptiFine. It is client side only, and this build is native NeoForge: no Sinytra Connector, no Fabric API, nothing else to install.</p>

<h3>Resource packs</h3>

<p>Packs are read straight from their <code>optifine/ctm</code> directory, the same layout OptiFine and Continuity on Fabric already use, so nothing needs converting. A file naming a block or texture that is not there is logged and skipped rather than breaking the rest of the pack.</p>

<p>The formats are documented on the <a href="https://github.com/PepperCode1/Continuity/wiki">Continuity wiki</a>.</p>

<h3>Built-in packs</h3>

<p>Two packs ship with the mod, both off by default:</p>

<ul>
	<li><strong>Default Connected Textures</strong> &rarr; connected glass, sandstone, and bookshelves, matching what OptiFine builds in</li>
	<li><strong>Glass Pane Culling Fix</strong> &rarr; culls the faces between stacked glass panes so they read as seamless</li>
</ul>

<h3>A native NeoForge port</h3>

<p>This is a port of <a href="https://github.com/PepperCode1/Continuity">PepperCode1's Continuity</a>, a Fabric mod whose NeoForge releases run under Connector with Forgified Fabric API beneath them. This build needs neither, so install it instead of the upstream one rather than alongside it.</p>

<p>Packs written for Continuity on Fabric are meant to look identical here. Connected textures are not yet visually confirmed on this build.</p>

<h3>Requirements</h3>

<p>NeoForge, and nothing else. A separate file is built for every Minecraft version from 1.21.1 through 1.21.11, and for 26.1 and 26.2. Download the one matching your Minecraft version; each needs the NeoForge line that goes with it, so the 1.21.8 build wants NeoForge 21.8.0 or newer and the 26.2 build wants 26.2.0 or newer.</p>

<h3>License</h3>

<p>LGPL-3.0-only, the same as upstream, with the full terms in <a href="https://github.com/aspctt/continuity-neoforge/blob/main/LICENSE">LICENSE</a>. This is a derivative work: if you distribute the JAR, you must make the source available to whoever you distribute it to.</p>
