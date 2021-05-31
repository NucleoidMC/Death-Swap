package io.github.haykam821.deathswap.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockBox;

public final class DeathSwapMap {
	private final DeathSwapMapConfig mapConfig;
	private final DeathSwapChunkGenerator chunkGenerator;
	private final BlockBox box;

	public DeathSwapMap(MinecraftServer server, DeathSwapMapConfig mapConfig) {
		this.mapConfig = mapConfig;
		this.chunkGenerator = new DeathSwapChunkGenerator(server, this.mapConfig);
		this.box = new BlockBox(1, 1, 1, mapConfig.getX() * 16 - 2, 254, mapConfig.getZ() * 16 - 2);
	}

	public DeathSwapChunkGenerator getChunkGenerator() {
		return this.chunkGenerator;
	}

	public BlockBox getBox() {
		return this.box;
	}
}
