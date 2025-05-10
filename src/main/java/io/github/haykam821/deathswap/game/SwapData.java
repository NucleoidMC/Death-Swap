package io.github.haykam821.deathswap.game;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public record SwapData(ServerWorld world, Vec3d pos, Entity vehicle, List<Entity> passengers) {
	public void apply(ServerPlayerEntity player) {
		player.stopRiding();
		player.teleport(world, this.pos.getX(), this.pos.getY(), this.pos.getZ(), PositionFlag.ROT, 0, 0, false);

		if (this.vehicle != null) {
			player.startRiding(this.vehicle, true);
		}

		for (Entity passenger : this.passengers) {
			passenger.startRiding(player, true);
		}
	}

	public static SwapData from(ServerPlayerEntity player) {
		return new SwapData(player.getServerWorld(), player.getPos(), player.getVehicle(), player.getPassengerList());
	}
}
