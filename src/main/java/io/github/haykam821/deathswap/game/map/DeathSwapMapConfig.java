package io.github.haykam821.deathswap.game.map;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.deathswap.Main;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryFixedCodec;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.NoiseHypercube;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public class DeathSwapMapConfig {
	public static final Codec<DeathSwapMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			RegistryFixedCodec.of(RegistryKeys.WORLD_PRESET).fieldOf("preset").forGetter(mapConfig -> mapConfig.worldPreset),
			RegistryKey.createCodec(RegistryKeys.DIMENSION).optionalFieldOf("dimension_options", DimensionOptions.OVERWORLD).forGetter(mapConfig -> mapConfig.dimensionOptions),
			RegistryCodecs.entryList(RegistryKeys.BIOME).optionalFieldOf("excluded_biomes").forGetter(mapConfig -> mapConfig.excludedBiomes),
			BlockStateProvider.TYPE_CODEC.optionalFieldOf("barrier", BlockStateProvider.of(Blocks.BARRIER)).forGetter(DeathSwapMapConfig::getBarrier),
			BlockStateProvider.TYPE_CODEC.optionalFieldOf("top_barrier", BlockStateProvider.of(Main.BARRIER_AIR)).forGetter(DeathSwapMapConfig::getTopBarrier),
			Codec.INT.optionalFieldOf("x", 32).forGetter(DeathSwapMapConfig::getX),
			Codec.INT.optionalFieldOf("z", 32).forGetter(DeathSwapMapConfig::getZ)
		).apply(instance, DeathSwapMapConfig::new);
	});

	private final RegistryEntry<WorldPreset> worldPreset;
	private final RegistryKey<DimensionOptions> dimensionOptions;
	private final Optional<RegistryEntryList<Biome>> excludedBiomes;
	private final BlockStateProvider barrier;
	private final BlockStateProvider topBarrier;
	private final int x;
	private final int z;

	private ChunkGenerator chunkGenerator;

	public DeathSwapMapConfig(RegistryEntry<WorldPreset> worldPreset, RegistryKey<DimensionOptions> dimensionOptions, Optional<RegistryEntryList<Biome>> excludedBiomes, BlockStateProvider barrier, BlockStateProvider topBarrier, int x, int z) {
		this.worldPreset = worldPreset;
		this.dimensionOptions = dimensionOptions;
		this.excludedBiomes = excludedBiomes;
		this.barrier = barrier;
		this.topBarrier = topBarrier;
		this.x = x;
		this.z = z;
	}

	public DimensionOptions getDimensionOptions() {
		DimensionOptionsRegistryHolder registryHolder = this.worldPreset.value().createDimensionsRegistryHolder();
		return registryHolder.dimensions().get(this.dimensionOptions);
	}

	public BlockStateProvider getBarrier() {
		return this.barrier;
	}

	public BlockStateProvider getTopBarrier() {
		return this.topBarrier;
	}

	public int getX() {
		return this.x;
	}

	public int getZ() {
		return this.z;
	}

	public ChunkGenerator getChunkGenerator() {
		if (this.chunkGenerator == null) {
			this.chunkGenerator = this.createChunkGenerator();
		}

		return this.chunkGenerator;
	}

	private boolean isIncludedBiome(Pair<NoiseHypercube, RegistryEntry<Biome>> pair) {
		return this.excludedBiomes.isEmpty() || !this.excludedBiomes.get().contains(pair.getSecond());
	}

	private ChunkGenerator createChunkGenerator() {
		DimensionOptions dimensionOptions = this.getDimensionOptions();

		if (this.excludedBiomes.isPresent()) {
			if (dimensionOptions.chunkGenerator() instanceof NoiseChunkGenerator noiseChunkGenerator) {
				if (noiseChunkGenerator.getBiomeSource() instanceof MultiNoiseBiomeSource biomeSource) {
					List<Pair<NoiseHypercube, RegistryEntry<Biome>>> entries = biomeSource.getBiomeEntries()
						.getEntries()
						.stream()
						.filter(this::isIncludedBiome)
						.toList();

					MultiNoiseBiomeSource newBiomeSource = MultiNoiseBiomeSource.create(new MultiNoiseUtil.Entries<>(entries));

					return new NoiseChunkGenerator(newBiomeSource, noiseChunkGenerator.getSettings());
				}

				throw new IllegalArgumentException("Cannot exclude biomes from unsupported biome source");
			}

			throw new IllegalArgumentException("Cannot exclude biomes from unsupported chunk generator");
		}

		return dimensionOptions.chunkGenerator();
	}
}
