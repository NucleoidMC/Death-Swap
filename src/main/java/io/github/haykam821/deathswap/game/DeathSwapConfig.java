package io.github.haykam821.deathswap.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.deathswap.game.map.DeathSwapMapConfig;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;

public class DeathSwapConfig {
	public static final Codec<DeathSwapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			PlayerConfig.CODEC.fieldOf("players").forGetter(DeathSwapConfig::getPlayerConfig),
			DeathSwapMapConfig.CODEC.fieldOf("map").forGetter(DeathSwapConfig::getMapConfig),
			Codec.INT.optionalFieldOf("initial_swap_ticks", 20 * 60 * 5).forGetter(DeathSwapConfig::getInitialSwapTicks),
			Codec.INT.optionalFieldOf("swap_ticks", 20 * 60 * 2).forGetter(DeathSwapConfig::getSwapTicks),
			Codec.INT.optionalFieldOf("swap_warning_ticks", 20 * 30).forGetter(DeathSwapConfig::getSwapWarningTicks)
		).apply(instance, DeathSwapConfig::new);
	});

	private final PlayerConfig playerConfig;
	private final DeathSwapMapConfig mapConfig;
	private final int initialSwapTicks;
	private final int swapTicks;
	private final int swapWarningTicks;

	public DeathSwapConfig(PlayerConfig playerConfig, DeathSwapMapConfig mapConfig, int initialSwapTicks, int swapTicks, int swapWarningTicks) {
		this.playerConfig = playerConfig;
		this.mapConfig = mapConfig;
		this.initialSwapTicks = initialSwapTicks;
		this.swapTicks = swapTicks;
		this.swapWarningTicks = swapWarningTicks;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public DeathSwapMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public int getInitialSwapTicks() {
		return this.initialSwapTicks;
	}

	public int getSwapTicks() {
		return this.swapTicks;
	}

	public int getSwapWarningTicks() {
		return this.swapWarningTicks;
	}
}
