package me.pepperbell.continuity.client.config;

import me.pepperbell.continuity.client.ContinuityClient;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.minecraft.network.chat.Component;

/**
 * Gives Continuity a page of its own in Sodium's video settings, holding the same options as its config screen.
 *
 * <p>Sodium finds this class through the {@code sodium:config_api_user} mod property and instantiates it itself.
 * Nothing in Continuity names it, so it is never loaded when Sodium is absent, and it is only compiled at all on
 * the targets whose Sodium build carries the config API.
 */
public class SodiumConfigImpl implements ConfigEntryPoint {
	@Override
	public void registerConfigLate(ConfigBuilder builder) {
		ContinuityConfig config = ContinuityConfig.INSTANCE;

		OptionPageBuilder page = builder.createOptionPage()
				.setName(Component.translatable(ContinuityConfigScreen.getTranslationKey("title")))
				.addOption(booleanOption(builder, config, config.connectedTextures))
				.addOption(booleanOption(builder, config, config.emissiveTextures));
		// Custom block layers stop being a feature on 26.1, so there is nothing to offer there.
		//? if <26.1
		page = page.addOption(booleanOption(builder, config, config.customBlockLayers));

		builder.registerOwnModOptions()
				.setNonTintedIcon(ContinuityClient.asId("icon.png"))
				.addPage(page);
	}

	private static BooleanOptionBuilder booleanOption(ConfigBuilder builder, ContinuityConfig config, Option.BooleanOption option) {
		String translationKey = ContinuityConfigScreen.getTranslationKey(option.getKey());

		return builder.createBooleanOption(ContinuityClient.asId(option.getKey()))
				.setName(Component.translatable(translationKey))
				.setTooltip(Component.translatable(ContinuityConfigScreen.getTooltipKey(translationKey)))
				.setDefaultValue(option.getDefault())
				.setBinding(option::set, option::get)
				.setStorageHandler(config::save)
				// Every one of these changes what the models bake to, so the terrain has to be built again.
				.setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD);
	}
}
