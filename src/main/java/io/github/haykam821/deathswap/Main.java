package io.github.haykam821.deathswap;

import io.github.haykam821.deathswap.block.BarrierAirBlock;
import io.github.haykam821.deathswap.game.DeathSwapConfig;
import io.github.haykam821.deathswap.game.phase.DeathSwapWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;

public class Main implements ModInitializer {
	private static final String MOD_ID = "deathswap";

	private static final Identifier BARRIER_AIR_ID = Main.identifier("barrier_air");
	private static final RegistryKey<Block> BARRIER_AIR_KEY = RegistryKey.of(RegistryKeys.BLOCK, BARRIER_AIR_ID);
	public static final Block BARRIER_AIR = new BarrierAirBlock(Block.Settings.copy(Blocks.AIR).strength(-1, 3600000).registryKey(BARRIER_AIR_KEY));

	private static final Identifier DEATH_SWAP_ID = Main.identifier("death_swap");
	public static final GameType<DeathSwapConfig> DEATH_SWAP_TYPE = GameType.register(DEATH_SWAP_ID, DeathSwapConfig.CODEC, DeathSwapWaitingPhase::open);

	@Override
	public void onInitialize() {
		Registry.register(Registries.BLOCK, BARRIER_AIR_KEY, BARRIER_AIR);
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
