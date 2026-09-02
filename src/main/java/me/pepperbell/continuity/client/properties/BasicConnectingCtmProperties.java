package me.pepperbell.continuity.client.properties;

import java.util.Locale;
import java.util.Properties;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import me.pepperbell.continuity.client.util.SpriteCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;

public class BasicConnectingCtmProperties extends BaseCtmProperties {
	protected ConnectionPredicate connectionPredicate;

	public BasicConnectingCtmProperties(Properties properties, ResourceLocation resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method) {
		super(properties, resourceId, pack, packPriority, resourceManager, method);
	}

	@Override
	public void init() {
		super.init();
		parseConnect();
		detectConnect();
		validateConnect();
	}

	protected void parseConnect() {
		String connectStr = properties.getProperty("connect");
		if (connectStr == null) {
			return;
		}

		try {
			connectionPredicate = ConnectionType.valueOf(connectStr.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			//
		}
	}

	protected void detectConnect() {
		if (connectionPredicate == null) {
			if (matchBlocksPredicate != null) {
				connectionPredicate = ConnectionType.BLOCK;
			} else if (matchTilesSet != null) {
				connectionPredicate = ConnectionType.TILE;
			}
		}
	}

	protected void validateConnect() {
		if (connectionPredicate == null) {
			ContinuityClient.LOGGER.error("No valid connection type provided in file '" + resourceId + "' in pack '" + packId + "'");
			valid = false;
		}
	}

	public ConnectionPredicate getConnectionPredicate() {
		return connectionPredicate;
	}

	public enum ConnectionType implements ConnectionPredicate {
		BLOCK {
			@Override
			public boolean shouldConnect(BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, BlockState otherAppearanceState, BlockState otherState, BlockPos otherPos, Direction face, TextureAtlasSprite quadSprite) {
				return appearanceState.getBlock() == otherAppearanceState.getBlock();
			}
		},
		TILE {
			@Override
			public boolean shouldConnect(BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, BlockState otherAppearanceState, BlockState otherState, BlockPos otherPos, Direction face, TextureAtlasSprite quadSprite) {
				if (appearanceState == otherAppearanceState) {
					return true;
				}
				return quadSprite == SpriteCalculator.getSprite(otherAppearanceState, face);
			}
		},
		STATE {
			@Override
			public boolean shouldConnect(BlockAndTintGetter blockView, BlockState appearanceState, BlockState state, BlockPos pos, BlockState otherAppearanceState, BlockState otherState, BlockPos otherPos, Direction face, TextureAtlasSprite quadSprite) {
				return appearanceState == otherAppearanceState;
			}
		};
	}
}
