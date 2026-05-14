package io.github.haykam821.deathswap.game.map;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.deathswap.Main;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class DeathSwapMapConfig {
	public static final Codec<DeathSwapMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			RegistryFixedCodec.create(Registries.WORLD_PRESET).fieldOf("preset").forGetter(mapConfig -> mapConfig.worldPreset),
			ResourceKey.codec(Registries.LEVEL_STEM).optionalFieldOf("dimension_options", LevelStem.OVERWORLD).forGetter(mapConfig -> mapConfig.dimensionOptions),
			RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("excluded_biomes").forGetter(mapConfig -> mapConfig.excludedBiomes),
			BlockStateProvider.CODEC.optionalFieldOf("barrier", BlockStateProvider.simple(Blocks.BARRIER)).forGetter(DeathSwapMapConfig::getBarrier),
			BlockStateProvider.CODEC.optionalFieldOf("top_barrier", BlockStateProvider.simple(Main.BARRIER_AIR)).forGetter(DeathSwapMapConfig::getTopBarrier),
			Codec.INT.optionalFieldOf("x", 32).forGetter(DeathSwapMapConfig::getX),
			Codec.INT.optionalFieldOf("z", 32).forGetter(DeathSwapMapConfig::getZ)
		).apply(instance, DeathSwapMapConfig::new);
	});

	private final Holder<WorldPreset> worldPreset;
	private final ResourceKey<LevelStem> dimensionOptions;
	private final Optional<HolderSet<Biome>> excludedBiomes;
	private final BlockStateProvider barrier;
	private final BlockStateProvider topBarrier;
	private final int x;
	private final int z;

	private ChunkGenerator chunkGenerator;

	public DeathSwapMapConfig(Holder<WorldPreset> worldPreset, ResourceKey<LevelStem> dimensionOptions, Optional<HolderSet<Biome>> excludedBiomes, BlockStateProvider barrier, BlockStateProvider topBarrier, int x, int z) {
		this.worldPreset = worldPreset;
		this.dimensionOptions = dimensionOptions;
		this.excludedBiomes = excludedBiomes;
		this.barrier = barrier;
		this.topBarrier = topBarrier;
		this.x = x;
		this.z = z;
	}

	public LevelStem getDimensionOptions() {
		WorldDimensions registryHolder = this.worldPreset.value().createWorldDimensions();
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

	private boolean isIncludedBiome(Pair<ParameterPoint, Holder<Biome>> pair) {
		return this.excludedBiomes.isEmpty() || !this.excludedBiomes.get().contains(pair.getSecond());
	}

	private ChunkGenerator createChunkGenerator() {
		LevelStem dimensionOptions = this.getDimensionOptions();

		if (this.excludedBiomes.isPresent()) {
			if (dimensionOptions.generator() instanceof NoiseBasedChunkGenerator noiseChunkGenerator) {
				if (noiseChunkGenerator.getBiomeSource() instanceof MultiNoiseBiomeSource biomeSource) {
					List<Pair<ParameterPoint, Holder<Biome>>> entries = biomeSource.parameters()
						.values()
						.stream()
						.filter(this::isIncludedBiome)
						.toList();

					MultiNoiseBiomeSource newBiomeSource = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(entries));

					return new NoiseBasedChunkGenerator(newBiomeSource, noiseChunkGenerator.generatorSettings());
				}

				throw new IllegalArgumentException("Cannot exclude biomes from unsupported biome source");
			}

			throw new IllegalArgumentException("Cannot exclude biomes from unsupported chunk generator");
		}

		return dimensionOptions.generator();
	}
}
