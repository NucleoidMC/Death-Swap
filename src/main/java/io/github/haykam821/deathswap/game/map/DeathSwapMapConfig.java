package io.github.haykam821.deathswap.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.deathswap.Main;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public class DeathSwapMapConfig {
	public static final Codec<DeathSwapMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Identifier.CODEC.optionalFieldOf("dimension_type_id", DimensionType.OVERWORLD_ID).forGetter(DeathSwapMapConfig::getDimensionTypeId),
			Identifier.CODEC.fieldOf("settings").forGetter(DeathSwapMapConfig::getChunkGeneratorSettingsId),
			BlockStateProvider.TYPE_CODEC.optionalFieldOf("barrier", BlockStateProvider.of(Blocks.BARRIER)).forGetter(DeathSwapMapConfig::getBarrier),
			BlockStateProvider.TYPE_CODEC.optionalFieldOf("top_barrier", BlockStateProvider.of(Main.BARRIER_AIR)).forGetter(DeathSwapMapConfig::getTopBarrier),
			Codec.INT.optionalFieldOf("x", 32).forGetter(DeathSwapMapConfig::getX),
			Codec.INT.optionalFieldOf("z", 32).forGetter(DeathSwapMapConfig::getZ)
		).apply(instance, DeathSwapMapConfig::new);
	});

	private final Identifier dimensionTypeId;
	private final Identifier chunkGeneratorSettingsId;
	private final BlockStateProvider barrier;
	private final BlockStateProvider topBarrier;
	private final int x;
	private final int z;

	public DeathSwapMapConfig(Identifier dimensionTypeId, Identifier chunkGeneratorSettingsId, BlockStateProvider barrier, BlockStateProvider topBarrier, int x, int z) {
		this.dimensionTypeId = dimensionTypeId;
		this.chunkGeneratorSettingsId = chunkGeneratorSettingsId;
		this.barrier = barrier;
		this.topBarrier = topBarrier;
		this.x = x;
		this.z = z;
	}

	public Identifier getDimensionTypeId() {
		return this.dimensionTypeId;
	}

	public DimensionType getDimensionType(MinecraftServer server) {
		DynamicRegistryManager registryManager = server.getRegistryManager();
		Registry<DimensionType> registry = registryManager.get(Registry.DIMENSION_TYPE_KEY);

		return registry.get(this.dimensionTypeId);
	}

	public Identifier getChunkGeneratorSettingsId() {
		return this.chunkGeneratorSettingsId;
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
}
