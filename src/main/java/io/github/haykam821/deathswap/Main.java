package io.github.haykam821.deathswap;

import io.github.haykam821.deathswap.block.BarrierAirBlock;
import io.github.haykam821.deathswap.game.DeathSwapConfig;
import io.github.haykam821.deathswap.game.phase.DeathSwapWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

public class Main implements ModInitializer {
	private static final String MOD_ID = "deathswap";

	private static final Identifier BARRIER_AIR_ID = Main.identifier("barrier_air");
	private static final ResourceKey<Block> BARRIER_AIR_KEY = ResourceKey.create(Registries.BLOCK, BARRIER_AIR_ID);
	public static final Block BARRIER_AIR = new BarrierAirBlock(Block.Properties.ofFullCopy(Blocks.AIR).strength(-1, 3600000).setId(BARRIER_AIR_KEY));

	private static final Identifier DEATH_SWAP_ID = Main.identifier("death_swap");
	public static final GameType<DeathSwapConfig> DEATH_SWAP_TYPE = GameTypes.register(DEATH_SWAP_ID, DeathSwapConfig.CODEC, DeathSwapWaitingPhase::open);

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.BLOCK, BARRIER_AIR_KEY, BARRIER_AIR);
	}

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
