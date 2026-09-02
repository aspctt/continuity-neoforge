package me.pepperbell.continuity.client.render;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.common.util.TriState;

/**
 * Concrete quad backed by a vanilla vertex array, so it can be loaded from and baked back into a
 * {@link BakedQuad} without reformatting the vertex data.
 */
public class MutableQuad implements MutableQuadView {
	protected final int[] data = new int[QUAD_STRIDE];

	@Nullable
	protected TextureAtlasSprite sprite;
	protected RenderMaterial material = MaterialFinder.DEFAULT_MATERIAL;
	protected int colorIndex = -1;
	@Nullable
	protected Direction cullFace;
	protected Direction lightFace = Direction.UP;
	@Nullable
	protected Direction nominalFace;
	protected int tag;

	/** Set by every mutator, so an untouched quad can be forwarded by reference instead of rebuilt. */
	protected boolean dirty;

	public int[] data() {
		return data;
	}

	public void clear() {
		Arrays.fill(data, 0);
		for (int i = 0; i < VERTEX_COUNT; i++) {
			data[i * VERTEX_STRIDE + OFFSET_COLOR] = -1;
		}
		sprite = null;
		material = MaterialFinder.DEFAULT_MATERIAL;
		colorIndex = -1;
		cullFace = null;
		lightFace = Direction.UP;
		nominalFace = null;
		tag = 0;
		dirty = false;
	}

	// Reading

	@Override
	@Nullable
	public TextureAtlasSprite sprite() {
		return sprite;
	}

	@Override
	public RenderMaterial material() {
		return material;
	}

	@Override
	public int colorIndex() {
		return colorIndex;
	}

	@Override
	@Nullable
	public Direction cullFace() {
		return cullFace;
	}

	@Override
	public Direction lightFace() {
		return lightFace;
	}

	@Override
	public Direction nominalFace() {
		return nominalFace == null ? lightFace : nominalFace;
	}

	@Override
	public float x(int vertexIndex) {
		return Float.intBitsToFloat(data[vertexIndex * VERTEX_STRIDE + OFFSET_X]);
	}

	@Override
	public float y(int vertexIndex) {
		return Float.intBitsToFloat(data[vertexIndex * VERTEX_STRIDE + OFFSET_Y]);
	}

	@Override
	public float z(int vertexIndex) {
		return Float.intBitsToFloat(data[vertexIndex * VERTEX_STRIDE + OFFSET_Z]);
	}

	@Override
	public float posByIndex(int vertexIndex, int coordinateIndex) {
		return Float.intBitsToFloat(data[vertexIndex * VERTEX_STRIDE + OFFSET_X + coordinateIndex]);
	}

	@Override
	public int color(int vertexIndex) {
		return ColorHelper.vanillaToArgb(data[vertexIndex * VERTEX_STRIDE + OFFSET_COLOR]);
	}

	@Override
	public float u(int vertexIndex) {
		return Float.intBitsToFloat(data[vertexIndex * VERTEX_STRIDE + OFFSET_U]);
	}

	@Override
	public float v(int vertexIndex) {
		return Float.intBitsToFloat(data[vertexIndex * VERTEX_STRIDE + OFFSET_V]);
	}

	@Override
	public int lightmap(int vertexIndex) {
		return data[vertexIndex * VERTEX_STRIDE + OFFSET_LIGHTMAP];
	}

	@Override
	public boolean hasNormal(int vertexIndex) {
		return data[vertexIndex * VERTEX_STRIDE + OFFSET_NORMAL] != 0;
	}

	@Override
	public float normalX(int vertexIndex) {
		return unpackNormalComponent(data[vertexIndex * VERTEX_STRIDE + OFFSET_NORMAL], 0);
	}

	@Override
	public float normalY(int vertexIndex) {
		return unpackNormalComponent(data[vertexIndex * VERTEX_STRIDE + OFFSET_NORMAL], 8);
	}

	@Override
	public float normalZ(int vertexIndex) {
		return unpackNormalComponent(data[vertexIndex * VERTEX_STRIDE + OFFSET_NORMAL], 16);
	}

	@Override
	public int tag() {
		return tag;
	}

	@Override
	public void copyTo(MutableQuadView target) {
		target.copyFrom(this);
	}

	// Writing

	@Override
	public MutableQuad sprite(@Nullable TextureAtlasSprite sprite) {
		this.sprite = sprite;
		dirty = true;
		return this;
	}

	@Override
	public MutableQuad material(RenderMaterial material) {
		this.material = material;
		dirty = true;
		return this;
	}

	@Override
	public MutableQuad colorIndex(int colorIndex) {
		this.colorIndex = colorIndex;
		dirty = true;
		return this;
	}

	@Override
	public MutableQuad cullFace(@Nullable Direction face) {
		cullFace = face;
		dirty = true;
		if (face != null) {
			nominalFace = face;
		}
		return this;
	}

	@Override
	public MutableQuad nominalFace(@Nullable Direction face) {
		nominalFace = face;
		dirty = true;
		if (face != null) {
			lightFace = face;
		}
		return this;
	}

	public MutableQuad lightFace(Direction face) {
		lightFace = face;
		dirty = true;
		return this;
	}

	@Override
	public MutableQuad tag(int tag) {
		this.tag = tag;
		dirty = true;
		return this;
	}

	@Override
	public MutableQuad pos(int vertexIndex, float x, float y, float z) {
		int base = vertexIndex * VERTEX_STRIDE;
		data[base + OFFSET_X] = Float.floatToRawIntBits(x);
		data[base + OFFSET_Y] = Float.floatToRawIntBits(y);
		data[base + OFFSET_Z] = Float.floatToRawIntBits(z);
		dirty = true;
		return this;
	}

	@Override
	public MutableQuad color(int vertexIndex, int color) {
		data[vertexIndex * VERTEX_STRIDE + OFFSET_COLOR] = ColorHelper.argbToVanilla(color);
		dirty = true;
		return this;
	}

	@Override
	public MutableQuad color(int c0, int c1, int c2, int c3) {
		color(0, c0);
		color(1, c1);
		color(2, c2);
		color(3, c3);
		return this;
	}

	@Override
	public MutableQuad uv(int vertexIndex, float u, float v) {
		int base = vertexIndex * VERTEX_STRIDE;
		data[base + OFFSET_U] = Float.floatToRawIntBits(u);
		data[base + OFFSET_V] = Float.floatToRawIntBits(v);
		dirty = true;
		return this;
	}

	@Override
	public MutableQuad lightmap(int vertexIndex, int lightmap) {
		data[vertexIndex * VERTEX_STRIDE + OFFSET_LIGHTMAP] = lightmap;
		dirty = true;
		return this;
	}

	@Override
	public MutableQuad lightmap(int l0, int l1, int l2, int l3) {
		lightmap(0, l0);
		lightmap(1, l1);
		lightmap(2, l2);
		lightmap(3, l3);
		return this;
	}

	@Override
	public MutableQuad normal(int vertexIndex, float x, float y, float z) {
		data[vertexIndex * VERTEX_STRIDE + OFFSET_NORMAL] = packNormal(x, y, z);
		dirty = true;
		return this;
	}

	@Override
	public MutableQuad copyFrom(QuadView source) {
		if (source instanceof MutableQuad quad) {
			System.arraycopy(quad.data, 0, data, 0, QUAD_STRIDE);
			sprite = quad.sprite;
			material = quad.material;
			colorIndex = quad.colorIndex;
			cullFace = quad.cullFace;
			lightFace = quad.lightFace;
			nominalFace = quad.nominalFace;
			tag = quad.tag;
			dirty = quad.dirty;
			return this;
		}

		for (int i = 0; i < VERTEX_COUNT; i++) {
			pos(i, source.x(i), source.y(i), source.z(i));
			color(i, source.color(i));
			uv(i, source.u(i), source.v(i));
			lightmap(i, source.lightmap(i));
			if (source.hasNormal(i)) {
				normal(i, source.normalX(i), source.normalY(i), source.normalZ(i));
			} else {
				data[i * VERTEX_STRIDE + OFFSET_NORMAL] = 0;
			}
		}
		sprite = source.sprite();
		material = source.material();
		colorIndex = source.colorIndex();
		cullFace = source.cullFace();
		lightFace = source.lightFace();
		nominalFace = source.nominalFace();
		tag = source.tag();
		dirty = true;
		return this;
	}

	@Override
	public MutableQuad fromVanilla(BakedQuad quad, @Nullable Direction cullFace) {
		System.arraycopy(quad.getVertices(), 0, data, 0, QUAD_STRIDE);
		sprite = quad.getSprite();
		colorIndex = quad.getTintIndex();
		lightFace = quad.getDirection();
		nominalFace = quad.getDirection();
		this.cullFace = cullFace;
		// The blend mode stays DEFAULT so the quad lands back in whichever render type it came from, even if that is
		// a modded chunk layer this abstraction has no name for.
		material = MaterialFinder.find(BlendMode.DEFAULT, false, !quad.isShade(), quad.hasAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE);
		tag = 0;
		dirty = false;
		return this;
	}

	/**
	 * {@return whether anything has written to this quad since it was loaded}
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * {@return a baked quad holding a copy of the current state of this quad}
	 */
	public BakedQuad toBakedQuad() {
		int[] vertices = new int[QUAD_STRIDE];
		System.arraycopy(data, 0, vertices, 0, QUAD_STRIDE);

		boolean shade = !material.disableDiffuse();
		// A baked quad can only force ambient occlusion off; forcing it on is a model-wide decision on NeoForge.
		boolean hasAmbientOcclusion = material.ambientOcclusion() != TriState.FALSE;

		if (material.emissive()) {
			// Vanilla block rendering overwrites the baked light, so the marker type is what actually drives the
			// full-bright substitution. Writing it into the vertex data as well costs nothing and means any renderer
			// that honours a quad's own lightmap gets it right without needing to know about Continuity.
			for (int i = 0; i < VERTEX_COUNT; i++) {
				vertices[i * VERTEX_STRIDE + OFFSET_LIGHTMAP] = LightTexture.FULL_BRIGHT;
			}
			return new EmissiveBakedQuad(vertices, colorIndex, lightFace, sprite, shade, hasAmbientOcclusion);
		}
		return new BakedQuad(vertices, colorIndex, lightFace, sprite, shade, hasAmbientOcclusion);
	}

	private static float unpackNormalComponent(int packed, int shift) {
		return ((byte) ((packed >> shift) & 0xFF)) / 127.0f;
	}

	private static int packNormal(float x, float y, float z) {
		int packedX = Math.round(Math.clamp(x, -1.0f, 1.0f) * 127.0f) & 0xFF;
		int packedY = Math.round(Math.clamp(y, -1.0f, 1.0f) * 127.0f) & 0xFF;
		int packedZ = Math.round(Math.clamp(z, -1.0f, 1.0f) * 127.0f) & 0xFF;
		return packedX | (packedY << 8) | (packedZ << 16);
	}
}
