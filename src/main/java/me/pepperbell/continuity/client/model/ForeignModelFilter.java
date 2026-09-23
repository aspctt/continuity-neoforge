package me.pepperbell.continuity.client.model;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.pepperbell.continuity.api.client.CachingPredicates;
import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;

/**
 * Decides whether a model that another mod supplies is worth wrapping.
 *
 * <p>Wrapping puts one of our models where the mod's own model used to be, so anything that looks the model up again
 * gets ours back. Some mods do exactly that from their own renderers and cast the result to their own model class,
 * which then throws, or test for it and quietly draw nothing. A model the game or NeoForge bakes is never looked up
 * that way, so those are still wrapped whether or not a pack targets them, as upstream does. A model of any other
 * class is only wrapped when a loaded pack has something for the textures it draws, which keeps those mods working
 * unless a pack actually targets them.
 *
 * <p>The textures are read off the model without a level, which is the best that can be done while baking. A block
 * that can take on the appearance of another draws textures that cannot be known then, so its model is wrapped as
 * before.
 */
public final class ForeignModelFilter {
	private static final ClassValue<Boolean> TAKES_APPEARANCE = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			return overridesGetAppearance(type);
		}
	};
	@Nullable
	private static final Method DEFAULT_GET_APPEARANCE = findDefaultGetAppearance();

	private final CachingPredicates[] spritePredicates;
	private final CachingPredicates[] otherPredicates;
	private final Reference2ObjectOpenHashMap<TextureAtlasSprite, CachingPredicates[]> predicatesBySprite = new Reference2ObjectOpenHashMap<>();

	public ForeignModelFilter(List<QuadProcessors.ProcessorHolder> processorHolders) {
		List<CachingPredicates> spritePredicates = new ObjectArrayList<>();
		List<CachingPredicates> otherPredicates = new ObjectArrayList<>();
		for (QuadProcessors.ProcessorHolder holder : processorHolders) {
			CachingPredicates predicates = holder.predicates();
			if (predicates.affectsSprites()) {
				spritePredicates.add(predicates);
			} else {
				otherPredicates.add(predicates);
			}
		}
		this.spritePredicates = spritePredicates.toArray(CachingPredicates[]::new);
		this.otherPredicates = otherPredicates.toArray(CachingPredicates[]::new);
	}

	public static boolean isForeign(Object model) {
		String name = model.getClass().getName();
		return !name.startsWith("net.minecraft.") && !name.startsWith("net.neoforged.");
	}

	public static boolean takesAppearance(Block block) {
		return TAKES_APPEARANCE.get(block.getClass());
	}

	/**
	 * Matches the lookup {@link QuadProcessors} makes for each quad at render time, so a model is wrapped exactly when
	 * at least one of its quads would be handed a processor.
	 */
	public boolean hasConnectedTextures(BlockState state, Set<TextureAtlasSprite> sprites) {
		for (CachingPredicates predicates : otherPredicates) {
			if (affectsState(predicates, state)) {
				return true;
			}
		}
		for (TextureAtlasSprite sprite : sprites) {
			// Many states draw the same few textures, so the predicates are only ever scanned once per texture.
			for (CachingPredicates predicates : predicatesBySprite.computeIfAbsent(sprite, this::findSpritePredicates)) {
				if (affectsState(predicates, state)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean hasEmissive(Set<TextureAtlasSprite> sprites) {
		EmissiveSpriteApi api = EmissiveSpriteApi.get();
		for (TextureAtlasSprite sprite : sprites) {
			if (api.getEmissiveSprite(sprite) != null) {
				return true;
			}
		}
		return false;
	}

	private CachingPredicates[] findSpritePredicates(TextureAtlasSprite sprite) {
		List<CachingPredicates> list = new ObjectArrayList<>();
		for (CachingPredicates predicates : spritePredicates) {
			if (predicates.affectsSprite(sprite)) {
				list.add(predicates);
			}
		}
		return list.toArray(CachingPredicates[]::new);
	}

	private static boolean affectsState(CachingPredicates predicates, BlockState state) {
		return !predicates.affectsBlockStates() || predicates.affectsBlockState(state);
	}

	// The level parameter changed type in 26.1, so the signature is taken from NeoForge's own declaration rather than
	// spelled out here.
	@Nullable
	private static Method findDefaultGetAppearance() {
		for (Method method : IBlockExtension.class.getDeclaredMethods()) {
			if (method.getName().equals("getAppearance") && method.getParameterCount() == 6) {
				return method;
			}
		}
		return null;
	}

	private static boolean overridesGetAppearance(Class<?> type) {
		if (DEFAULT_GET_APPEARANCE == null) {
			return true;
		}
		try {
			return type.getMethod(DEFAULT_GET_APPEARANCE.getName(), DEFAULT_GET_APPEARANCE.getParameterTypes()).getDeclaringClass() != IBlockExtension.class;
		} catch (NoSuchMethodException e) {
			return true;
		}
	}
}
