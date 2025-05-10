package io.github.haykam821.deathswap.game.phase;

import io.github.haykam821.deathswap.game.DeathSwapConfig;
import io.github.haykam821.deathswap.game.map.DeathSwapMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionType;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class DeathSwapWaitingPhase implements PlayerAttackEntityEvent, GamePlayerEvents.Accept, PlayerDamageEvent, PlayerDeathEvent, GameActivityEvents.RequestStart {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final DeathSwapMap map;
	private final DeathSwapConfig config;

	public DeathSwapWaitingPhase(GameSpace gameSpace, ServerWorld world, DeathSwapMap map, DeathSwapConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<DeathSwapConfig> context) {
		DeathSwapConfig config = context.config();

		RegistryEntry<DimensionType> dimensionType = config.getMapConfig().getDimensionOptions().dimensionTypeEntry();
		DeathSwapMap map = new DeathSwapMap(context.server(), config.getMapConfig(), dimensionType.value());

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setSeed(RandomSeed.getSeed())
			.setDimensionType(dimensionType)
			.setGenerator(map.getChunkGenerator())
			.setGameRule(GameRules.DO_MOB_SPAWNING, true)
			.setDifficulty(Difficulty.NORMAL);

		return context.openWithWorld(worldConfig, (activity, world) -> {
			DeathSwapWaitingPhase phase = new DeathSwapWaitingPhase(activity.getGameSpace(), world, map, config);
			GameWaitingLobby.addTo(activity, config.getPlayerConfig());

			// Rules
			activity.deny(GameRuleType.BLOCK_DROPS);
			activity.deny(GameRuleType.CRAFTING);
			activity.deny(GameRuleType.FALL_DAMAGE);
			activity.deny(GameRuleType.HUNGER);
			activity.deny(GameRuleType.INTERACTION);
			activity.deny(GameRuleType.PORTALS);
			activity.deny(GameRuleType.PVP);
			activity.deny(GameRuleType.THROW_ITEMS);

			// Listeners
			activity.listen(PlayerAttackEntityEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.ACCEPT, phase);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(PlayerDamageEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GameActivityEvents.REQUEST_START, phase);
		});
	}

	// Listeners
	@Override
	public EventResult onAttackEntity(ServerPlayerEntity attacker, Hand hand, Entity attacked, EntityHitResult hitResult) {
		return EventResult.DENY;
	}

	@Override
	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.world, DeathSwapActivePhase.getCenterPos(this.world, this.map, this.config.getMapConfig())).thenRunForEach(player -> {
			player.setBodyYaw(DeathSwapActivePhase.getSpawnYaw(world));
			player.changeGameMode(GameMode.ADVENTURE);
		});
	}

	@Override
	public EventResult onDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return EventResult.DENY;
	}

	@Override
	public EventResult onDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		DeathSwapActivePhase.spawnAtCenter(this.world, this.map, this.config.getMapConfig(), player);
		player.setHealth(player.getMaxHealth());

		return EventResult.DENY;
	}

	@Override
	public GameResult onRequestStart() {
		DeathSwapActivePhase.open(this.gameSpace, this.world, this.map, this.config);
		return GameResult.ok();
	}
}
