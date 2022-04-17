package io.github.haykam821.deathswap.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.dimension.DimensionType;

public final class DeathSwapMap {
	private final DeathSwapMapConfig mapConfig;
	private final DeathSwapChunkGenerator chunkGenerator;
	private final BlockBox box;

	public DeathSwapMap(MinecraftServer server, DeathSwapMapConfig mapConfig, DimensionType dimensionType) {
		this.mapConfig = mapConfig;
		this.chunkGenerator = new DeathSwapChunkGenerator(server, this.mapConfig);

		int minY = dimensionType.getMinimumY();
		int maxY = minY + dimensionType.getHeight();

		this.box = new BlockBox(1, minY + 1, 1, mapConfig.getX() * 16 - 2, maxY - 1, mapConfig.getZ() * 16 - 2);
	}

	public DeathSwapChunkGenerator getChunkGenerator() {
		return this.chunkGenerator;
	}

	public BlockBox getBox() {
		return this.box;
	}
}
