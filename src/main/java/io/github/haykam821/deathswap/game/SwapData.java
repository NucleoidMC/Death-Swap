package io.github.haykam821.deathswap.game;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public record SwapData(ServerLevel world, Vec3 pos, Entity vehicle, List<Entity> passengers) {
	public void apply(ServerPlayer player) {
		player.stopRiding();
		player.teleportTo(world, this.pos.x(), this.pos.y(), this.pos.z(), Relative.ROTATION, 0, 0, false);

		if (this.vehicle != null) {
			player.startRiding(this.vehicle, true, true);
		}

		for (Entity passenger : this.passengers) {
			passenger.startRiding(player, true, true);
		}
	}

	public static SwapData from(ServerPlayer player) {
		return new SwapData(player.level(), player.position(), player.getVehicle(), player.getPassengers());
	}
}
