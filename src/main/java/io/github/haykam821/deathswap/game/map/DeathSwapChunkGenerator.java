package io.github.haykam821.deathswap.game.map;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.RandomState;
import org.jspecify.annotations.Nullable;
import xyz.nucleoid.fantasy.util.ChunkGeneratorSettingsProvider;
import xyz.nucleoid.plasmid.api.game.level.generator.GameChunkGenerator;

public final class DeathSwapChunkGenerator extends GameChunkGenerator implements ChunkGeneratorSettingsProvider {
	private final DeathSwapMapConfig mapConfig;
	private final ChunkGenerator chunkGenerator;

	public DeathSwapChunkGenerator(MinecraftServer server, DeathSwapMapConfig mapConfig) {
		super(mapConfig.getChunkGenerator().getBiomeSource());

		this.mapConfig = mapConfig;
		this.chunkGenerator = mapConfig.getChunkGenerator();
	}

	@Override
	public NoiseGeneratorSettings getSettings() {
		if (this.chunkGenerator instanceof NoiseBasedChunkGenerator noiseChunkGenerator) {
			return noiseChunkGenerator.generatorSettings().value();
		}

		return null;
	}

	private boolean isChunkPosWithinArea(ChunkPos chunkPos) {
		return chunkPos.x() >= 0 && chunkPos.z() >= 0 && chunkPos.x() < this.mapConfig.getX() && chunkPos.z() < this.mapConfig.getZ();
	}

	private boolean isChunkWithinArea(ChunkAccess chunk) {
		return this.isChunkPosWithinArea(chunk.getPos());
	}

	@Override
	public CompletableFuture<ChunkAccess> createBiomes(RandomState noiseConfig, Blender blender, StructureManager structures, ChunkAccess chunk) {
		if (this.isChunkWithinArea(chunk)) {
			return this.chunkGenerator.createBiomes(noiseConfig, blender, structures, chunk);
		} else {
			return super.createBiomes(noiseConfig, blender, structures, chunk);
		}
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
		int chunkX = region.getCenter().x();
		int chunkZ = region.getCenter().z();

		ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
		if (this.isChunkPosWithinArea(chunkPos)) {
			this.chunkGenerator.spawnOriginalMobs(region);
		} else {
			super.spawnOriginalMobs(region);
		}
	}

	@Override
	public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState, StructureManager structureManager, BiomeManager biomeManager, @Nullable WorldGenRegion carverBiomeRegion, Set<Holder<Biome>> possibleBiomes) {
		if (this.isChunkWithinArea(chunk)) {
			return this.chunkGenerator.buildTerrain(chunk, blender, randomState, structureManager, biomeManager, carverBiomeRegion, possibleBiomes);
		}

		return super.buildTerrain(chunk, blender, randomState, structureManager, biomeManager, carverBiomeRegion, possibleBiomes);
	}

	@Override
	public BiomeSource getBiomeSource() {
		return this.chunkGenerator.getBiomeSource();
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
		ChunkPos chunkPos = chunk.getPos();
		if (!this.isChunkPosWithinArea(chunkPos)) return;
	
		this.chunkGenerator.applyBiomeDecoration(level, chunk, structures);

		int chunkX = chunkPos.x();
		int chunkZ = chunkPos.z();

		BlockPos pos = new BlockPos(chunkX * 16, 0, chunkZ * 16);
		this.generateWalls(chunkX, chunkZ, pos, chunk, level, level.getRandom());
	}

	private void generateWalls(int chunkX, int chunkZ, BlockPos originPos, ChunkAccess chunk, WorldGenLevel level, RandomSource random) {
		int originX = originPos.getX();
		int originZ = originPos.getZ();

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		int bottomY = chunk.getMinY();
		int topY = chunk.getMaxY();
		// Top
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				pos.set(x + originX, topY, z + originZ);
				chunk.setBlockState(pos, this.getTopBarrierState(level, random, pos));
			}
		}

		// North
		if (chunkZ == 0) {
			for (int x = 0; x < 16; x++) {
				for (int y = bottomY; y < topY; y++) {
					pos.set(x + originX, y, originZ);
					chunk.setBlockState(pos, this.getBarrierState(level, random, pos));
				}
			}
		}

		// East
		if (chunkX == this.mapConfig.getX() - 1) {
			for (int z = 0; z < 16; z++) {
				for (int y = bottomY; y < topY; y++) {
					pos.set(originX + 15, y, z + originZ);
					chunk.setBlockState(pos, this.getBarrierState(level, random, pos));
				}
			}
		}

		// South
		if (chunkZ == this.mapConfig.getZ() - 1) {
			for (int x = 0; x < 16; x++) {
				for (int y = bottomY; y < topY; y++) {
					pos.set(x + originX, y, originZ + 15);
					chunk.setBlockState(pos, this.getBarrierState(level, random, pos));
				}
			}
		}

		// West
		if (chunkX == 0) {
			for (int z = 0; z < 16; z++) {
				for (int y = bottomY; y < topY; y++) {
					pos.set(originX, y, z + originZ);
					chunk.setBlockState(pos, this.getBarrierState(level, random, pos));
				}
			}
		}
	}

	private BlockState getTopBarrierState(WorldGenLevel level, RandomSource random, BlockPos pos) {
		return this.mapConfig.getTopBarrier().getState(level, random, pos);
	}

	private BlockState getBarrierState(WorldGenLevel level, RandomSource random, BlockPos pos) {
		return this.mapConfig.getBarrier().getState(level, random, pos);
	}

	@Override
	public int getSeaLevel() {
		return this.chunkGenerator.getSeaLevel();
	}

	@Override
	public int getMinY() {
		return this.chunkGenerator.getMinY();
	}

	@Override
	public int getGenDepth() {
		return this.chunkGenerator.getGenDepth();
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor level, RandomState noiseConfig) {
		if (this.isChunkPosWithinArea(new ChunkPos(x >> 4, z >> 4))) {
			return this.chunkGenerator.getBaseHeight(x, z, heightmapType, level, noiseConfig);
		}
		return super.getBaseHeight(x, z, heightmapType, level, noiseConfig);
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState noiseConfig) {
		if (this.isChunkPosWithinArea(new ChunkPos(x >> 4, z >> 4))) {
			return this.chunkGenerator.getBaseColumn(x, z, level, noiseConfig);
		}
		return super.getBaseColumn(x, z, level, noiseConfig);
	}
}
