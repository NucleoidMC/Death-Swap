package io.github.haykam821.deathswap.block;

import com.mojang.serialization.MapCodec;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class BarrierAirBlock extends AirBlock implements PolymerBlock {
	public static final MapCodec<AirBlock> CODEC = Block.createCodec(BarrierAirBlock::new);

	public BarrierAirBlock(Block.Settings settings) {
		super(settings);
	}

	@Override
	public MapCodec<AirBlock> getCodec() {
		return CODEC;
	}

	@Override
	public Block getPolymerBlock(BlockState state) {
		return Blocks.BARRIER;
	}
}
