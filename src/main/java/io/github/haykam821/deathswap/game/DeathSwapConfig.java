package io.github.haykam821.deathswap.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.deathswap.game.map.DeathSwapMapConfig;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public class DeathSwapConfig {
	public static final MapCodec<DeathSwapConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
		return instance.group(
			WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(DeathSwapConfig::getPlayerConfig),
			DeathSwapMapConfig.CODEC.fieldOf("map").forGetter(DeathSwapConfig::getMapConfig),
			IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("ticks_until_close", ConstantIntProvider.create(SharedConstants.TICKS_PER_SECOND * 10)).forGetter(DeathSwapConfig::getTicksUntilClose),
			Codec.INT.optionalFieldOf("initial_swap_ticks", SharedConstants.TICKS_PER_MINUTE * 5).forGetter(DeathSwapConfig::getInitialSwapTicks),
			Codec.INT.optionalFieldOf("swap_ticks", SharedConstants.TICKS_PER_MINUTE * 2).forGetter(DeathSwapConfig::getSwapTicks),
			Codec.INT.optionalFieldOf("swap_warning_ticks", SharedConstants.TICKS_PER_SECOND * 30).forGetter(DeathSwapConfig::getSwapWarningTicks),
			Codec.INT.optionalFieldOf("swap_elimination_collection_ticks", SharedConstants.TICKS_PER_SECOND * 5).forGetter(DeathSwapConfig::getSwapEliminationCollectionTicks)
		).apply(instance, DeathSwapConfig::new);
	});

	private final WaitingLobbyConfig playerConfig;
	private final DeathSwapMapConfig mapConfig;
	private final IntProvider ticksUntilClose;
	private final int initialSwapTicks;
	private final int swapTicks;
	private final int swapWarningTicks;
	private final int swapEliminationCollectionTicks;

	public DeathSwapConfig(WaitingLobbyConfig playerConfig, DeathSwapMapConfig mapConfig, IntProvider ticksUntilClose, int initialSwapTicks, int swapTicks, int swapWarningTicks, int swapEliminationCollectionTicks) {
		this.playerConfig = playerConfig;
		this.mapConfig = mapConfig;
		this.ticksUntilClose = ticksUntilClose;
		this.initialSwapTicks = initialSwapTicks;
		this.swapTicks = swapTicks;
		this.swapWarningTicks = swapWarningTicks;
		this.swapEliminationCollectionTicks = swapEliminationCollectionTicks;
	}

	public WaitingLobbyConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public DeathSwapMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public IntProvider getTicksUntilClose() {
		return this.ticksUntilClose;
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

	public int getSwapEliminationCollectionTicks() {
		return this.swapEliminationCollectionTicks;
	}
}
