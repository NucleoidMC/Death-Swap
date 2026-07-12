package io.github.haykam821.deathswap.game.phase;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import io.github.haykam821.deathswap.game.DeathSwapConfig;
import io.github.haykam821.deathswap.game.DeathSwapTimer;
import io.github.haykam821.deathswap.game.EliminationCollector;
import io.github.haykam821.deathswap.game.map.DeathSwapMap;
import io.github.haykam821.deathswap.game.map.DeathSwapMapConfig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class DeathSwapActivePhase implements GameActivityEvents.Enable, GameActivityEvents.Tick, GamePlayerEvents.Accept, PlayerDamageEvent, PlayerDeathEvent, GamePlayerEvents.Remove {
	private final GameSpace gameSpace;
	private final ServerLevel level;
	private final DeathSwapMap map;
	private final DeathSwapConfig config;
	private final Set<ServerPlayer> players;
	private final DeathSwapTimer timer;
	private final EliminationCollector eliminationCollector = new EliminationCollector(this);

	private boolean singleplayer;
	private int ticksUntilClose = -1;

	public DeathSwapActivePhase(GameSpace gameSpace, ServerLevel level, GlobalWidgets widgets, DeathSwapMap map, DeathSwapConfig config, Set<ServerPlayer> players) {
		this.gameSpace = gameSpace;
		this.level = level;
		this.map = map;
		this.config = config;
		this.timer = new DeathSwapTimer(this, widgets);
		this.players = players;
	}

	public static void open(GameSpace gameSpace, ServerLevel level, DeathSwapMap map, DeathSwapConfig config) {
		gameSpace.setActivity(activity -> {
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);
			Set<ServerPlayer> players = Sets.newHashSet(gameSpace.getPlayers().participants());
			DeathSwapActivePhase phase = new DeathSwapActivePhase(gameSpace, level, widgets, map, config, players);

			// Rules
			activity.allow(GameRuleType.BLOCK_DROPS);
			activity.allow(GameRuleType.CRAFTING);
			activity.allow(GameRuleType.FALL_DAMAGE);
			activity.allow(GameRuleType.HUNGER);
			activity.deny(GameRuleType.PORTALS);
			activity.deny(GameRuleType.PVP);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase);
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(GamePlayerEvents.ACCEPT, phase);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
			activity.listen(PlayerDamageEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
		});
	}

	// Listeners
	@Override
	public void onEnable() {
		this.singleplayer = this.players.size() == 1;

		for (ServerPlayer player : this.players) {
			player.setGameMode(GameType.SURVIVAL);
			DeathSwapActivePhase.spawn(this.level, this.map, this.config.getMapConfig(), player);
		}

		for (ServerPlayer player : this.gameSpace.getPlayers().spectators()) {
			this.setSpectator(player);
			DeathSwapActivePhase.spawnAtCenter(this.level, this.map, this.config.getMapConfig(), player);
		}
	}
	
	@Override
	public void onTick() {
		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			this.eliminationCollector.tick();

			return;
		}

		this.timer.tick();
		this.eliminationCollector.tick();

		// Eliminate players that are out of bounds
		Iterator<ServerPlayer> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			ServerPlayer player = iterator.next();

			if (!this.map.getBox().isInside(player.blockPosition())) {
				this.eliminate(player, ".out_of_bounds", false);
				iterator.remove();
			}
		}

		// Check for a winner
		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;

			this.gameSpace.getPlayers().sendMessage(this.getEndingMessage().withStyle(ChatFormatting.GOLD));
			this.ticksUntilClose = this.config.getTicksUntilClose().sample(this.level.getRandom());
		}
	}

	@Override
	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.level, DeathSwapActivePhase.getCenterPos(this.level, this.map, this.config.getMapConfig())).thenRunForEach(player -> {
			player.setYBodyRot(DeathSwapActivePhase.getSpawnYaw(level));
			this.setSpectator(player);
		});
	}

	@Override
	public void onRemovePlayer(ServerPlayer player) {
		this.eliminate(player, true);
	}

	@Override
	public EventResult onDamage(ServerPlayer player, DamageSource source, float amount) {
		return this.isGameEnding() ? EventResult.DENY : EventResult.PASS;
	}

	@Override
	public EventResult onDeath(ServerPlayer player, DamageSource source) {
		if (this.isGameEnding()) {
			DeathSwapActivePhase.spawn(this.level, this.map, this.config.getMapConfig(), player);
			return EventResult.DENY;
		}

		if (this.players.contains(player) && this.level.getGameRules().get(GameRules.SHOW_DEATH_MESSAGES)) {
			Component message = player.getCombatTracker().getDeathMessage().copy().withStyle(ChatFormatting.RED);
			this.gameSpace.getPlayers().sendMessage(message);
		}
		this.eliminate(player, true);

		return EventResult.DENY;
	}

	// Getters
	public GameSpace getGameSpace() {
		return this.gameSpace;
	}

	public DeathSwapConfig getConfig() {
		return this.config;
	}

	public Set<ServerPlayer> getPlayers() {
		return this.players;
	}

	public EliminationCollector getEliminationCollector() {
		return this.eliminationCollector;
	}

	// Utilities
	private MutableComponent getEndingMessage() {
		if (this.players.size() == 1) {
			ServerPlayer winner = this.players.iterator().next();
			return Component.translatable("text.deathswap.win", winner.getDisplayName());
		}
		return Component.translatable("text.deathswap.win.none");
	}

	private boolean eliminate(ServerPlayer player, boolean remove) {
		return this.eliminate(player, "", remove);
	}

	private boolean eliminate(ServerPlayer player, String suffix, boolean remove) {
		// Assume removed as caller should handle removal
		boolean removed = true;
		if (remove) {
			removed = this.players.remove(player);
		}

		if (removed) {
			this.setSpectator(player);

			if (!this.eliminationCollector.add(player)) {
				this.sendEliminateMessage(player, suffix);
			}
		}

		return removed;
	}

	private void sendEliminateMessage(ServerPlayer player, String suffix) {
		if (this.isGameEnding()) {
			return;
		}

		Component message = Component.translatable("text.deathswap.eliminated" + suffix, player.getDisplayName()).withStyle(ChatFormatting.RED);
		this.gameSpace.getPlayers().sendMessage(message);
	}

	private void setSpectator(ServerPlayer player) {
		player.setGameMode(GameType.SPECTATOR);
	}

	public static float getSpawnYaw(ServerLevel level) {
		return level.getRandom().nextInt(3) * 90;
	}

	public boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}

	public static void spawn(ServerLevel level, DeathSwapMap map, DeathSwapMapConfig mapConfig, ServerPlayer player) {
		int x = Mth.nextInt(level.getRandom(), map.getBox().minX(), map.getBox().maxX());
		int z = Mth.nextInt(level.getRandom(), map.getBox().minZ(), map.getBox().maxZ());

		int surfaceY = map.getSurfaceY(level, x, z);
		float yaw = DeathSwapActivePhase.getSpawnYaw(level);

		player.teleportTo(level, x + 0.5, surfaceY, z + 0.5, Set.of(), yaw, 0, true);
	}

	public static Vec3 getCenterPos(ServerLevel level, DeathSwapMap map, DeathSwapMapConfig mapConfig) {
		int x = mapConfig.getX() * 8;
		int z = mapConfig.getZ() * 8;

		int surfaceY = map.getSurfaceY(level, x, z);

		return new Vec3(x + 0.5, surfaceY, z + 0.5);
	}

	public static void spawnAtCenter(ServerLevel level, DeathSwapMap map, DeathSwapMapConfig mapConfig, ServerPlayer player) {
		Vec3 pos = DeathSwapActivePhase.getCenterPos(level, map, mapConfig);
		float yaw = DeathSwapActivePhase.getSpawnYaw(level);

		player.teleportTo(level, pos.x(), pos.y(), pos.z(), Set.of(), yaw, 0, true);
	}
}
