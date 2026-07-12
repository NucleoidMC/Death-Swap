package io.github.haykam821.deathswap.game.phase;

import io.github.haykam821.deathswap.game.DeathSwapConfig;
import io.github.haykam821.deathswap.game.map.DeathSwapMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.dimension.DimensionType;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
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
	private final ServerLevel level;
	private final DeathSwapMap map;
	private final DeathSwapConfig config;

	public DeathSwapWaitingPhase(GameSpace gameSpace, ServerLevel level, DeathSwapMap map, DeathSwapConfig config) {
		this.gameSpace = gameSpace;
		this.level = level;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<DeathSwapConfig> context) {
		DeathSwapConfig config = context.config();

		Holder<DimensionType> dimensionType = config.getMapConfig().getDimensionOptions().type();
		DeathSwapMap map = new DeathSwapMap(context.server(), config.getMapConfig(), dimensionType.value());

		RuntimeLevelConfig levelConfig = new RuntimeLevelConfig()
			.setSeed(RandomSupport.generateUniqueSeed())
			.setDimensionType(dimensionType)
			.setGenerator(map.getChunkGenerator())
			.setGameRule(GameRules.SPAWN_MOBS, true)
			.setDifficulty(Difficulty.NORMAL);

		return context.openWithLevel(levelConfig, (activity, level) -> {
			DeathSwapWaitingPhase phase = new DeathSwapWaitingPhase(activity.getGameSpace(), level, map, config);
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
	public EventResult onAttackEntity(ServerPlayer attacker, InteractionHand hand, Entity attacked, EntityHitResult hitResult) {
		return EventResult.DENY;
	}

	@Override
	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.level, DeathSwapActivePhase.getCenterPos(this.level, this.map, this.config.getMapConfig())).thenRunForEach(player -> {
			player.setYBodyRot(DeathSwapActivePhase.getSpawnYaw(level));
			player.setGameMode(GameType.ADVENTURE);
		});
	}

	@Override
	public EventResult onDamage(ServerPlayer player, DamageSource source, float amount) {
		return EventResult.DENY;
	}

	@Override
	public EventResult onDeath(ServerPlayer player, DamageSource source) {
		// Respawn player
		DeathSwapActivePhase.spawnAtCenter(this.level, this.map, this.config.getMapConfig(), player);
		player.setHealth(player.getMaxHealth());

		return EventResult.DENY;
	}

	@Override
	public GameResult onRequestStart() {
		DeathSwapActivePhase.open(this.gameSpace, this.level, this.map, this.config);
		return GameResult.ok();
	}
}
