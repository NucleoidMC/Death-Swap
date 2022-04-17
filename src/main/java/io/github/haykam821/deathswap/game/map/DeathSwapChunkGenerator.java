package io.github.haykam821.deathswap.game.map;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.MultiNoiseSampler;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep.Carver;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import xyz.nucleoid.plasmid.game.world.generator.GameChunkGenerator;

public final class DeathSwapChunkGenerator extends GameChunkGenerator {
	private final DeathSwapMapConfig mapConfig;
	private final ChunkGenerator chunkGenerator;

	public DeathSwapChunkGenerator(MinecraftServer server, DeathSwapMapConfig mapConfig) {
		super(server);
		this.mapConfig = mapConfig;

		long seed = server.getOverworld().getRandom().nextLong();

		RegistryKey<ChunkGeneratorSettings> chunkGeneratorSettingsKey = RegistryKey.of(Registry.CHUNK_GENERATOR_SETTINGS_KEY, mapConfig.getChunkGeneratorSettingsId());
		this.chunkGenerator = GeneratorOptions.createGenerator(server.getRegistryManager(), seed, chunkGeneratorSettingsKey);
	}

	private boolean isChunkPosWithinArea(ChunkPos chunkPos) {
		return chunkPos.x >= 0 && chunkPos.z >= 0 && chunkPos.x < this.mapConfig.getX() && chunkPos.z < this.mapConfig.getZ();
	}

	private boolean isChunkWithinArea(Chunk chunk) {
		return this.isChunkPosWithinArea(chunk.getPos());
	}

	@Override
	public CompletableFuture<Chunk> populateBiomes(Registry<Biome> registry, Executor executor, Blender blender, StructureAccessor structures, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			return this.chunkGenerator.populateBiomes(registry, executor, blender, structures, chunk);
		} else {
			return super.populateBiomes(registry, executor, blender, structures, chunk);
		}
	}

	@Override
	public void populateEntities(ChunkRegion region) {
		int chunkX = region.getCenterPos().x;
		int chunkZ = region.getCenterPos().z;

		ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
		if (this.isChunkPosWithinArea(chunkPos)) {
			this.chunkGenerator.populateEntities(region);
		} else {
			super.populateEntities(region);
		}
	}

	@Override
	public MultiNoiseSampler getMultiNoiseSampler() {
		return this.chunkGenerator.getMultiNoiseSampler();
	}

	@Override
	public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, StructureAccessor structures, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			return this.chunkGenerator.populateNoise(executor, blender, structures, chunk);
		}
		return super.populateNoise(executor, blender, structures, chunk);
	}

	@Override
	public void buildSurface(ChunkRegion region, StructureAccessor structures, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			this.chunkGenerator.buildSurface(region, structures, chunk);
		}
	}

	@Override
	public BiomeSource getBiomeSource() {
		return this.chunkGenerator.getBiomeSource();
	}

	@Override
	public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structures) {
		ChunkPos chunkPos = chunk.getPos();
		if (!this.isChunkPosWithinArea(chunkPos)) return;
	
		this.chunkGenerator.generateFeatures(world, chunk, structures);

		int chunkX = chunkPos.x;
		int chunkZ = chunkPos.z;

		BlockPos pos = new BlockPos(chunkX * 16, 0, chunkZ * 16);
		this.generateWalls(chunkX, chunkZ, pos, chunk, world.getRandom());
	}

	private void generateWalls(int chunkX, int chunkZ, BlockPos originPos, Chunk chunk, Random random) {
		int originX = originPos.getX();
		int originZ = originPos.getZ();

		BlockPos.Mutable pos = new BlockPos.Mutable();

		int bottomY = chunk.getBottomY();
		int topY = chunk.getTopY() - 1;

		// Top
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				pos.set(x + originX, chunk.getTopY(), z + originZ);
				chunk.setBlockState(pos, this.getTopBarrierState(random, pos), false);
			}
		}

		// North
		if (chunkZ == 0) {
			for (int x = 0; x < 16; x++) {
				for (int y = bottomY; y < topY; y++) {
					pos.set(x + originX, y, originZ);
					chunk.setBlockState(pos, this.getBarrierState(random, pos), false);
				}
			}
		}

		// East
		if (chunkX == this.mapConfig.getX() - 1) {
			for (int z = 0; z < 16; z++) {
				for (int y = bottomY; y < topY; y++) {
					pos.set(originX + 15, y, z + originZ);
					chunk.setBlockState(pos, this.getBarrierState(random, pos), false);
				}
			}
		}

		// South
		if (chunkZ == this.mapConfig.getZ() - 1) {
			for (int x = 0; x < 16; x++) {
				for (int y = bottomY; y < topY; y++) {
					pos.set(x + originX, y, originZ + 15);
					chunk.setBlockState(pos, this.getBarrierState(random, pos), false);
				}
			}
		}

		// West
		if (chunkX == 0) {
			for (int z = 0; z < 16; z++) {
				for (int y = bottomY; y < topY; y++) {
					pos.set(originX, y, z + originZ);
					chunk.setBlockState(pos, this.getBarrierState(random, pos), false);
				}
			}
		}
	}

	private BlockState getTopBarrierState(Random random, BlockPos pos) {
		return this.mapConfig.getTopBarrier().getBlockState(random, pos);
	}

	private BlockState getBarrierState(Random random, BlockPos pos) {
		return this.mapConfig.getBarrier().getBlockState(random, pos);
	}

	@Override
	public void carve(ChunkRegion region, long seed, BiomeAccess access, StructureAccessor structures, Chunk chunk, Carver carver) {
		if (this.isChunkWithinArea(chunk)) {
			this.chunkGenerator.carve(region, seed, access, structures, chunk, carver);
		}
	}

	@Override
	public int getSeaLevel() {
		return this.chunkGenerator.getSeaLevel();
	}

	@Override
	public int getMinimumY() {
		return this.chunkGenerator.getMinimumY();
	}

	@Override
	public int getWorldHeight() {
		return this.chunkGenerator.getWorldHeight();
	}

	@Override
	public int getHeight(int x, int z, Heightmap.Type heightmapType, HeightLimitView world) {
		if (this.isChunkPosWithinArea(new ChunkPos(x >> 4, z >> 4))) {
			return this.chunkGenerator.getHeight(x, z, heightmapType, world);
		}
		return super.getHeight(x, z, heightmapType, world);
	}

	@Override
	public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world) {
		if (this.isChunkPosWithinArea(new ChunkPos(x >> 4, z >> 4))) {
			return this.chunkGenerator.getColumnSample(x, z, world);
		}
		return super.getColumnSample(x, z, world);
	}
}
