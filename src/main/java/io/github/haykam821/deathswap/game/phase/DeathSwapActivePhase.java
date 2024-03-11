package io.github.haykam821.deathswap.game.phase;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import io.github.haykam821.deathswap.game.DeathSwapConfig;
import io.github.haykam821.deathswap.game.DeathSwapTimer;
import io.github.haykam821.deathswap.game.EliminationCollector;
import io.github.haykam821.deathswap.game.map.DeathSwapMap;
import io.github.haykam821.deathswap.game.map.DeathSwapMapConfig;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class DeathSwapActivePhase implements GameActivityEvents.Enable, GameActivityEvents.Tick, GamePlayerEvents.Offer, PlayerDamageEvent, PlayerDeathEvent, GamePlayerEvents.Remove {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final DeathSwapMap map;
	private final DeathSwapConfig config;
	private final Set<ServerPlayerEntity> players;
	private final DeathSwapTimer timer;
	private final EliminationCollector eliminationCollector = new EliminationCollector(this);

	private boolean singleplayer;
	private int ticksUntilClose = -1;

	public DeathSwapActivePhase(GameSpace gameSpace, ServerWorld world, GlobalWidgets widgets, DeathSwapMap map, DeathSwapConfig config, Set<ServerPlayerEntity> players) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
		this.timer = new DeathSwapTimer(this, widgets);
		this.players = players;
	}

	public static void open(GameSpace gameSpace, ServerWorld world, DeathSwapMap map, DeathSwapConfig config) {
		gameSpace.setActivity(activity -> {
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);
			Set<ServerPlayerEntity> players = Sets.newHashSet(gameSpace.getPlayers());
			DeathSwapActivePhase phase = new DeathSwapActivePhase(gameSpace, world, widgets, map, config, players);

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
			activity.listen(GamePlayerEvents.OFFER, phase);
			activity.listen(PlayerDamageEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
		});
	}

	// Listeners
	@Override
	public void onEnable() {
		this.singleplayer = this.players.size() == 1;

		for (ServerPlayerEntity player : this.players) {
			player.changeGameMode(GameMode.SURVIVAL);
			DeathSwapActivePhase.spawn(this.world, this.map, this.config.getMapConfig(), player);
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
		Iterator<ServerPlayerEntity> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			ServerPlayerEntity player = iterator.next();

			if (!this.map.getBox().contains(player.getBlockPos())) {
				this.eliminate(player, ".out_of_bounds", false);
				iterator.remove();
			}
		}

		// Check for a winner
		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;

			this.gameSpace.getPlayers().sendMessage(this.getEndingMessage().formatted(Formatting.GOLD));
			this.ticksUntilClose = this.config.getTicksUntilClose().get(this.world.getRandom());
		}
	}

	@Override
	public PlayerOfferResult onOfferPlayer(PlayerOffer offer) {
		return offer.accept(this.world, DeathSwapActivePhase.getCenterPos(this.world, this.map, this.config.getMapConfig())).and(() -> {
			offer.player().setBodyYaw(DeathSwapActivePhase.getSpawnYaw(world));
			this.setSpectator(offer.player());
		});
	}

	@Override
	public void onRemovePlayer(ServerPlayerEntity player) {
		this.eliminate(player, true);
	}

	@Override
	public ActionResult onDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return this.isGameEnding() ? ActionResult.FAIL : ActionResult.PASS;
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		if (this.isGameEnding()) {
			DeathSwapActivePhase.spawn(this.world, this.map, this.config.getMapConfig(), player);
			return ActionResult.FAIL;
		}

		if (this.players.contains(player) && this.world.getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES)) {
			Text message = player.getDamageTracker().getDeathMessage().copy().formatted(Formatting.RED);
			this.gameSpace.getPlayers().sendMessage(message);
		}
		this.eliminate(player, true);

		return ActionResult.FAIL;
	}

	// Getters
	public GameSpace getGameSpace() {
		return this.gameSpace;
	}

	public DeathSwapConfig getConfig() {
		return this.config;
	}

	public Set<ServerPlayerEntity> getPlayers() {
		return this.players;
	}

	public EliminationCollector getEliminationCollector() {
		return this.eliminationCollector;
	}

	// Utilities
	private MutableText getEndingMessage() {
		if (this.players.size() == 1) {
			ServerPlayerEntity winner = this.players.iterator().next();
			return Text.translatable("text.deathswap.win", winner.getDisplayName());
		}
		return Text.translatable("text.deathswap.win.none");
	}

	private boolean eliminate(ServerPlayerEntity player, boolean remove) {
		return this.eliminate(player, "", remove);
	}

	private boolean eliminate(ServerPlayerEntity player, String suffix, boolean remove) {
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

	private void sendEliminateMessage(ServerPlayerEntity player, String suffix) {
		if (this.isGameEnding()) {
			return;
		}

		Text message = Text.translatable("text.deathswap.eliminated" + suffix, player.getDisplayName()).formatted(Formatting.RED);
		this.gameSpace.getPlayers().sendMessage(message);
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	public static float getSpawnYaw(ServerWorld world) {
		return world.getRandom().nextInt(3) * 90;
	}

	public boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}

	public static void spawn(ServerWorld world, DeathSwapMap map, DeathSwapMapConfig mapConfig, ServerPlayerEntity player) {
		int x = MathHelper.nextInt(world.getRandom(), map.getBox().getMinX(), map.getBox().getMaxX());
		int z = MathHelper.nextInt(world.getRandom(), map.getBox().getMinZ(), map.getBox().getMaxZ());

		int surfaceY = map.getSurfaceY(world, x, z);
		float yaw = DeathSwapActivePhase.getSpawnYaw(world);

		player.teleport(world, x + 0.5, surfaceY, z + 0.5, yaw, 0);
	}

	public static Vec3d getCenterPos(ServerWorld world, DeathSwapMap map, DeathSwapMapConfig mapConfig) {
		int x = mapConfig.getX() * 8;
		int z = mapConfig.getZ() * 8;

		int surfaceY = map.getSurfaceY(world, x, z);

		return new Vec3d(x + 0.5, surfaceY, z + 0.5);
	}

	public static void spawnAtCenter(ServerWorld world, DeathSwapMap map, DeathSwapMapConfig mapConfig, ServerPlayerEntity player) {
		Vec3d pos = DeathSwapActivePhase.getCenterPos(world, map, mapConfig);
		float yaw = DeathSwapActivePhase.getSpawnYaw(world);

		player.teleport(world, pos.getX(), pos.getY(), pos.getZ(), yaw, 0);
	}
}
