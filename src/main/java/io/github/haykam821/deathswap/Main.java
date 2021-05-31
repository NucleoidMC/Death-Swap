package io.github.haykam821.deathswap;

import io.github.haykam821.deathswap.block.BarrierAirBlock;
import io.github.haykam821.deathswap.game.DeathSwapConfig;
import io.github.haykam821.deathswap.game.phase.DeathSwapWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	public static final String MOD_ID = "deathswap";

	private static final Identifier BARRIER_AIR_ID = new Identifier(MOD_ID, "barrier_air");
	public static final Block BARRIER_AIR = new BarrierAirBlock(FabricBlockSettings.copyOf(Blocks.AIR).strength(-1, 3600000));

	private static final Identifier DEATH_SWAP_ID = new Identifier(MOD_ID, "death_swap");
	public static final GameType<DeathSwapConfig> DEATH_SWAP_TYPE = GameType.register(DEATH_SWAP_ID, DeathSwapWaitingPhase::open, DeathSwapConfig.CODEC);

	@Override
	public void onInitialize() {
		Registry.register(Registry.BLOCK, BARRIER_AIR_ID, BARRIER_AIR);
	}
}
