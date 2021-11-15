package io.github.haykam821.deathswap.game.map;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep.Carver;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import xyz.nucleoid.plasmid.game.world.generator.GameChunkGenerator;

public final class DeathSwapChunkGenerator extends GameChunkGenerator {
	private final DeathSwapMapConfig mapConfig;
	private final long seed;
	private final ChunkGenerator chunkGenerator;

	public DeathSwapChunkGenerator(MinecraftServer server, DeathSwapMapConfig mapConfig) {
		super(server);
		this.mapConfig = mapConfig;

		this.seed = server.getOverworld().getRandom().nextLong();
		BiomeSource biomeSource = new VanillaLayeredBiomeSource(this.seed, false, false, server.getRegistryManager().get(Registry.BIOME_KEY));
		
		ChunkGeneratorSettings chunkGeneratorSettings = BuiltinRegistries.CHUNK_GENERATOR_SETTINGS.get(mapConfig.getChunkGeneratorSettingsId());
		this.chunkGenerator = new NoiseChunkGenerator(biomeSource, this.seed, () -> chunkGeneratorSettings);
	}

	private boolean isChunkPosWithinArea(ChunkPos chunkPos) {
		return chunkPos.x >= 0 && chunkPos.z >= 0 && chunkPos.x < this.mapConfig.getX() && chunkPos.z < this.mapConfig.getZ();
	}

	private boolean isChunkWithinArea(Chunk chunk) {
		return this.isChunkPosWithinArea(chunk.getPos());
	}

	@Override
	public void populateBiomes(Registry<Biome> registry, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			this.chunkGenerator.populateBiomes(registry, chunk);
		} else {
			super.populateBiomes(registry, chunk);
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
	public CompletableFuture<Chunk> populateNoise(Executor executor, StructureAccessor structures, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			return this.chunkGenerator.populateNoise(executor, structures, chunk);
		}
		return super.populateNoise(executor, structures, chunk);
	}

	@Override
	public void buildSurface(ChunkRegion region, Chunk chunk) {
		if (this.isChunkWithinArea(chunk)) {
			this.chunkGenerator.buildSurface(region, chunk);
		}
	}

	@Override
	public BiomeSource getBiomeSource() {
		return this.chunkGenerator.getBiomeSource();
	}

	@Override
	public void generateFeatures(ChunkRegion region, StructureAccessor structures) {
		int chunkX = region.getCenterPos().x;
		int chunkZ = region.getCenterPos().z;

		ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
		if (!this.isChunkPosWithinArea(chunkPos)) return;
	
		BlockPos pos = new BlockPos(chunkX * 16, 0, chunkZ * 16);
		Biome biome = this.chunkGenerator.getBiomeSource().getBiomeForNoiseGen((chunkX << 2) + 2, 2, (chunkZ << 2) + 2);
	
		ChunkRandom random = new ChunkRandom();
		long populationSeed = random.setPopulationSeed(this.seed, pos.getX(), pos.getZ());
		
		biome.generateFeatureStep(structures, this.chunkGenerator, region, populationSeed, random, pos);

		this.generateWalls(chunkX, chunkZ, chunkPos, pos, region);
	}

	private void generateWalls(int chunkX, int chunkZ, ChunkPos chunkPos, BlockPos originPos, ChunkRegion region) {
		this.generateWalls(chunkX, chunkZ, originPos, region.getChunk(chunkPos.getStartPos()), region.getRandom());
	}

	private void generateWalls(int chunkX, int chunkZ, BlockPos originPos, Chunk chunk, Random random) {
		int originX = originPos.getX();
		int originZ = originPos.getZ();

		BlockPos.Mutable pos = new BlockPos.Mutable();

		// Top
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				pos.set(x + originX, 255, z + originZ);
				chunk.setBlockState(pos, this.getTopBarrierState(random, pos), false);
			}
		}

		// North
		if (chunkZ == 0) {
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 256; y++) {
					pos.set(x + originX, y, originZ);
					chunk.setBlockState(pos, this.getBarrierState(random, pos), false);
				}
			}
		}

		// East
		if (chunkX == this.mapConfig.getX() - 1) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 256; y++) {
					pos.set(originX + 15, y, z + originZ);
					chunk.setBlockState(pos, this.getBarrierState(random, pos), false);
				}
			}
		}

		// South
		if (chunkZ == this.mapConfig.getZ() - 1) {
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 256; y++) {
					pos.set(x + originX, y, originZ + 15);
					chunk.setBlockState(pos, this.getBarrierState(random, pos), false);
				}
			}
		}

		// West
		if (chunkX == 0) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 256; y++) {
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
	public void carve(long seed, BiomeAccess access, Chunk chunk, Carver carver) {
		if (this.isChunkWithinArea(chunk)) {
			this.chunkGenerator.carve(this.seed, access, chunk, carver);
		}
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
