package io.github.haykam821.deathswap.block;

import eu.pb4.polymer.block.VirtualBlock;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public class BarrierAirBlock extends AirBlock implements VirtualBlock {
	public BarrierAirBlock(Block.Settings settings) {
		super(settings);
	}

	@Override
	public Block getVirtualBlock() {
		return Blocks.BARRIER;
	}
}
