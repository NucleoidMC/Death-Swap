package io.github.haykam821.deathswap.game.map;

import net.minecraft.world.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;

public final class DeathSwapMap {
	private final DeathSwapMapConfig mapConfig;
	private final DeathSwapChunkGenerator chunkGenerator;
	private final BoundingBox box;

	public DeathSwapMap(MinecraftServer server, DeathSwapMapConfig mapConfig, DimensionType dimensionType) {
		this.mapConfig = mapConfig;
		this.chunkGenerator = new DeathSwapChunkGenerator(server, this.mapConfig);

		int minY = dimensionType.minY();
		int maxY = minY + dimensionType.height();

		this.box = new BoundingBox(1, minY + 1, 1, mapConfig.getX() * 16 - 2, maxY - 1, mapConfig.getZ() * 16 - 2);
	}

	public DeathSwapChunkGenerator getChunkGenerator() {
		return this.chunkGenerator;
	}

	public BoundingBox getBox() {
		return this.box;
	}

	public int getSurfaceY(ServerLevel level, int x, int z) {
		int bottomY = level.getMinY();
		int maxY = Math.min(level.getMaxY(), bottomY + level.getLogicalHeight() - 1);

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, maxY, z);
		ChunkAccess chunk = level.getChunk(pos);

		int air = 0;

		while (pos.getY() > bottomY) {
			if (chunk.getBlockState(pos).isAir()) {
				air += 1;
			} else if (air > EntityTypes.PLAYER.getHeight()) {
				air = 0;
				break;
			} else {
				air = 0;
			}

			pos.move(Direction.DOWN);
		}

		if (pos.getY() == bottomY) {
			pos.setY(chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
		}

		return pos.getY() + 1;
	}
}
