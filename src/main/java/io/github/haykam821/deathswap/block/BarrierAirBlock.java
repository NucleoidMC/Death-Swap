package io.github.haykam821.deathswap.block;

import com.mojang.serialization.MapCodec;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

public class BarrierAirBlock extends AirBlock implements PolymerBlock {
	public static final MapCodec<AirBlock> CODEC = Block.simpleCodec(BarrierAirBlock::new);

	public BarrierAirBlock(Block.Properties settings) {
		super(settings);
	}

	@Override
	public MapCodec<AirBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
		return Blocks.BARRIER.defaultBlockState();
	}
}
