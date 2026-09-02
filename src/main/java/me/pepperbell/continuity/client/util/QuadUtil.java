package me.pepperbell.continuity.client.util;

import me.pepperbell.continuity.client.render.RenderMaterial;
import me.pepperbell.continuity.client.render.MutableQuadView;
import me.pepperbell.continuity.client.render.QuadEmitter;
import me.pepperbell.continuity.client.render.QuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public final class QuadUtil {
	public static void interpolate(MutableQuadView quad, TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite) {
		float oldMinU = oldSprite.getU0();
		float oldMinV = oldSprite.getV0();
		float newMinU = newSprite.getU0();
		float newMinV = newSprite.getV0();
		float uFactor = (newSprite.getU1() - newMinU) / (oldSprite.getU1() - oldMinU);
		float vFactor = (newSprite.getV1() - newMinV) / (oldSprite.getV1() - oldMinV);
		for (int i = 0; i < 4; i++) {
			quad.uv(i,
					newMinU + (quad.u(i) - oldMinU) * uFactor,
					newMinV + (quad.v(i) - oldMinV) * vFactor
			);
		}
	}

	public static void assignLerpedUVs(MutableQuadView quad, TextureAtlasSprite sprite) {
		float delta = sprite.uvShrinkRatio();
		float centerU = (sprite.getU0() + sprite.getU1()) * 0.5f;
		float centerV = (sprite.getV0() + sprite.getV1()) * 0.5f;
		float lerpedMinU = Mth.lerp(delta, sprite.getU0(), centerU);
		float lerpedMaxU = Mth.lerp(delta, sprite.getU1(), centerU);
		float lerpedMinV = Mth.lerp(delta, sprite.getV0(), centerV);
		float lerpedMaxV = Mth.lerp(delta, sprite.getV1(), centerV);
		quad.uv(0, lerpedMinU, lerpedMinV);
		quad.uv(1, lerpedMinU, lerpedMaxV);
		quad.uv(2, lerpedMaxU, lerpedMaxV);
		quad.uv(3, lerpedMaxU, lerpedMinV);
	}

	public static void emitOverlayQuad(QuadEmitter emitter, Direction face, TextureAtlasSprite sprite, int color, RenderMaterial material) {
		emitter.square(face, 0, 0, 1, 1, 0);
		emitter.color(color, color, color, color);
		assignLerpedUVs(emitter, sprite);
		emitter.material(material);
		emitter.emit();
	}

	public static boolean isQuadUnitSquare(QuadView quad) {
		int indexA;
		int indexB;
		switch (quad.lightFace().getAxis()) {
			case X:
				indexA = 1;
				indexB = 2;
				break;
			case Y:
				indexA = 0;
				indexB = 2;
				break;
			case Z:
				indexA = 1;
				indexB = 0;
				break;
			default:
				return false;
		}

		for (int i = 0; i < 4; i++) {
			float a = quad.posByIndex(i, indexA);
			if ((a >= 0.0001f || a <= -0.0001f) && (a >= 1.0001f || a <= 0.9999f)) {
				return false;
			}
			float b = quad.posByIndex(i, indexB);
			if ((b >= 0.0001f || b <= -0.0001f) && (b >= 1.0001f || b <= 0.9999f)) {
				return false;
			}
		}
		return true;
	}

	public static int getTextureOrientation(QuadView quad) {
		int rotation = getUVRotation(quad);
		if (getUVWinding(quad) == Winding.CLOCKWISE) {
			return rotation + 4;
		}
		return rotation;
	}

	public static int getUVRotation(QuadView quad) {
		int minVertex = 0;
		float minDistance = 3.0f;
		for (int vertexId = 0; vertexId < 4; vertexId++) {
			float u = quad.u(vertexId);
			float v = quad.v(vertexId);
			float distance = u * u + v * v;
			if (distance < minDistance) {
				minDistance = distance;
				minVertex = vertexId;
			}
		}
		return minVertex;
	}

	public static Winding getUVWinding(QuadView quad) {
		float u3 = quad.u(3);
		float v3 = quad.v(3);
		float u0 = quad.u(0);
		float v0 = quad.v(0);
		float u1 = quad.u(1);
		float v1 = quad.v(1);

		float value = (u3 - u0) * (v1 - v0) - (v3 - v0) * (u1 - u0);
		if (value > 0) {
			return Winding.COUNTERCLOCKWISE;
		} else if (value < 0) {
			return Winding.CLOCKWISE;
		}
		return Winding.UNDEFINED;
	}

	public enum Winding {
		COUNTERCLOCKWISE,
		CLOCKWISE,
		UNDEFINED;

		public Winding reverse() {
			if (this == UNDEFINED) {
				return this;
			}
			return this == CLOCKWISE ? COUNTERCLOCKWISE : CLOCKWISE;
		}
	}
}
