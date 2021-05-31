package io.github.haykam821.deathswap.game.phase;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;

import io.github.haykam821.deathswap.game.DeathSwapConfig;
import io.github.haykam821.deathswap.game.DeathSwapTimer;
import io.github.haykam821.deathswap.game.map.DeathSwapMap;
import io.github.haykam821.deathswap.game.map.DeathSwapMapConfig;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.PlayerRemoveListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

public class DeathSwapActivePhase implements GameOpenListener, GameTickListener, PlayerAddListener, PlayerDeathListener, PlayerRemoveListener {
	private final GameSpace gameSpace;
	private final DeathSwapMap map;
	private final DeathSwapConfig config;
	private final Set<ServerPlayerEntity> players;
	private final DeathSwapTimer timer;
	private boolean singleplayer;

	public DeathSwapActivePhase(GameSpace gameSpace, GlobalWidgets widgets, DeathSwapMap map, DeathSwapConfig config, Set<ServerPlayerEntity> players) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
		this.timer = new DeathSwapTimer(this, widgets);
		this.players = players;
	}

	public static void open(GameSpace gameSpace, DeathSwapMap map, DeathSwapConfig config) {
		gameSpace.openGame(game -> {
			GlobalWidgets widgets = new GlobalWidgets(game);
			Set<ServerPlayerEntity> players = Sets.newHashSet(gameSpace.getPlayers());
			DeathSwapActivePhase phase = new DeathSwapActivePhase(gameSpace, widgets, map, config, players);

			// Rules
			game.setRule(GameRule.BLOCK_DROPS, RuleResult.ALLOW);
			game.setRule(GameRule.CRAFTING, RuleResult.ALLOW);
			game.setRule(GameRule.FALL_DAMAGE, RuleResult.ALLOW);
			game.setRule(GameRule.HUNGER, RuleResult.ALLOW);
			game.setRule(GameRule.PORTALS, RuleResult.DENY);
			game.setRule(GameRule.PVP, RuleResult.DENY);

			// Listeners
			game.on(GameOpenListener.EVENT, phase);
			game.on(GameTickListener.EVENT, phase);
			game.on(PlayerAddListener.EVENT, phase);
			game.on(PlayerDeathListener.EVENT, phase);
			game.on(PlayerRemoveListener.EVENT, phase);
		});
	}

	// Listeners
	@Override
	public void onOpen() {
		this.singleplayer = this.players.size() == 1;

		for (ServerPlayerEntity player : this.players) {
			player.setGameMode(GameMode.SURVIVAL);
			DeathSwapActivePhase.spawn(this.gameSpace.getWorld(), this.map, this.config.getMapConfig(), player);
		}
	}
	
	@Override
	public void onTick() {
		this.timer.tick();

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
			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	@Override
	public void onAddPlayer(ServerPlayerEntity player) {
		if (!this.players.contains(player)) {
			this.setSpectator(player);
			DeathSwapActivePhase.spawnAtCenter(this.gameSpace.getWorld(), this.map, this.config.getMapConfig(), player);
		}
	}

	@Override
	public void onRemovePlayer(ServerPlayerEntity player) {
		this.eliminate(player, true);
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		if (this.players.contains(player) && this.gameSpace.getWorld().getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES)) {
			Text message = player.getDamageTracker().getDeathMessage().shallowCopy().formatted(Formatting.RED);
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

	// Utilities
	private MutableText getEndingMessage() {
		if (this.players.size() == 1) {
			ServerPlayerEntity winner = this.players.iterator().next();
			return new TranslatableText("text.deathswap.win", winner.getDisplayName());
		}
		return new TranslatableText("text.deathswap.win.none");
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
			this.sendEliminateMessage(player, suffix);
		}

		return removed;
	}

	private void sendEliminateMessage(ServerPlayerEntity player, String suffix) {
		Text message = new TranslatableText("text.deathswap.eliminated" + suffix, player.getDisplayName()).formatted(Formatting.RED);
		this.gameSpace.getPlayers().sendMessage(message);
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.setGameMode(GameMode.SPECTATOR);
	}

	public static void spawn(ServerWorld world, DeathSwapMap map, DeathSwapMapConfig mapConfig, ServerPlayerEntity player) {
		int x = MathHelper.nextInt(world.getRandom(), map.getBox().minX, map.getBox().maxX);
		int z = MathHelper.nextInt(world.getRandom(), map.getBox().minZ, map.getBox().maxZ);

		int surfaceY = map.getChunkGenerator().getHeight(x, z, Heightmap.Type.WORLD_SURFACE);
		float yaw = world.getRandom().nextInt(3) * 90;

		player.teleport(world, x + 0.5, surfaceY, z + 0.5, yaw, 0);
	}

	public static void spawnAtCenter(ServerWorld world, DeathSwapMap map, DeathSwapMapConfig mapConfig, ServerPlayerEntity player) {
		int x = mapConfig.getX() * 8;
		int z = mapConfig.getZ() * 8;

		int surfaceY = map.getChunkGenerator().getHeight(x, z, Heightmap.Type.WORLD_SURFACE);
		float yaw = world.getRandom().nextInt(3) * 90;

		player.teleport(world, x + 0.5, surfaceY, z + 0.5, yaw, 0);
	}
}
