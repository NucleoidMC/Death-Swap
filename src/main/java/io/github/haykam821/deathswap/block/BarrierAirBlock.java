package io.github.haykam821.deathswap.block;

import eu.pb4.polymer.api.block.PolymerBlock;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class BarrierAirBlock extends AirBlock implements PolymerBlock {
	public BarrierAirBlock(Block.Settings settings) {
		super(settings);
	}

	@Override
	public Block getPolymerBlock(BlockState state) {
		return Blocks.BARRIER;
	}
}
