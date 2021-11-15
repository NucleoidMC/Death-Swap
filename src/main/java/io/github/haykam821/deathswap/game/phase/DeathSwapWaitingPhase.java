package io.github.haykam821.deathswap.game.phase;

import io.github.haykam821.deathswap.game.DeathSwapConfig;
import io.github.haykam821.deathswap.game.map.DeathSwapMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.AttackEntityListener;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDamageListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;

public class DeathSwapWaitingPhase implements AttackEntityListener, OfferPlayerListener, PlayerAddListener, PlayerDamageListener, PlayerDeathListener, RequestStartListener {
	private final GameSpace gameSpace;
	private final DeathSwapMap map;
	private final DeathSwapConfig config;

	public DeathSwapWaitingPhase(GameSpace gameSpace, DeathSwapMap map, DeathSwapConfig config) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<DeathSwapConfig> context) {
		DeathSwapConfig config = context.getConfig();
		DeathSwapMap map = new DeathSwapMap(context.getServer(), config.getMapConfig());

		BubbleWorldConfig worldConfig = new BubbleWorldConfig()
			.setGenerator(map.getChunkGenerator())
			.setGameRule(GameRules.DO_MOB_SPAWNING, true)
			.setDifficulty(Difficulty.NORMAL)
			.setDefaultGameMode(GameMode.ADVENTURE);

		return context.createOpenProcedure(worldConfig, game -> {
			DeathSwapWaitingPhase phase = new DeathSwapWaitingPhase(game.getGameSpace(), map, config);
			GameWaitingLobby.applyTo(game, config.getPlayerConfig());

			// Rules
			game.deny(GameRule.BLOCK_DROPS);
			game.deny(GameRule.CRAFTING);
			game.deny(GameRule.FALL_DAMAGE);
			game.deny(GameRule.HUNGER);
			game.deny(GameRule.INTERACTION);
			game.deny(GameRule.PORTALS);
			game.deny(GameRule.PVP);
			game.deny(GameRule.THROW_ITEMS);

			// Listeners
			game.listen(AttackEntityListener.EVENT, phase);
			game.listen(OfferPlayerListener.EVENT, phase);
			game.listen(PlayerAddListener.EVENT, phase);
			game.listen(PlayerDamageListener.EVENT, phase);
			game.listen(PlayerDeathListener.EVENT, phase);
			game.listen(RequestStartListener.EVENT, phase);
		});
	}

	// Listeners
	@Override
	public ActionResult onAttackEntity(ServerPlayerEntity attacker, Hand hand, Entity attacked, EntityHitResult hitResult) {
		return ActionResult.FAIL;
	}

	@Override
	public JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	@Override
	public void onAddPlayer(ServerPlayerEntity player) {
		DeathSwapActivePhase.spawnAtCenter(this.gameSpace.getWorld(), this.map, this.config.getMapConfig(), player);
	}

	@Override
	public ActionResult onDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return ActionResult.FAIL;
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		DeathSwapActivePhase.spawnAtCenter(this.gameSpace.getWorld(), this.map, this.config.getMapConfig(), player);
		player.setHealth(player.getMaxHealth());

		return ActionResult.FAIL;
	}

	@Override
	public StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameSpace.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.NOT_ENOUGH_PLAYERS;
		}

		DeathSwapActivePhase.open(this.gameSpace, this.map, this.config);
		return StartResult.OK;
	}

	// Utilities
	private boolean isFull() {
		return this.gameSpace.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}
}
