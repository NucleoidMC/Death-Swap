package io.github.haykam821.deathswap.block;

import com.mojang.serialization.MapCodec;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

public class BarrierAirBlock extends AirBlock implements PolymerBlock {
	public BarrierAirBlock(Block.Properties settings) {
		super(settings);
	}

	@Override
	public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
		return Blocks.BARRIER.defaultBlockState();
	}
}
