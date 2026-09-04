package me.pepperbell.continuity.client.resource;

//? if <1.21.2 {
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.pepperbell.continuity.client.mixin.ReloadableResourceManagerAccessor;
import me.pepperbell.continuity.client.mixinterface.MultiPackResourceManagerExtension;
import me.pepperbell.continuity.client.util.BooleanState;
import net.minecraft.server.packs.resources.ResourceManager;
//?}
import net.minecraft.resources.ResourceLocation;

public class ResourceRedirectHandler {
	public static final String SPRITE_PATH_START = "continuity_reserved/";
	public static final String PATH_START = "textures/" + SPRITE_PATH_START;
	public static final int PATH_START_LENGTH = PATH_START.length();
	//? if <1.21.2
	public static final String PATH_END = ".png";

	// Rewrites a sprite id back to the file it stands for. Tiles named by a path rather than a sprite id are
	// given an id under SPRITE_PATH_START so they can be stitched onto the atlas. From 1.21.2 that id simply
	// carries the optifine path, because the game no longer rejects it; before then the path had to be hidden
	// behind an index and the id validation suppressed, which is what the block below does.
	//? if >=1.21.2 {
	/*public static ResourceLocation redirect(ResourceLocation id) {
		String path = id.getPath();
		if (!path.startsWith(PATH_START)) {
			return id;
		}

		return id.withPath("optifine/" + path.substring(PATH_START_LENGTH));
	}
	*///?}

	//? if <1.21.2 {
	public static final int PATH_END_LENGTH = PATH_END.length();
	public static final int HEX_LENGTH = 8;
	public static final int HEX_END = PATH_START_LENGTH + HEX_LENGTH;
	public static final int MIN_LENGTH = PATH_START_LENGTH + HEX_LENGTH + PATH_END_LENGTH;

	private static final char[] HEX_BUFFER = new char[HEX_LENGTH];
	private static final char[] HEX_DIGITS = {
			'0', '1', '2', '3',
			'4', '5', '6', '7',
			'8', '9', 'a', 'b',
			'c', 'd', 'e', 'f'
	};

	private final ObjectList<RedirectInfo> redirects = new ObjectArrayList<>();
	private final Object2IntMap<String> indexMap = new Object2IntOpenHashMap<>();
	private int nextIndex = 0;

	{
		indexMap.defaultReturnValue(-1);
	}

	@Nullable
	public static ResourceRedirectHandler get(ResourceManager resourceManager) {
		if (resourceManager instanceof ReloadableResourceManagerAccessor accessor) {
			resourceManager = accessor.getResources();
		}
		if (resourceManager instanceof MultiPackResourceManagerExtension extension) {
			return extension.continuity$getRedirectHandler();
		}
		return null;
	}

	public String getSourceSpritePath(String absolutePath) {
		int index = indexMap.getInt(absolutePath);
		if (index == -1) {
			RedirectInfo info = RedirectInfo.of(absolutePath);
			index = nextIndex++;
			redirects.add(info);
			indexMap.put(absolutePath, index);
		}
		return SPRITE_PATH_START + toHex(index);
	}

	public ResourceLocation redirect(ResourceLocation id) {
		String path = id.getPath();
		if (!path.startsWith(PATH_START) || !path.endsWith(PATH_END)) {
			return id;
		}

		int length = path.length();
		if (length < MIN_LENGTH) {
			return id;
		}

		int index = parseHex(path, PATH_START_LENGTH);
		if (index < 0 || index >= redirects.size()) {
			return id;
		}

		RedirectInfo info = redirects.get(index);
		String newPath;
		if (length == MIN_LENGTH) {
			newPath = info.defaultPath;
		} else {
			String suffix = path.substring(HEX_END, length - PATH_END_LENGTH);
			newPath = info.createPath(suffix);
		}

		BooleanState invalidIdentifierState = InvalidIdentifierStateHolder.get();
		invalidIdentifierState.enable();
		ResourceLocation newId = id.withPath(newPath);
		invalidIdentifierState.disable();

		return newId;
	}

	public static int parseHex(String string, int startIndex) {
		int i = 0;
		int charPos = startIndex;
		int endIndex = startIndex + HEX_LENGTH;
		while (charPos < endIndex) {
			i <<= 4;
			char c = string.charAt(charPos++);
			if (c >= '0' && c <= '9') {
				i |= c - '0';
			} else if (c >= 'a' && c <= 'f') {
				i |= c - 'a' + 10;
			} else {
				return -1;
			}
		}
		return i;
	}

	public static String toHex(int i) {
		int charPos = HEX_LENGTH;
		do {
			HEX_BUFFER[--charPos] = HEX_DIGITS[i & 15];
			i >>>= 4;
		} while (charPos > 0);
		return new String(HEX_BUFFER);
	}

	private static abstract class RedirectInfo {
		public final String defaultPath;

		protected RedirectInfo(String defaultPath) {
			this.defaultPath = defaultPath;
		}

		public abstract String createPath(String suffix);

		public static RedirectInfo of(String path) {
			int extensionIndex = FilenameUtils.indexOfExtension(path);
			if (extensionIndex != -1) {
				String pathStart = path.substring(0, extensionIndex);
				String pathEnd = path.substring(extensionIndex);
				return new RedirectInfo(path) {
					@Override
					public String createPath(String suffix) {
						return pathStart + suffix + pathEnd;
					}
				};
			} else {
				return new RedirectInfo(path) {
					@Override
					public String createPath(String suffix) {
						return path + suffix;
					}
				};
			}
		}
	}
	//?}
}
