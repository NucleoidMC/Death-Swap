package io.github.haykam821.deathswap.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import io.github.haykam821.deathswap.game.map.DeathSwapChunkGenerator;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {
	@Shadow
	@Final
	private ChunkGenerator chunkGenerator;

	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/chunk/ChunkGeneratorSettings;createMissingSettings()Lnet/minecraft/world/gen/chunk/ChunkGeneratorSettings;"))
	private ChunkGeneratorSettings useBeaconBreakersChunkGeneratorSettings() {
		if (this.chunkGenerator instanceof DeathSwapChunkGenerator deathSwapChunkGenerator) {
			ChunkGeneratorSettings settings = deathSwapChunkGenerator.getSettings();

			if (settings != null) {
				return settings;
			}
		}

		return ChunkGeneratorSettings.createMissingSettings();
	}
}
