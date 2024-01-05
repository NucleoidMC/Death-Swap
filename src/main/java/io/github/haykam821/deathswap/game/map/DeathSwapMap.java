package io.github.haykam821.deathswap.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.world.Heightmap;
import net.minecraft.world.dimension.DimensionType;

public final class DeathSwapMap {
	private final DeathSwapMapConfig mapConfig;
	private final DeathSwapChunkGenerator chunkGenerator;
	private final BlockBox box;

	public DeathSwapMap(MinecraftServer server, DeathSwapMapConfig mapConfig, DimensionType dimensionType) {
		this.mapConfig = mapConfig;
		this.chunkGenerator = new DeathSwapChunkGenerator(server, this.mapConfig);

		int minY = dimensionType.minY();
		int maxY = minY + dimensionType.height();

		this.box = new BlockBox(1, minY + 1, 1, mapConfig.getX() * 16 - 2, maxY - 1, mapConfig.getZ() * 16 - 2);
	}

	public DeathSwapChunkGenerator getChunkGenerator() {
		return this.chunkGenerator;
	}

	public BlockBox getBox() {
		return this.box;
	}

	public int getSurfaceY(ServerWorld world, int x, int z) {
		return this.chunkGenerator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE, world, world.getChunkManager().getNoiseConfig());
	}
}
