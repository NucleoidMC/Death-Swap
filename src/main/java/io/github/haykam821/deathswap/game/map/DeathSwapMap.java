package io.github.haykam821.deathswap.game.map;

import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
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
		int bottomY = world.getBottomY();
		int maxY = Math.min(world.getTopY(), bottomY + world.getLogicalHeight()) - 1;

		BlockPos.Mutable pos = new BlockPos.Mutable(x, maxY, z);
		Chunk chunk = world.getChunk(pos);

		int air = 0;

		while (pos.getY() > bottomY) {
			if (chunk.getBlockState(pos).isAir()) {
				air += 1;
			} else if (air > EntityType.PLAYER.getHeight()) {
				air = 0;
				break;
			} else {
				air = 0;
			}

			pos.move(Direction.DOWN);
		}

		if (pos.getY() == bottomY) {
			pos.setY(chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x, z));
		}

		return pos.getY() + 1;
	}
}
