package io.github.haykam821.deathswap.game;

import java.util.HashSet;
import java.util.Set;

import io.github.haykam821.deathswap.game.phase.DeathSwapActivePhase;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.player.PlayerIterable;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class EliminationCollector {
	private static final int FADE_IN_TICKS = 20;
	private static final int STAY_TICKS = 10;
	private static final int FADE_OUT_TICKS = 70;

	private static final Formatting CONTINUE_COLOR = Formatting.GREEN;
	private static final Formatting ELIMINATION_COLOR = Formatting.RED;

	private static final Text CONTINUE_TITLE = createText("title", CONTINUE_COLOR, true);
	private static final Text CONTINUE_SUBTITLE = createText("continue.subtitle", CONTINUE_COLOR, false);

	private static final Text ELIMINATION_TITLE = createText("title", ELIMINATION_COLOR, true);
	private static final Text ELIMINATION_SUBTITLE_SINGULAR = createText("elimination.subtitle.singular", ELIMINATION_COLOR, false);

	private static final SoundEvent CONTINUE_SOUND = SoundEvents.ENTITY_PLAYER_LEVELUP;
	private static final SoundEvent ELIMINATION_SOUND = SoundEvents.ENTITY_WITHER_SPAWN;

	private final DeathSwapActivePhase phase;

	private final Set<PlayerRef> eliminated = new HashSet<>();
	private int ticksRemaining;

	public EliminationCollector(DeathSwapActivePhase phase) {
		this.phase = phase;
	}

	public void start() {
		this.eliminated.clear();
		this.ticksRemaining = this.phase.getConfig().getSwapEliminationCollectionTicks();
	}

	/**
	 * @return whether the player's elimination display was integrated into an active elimination collector
	 */
	public boolean add(ServerPlayerEntity player) {
		if (this.ticksRemaining > 0) {
			this.eliminated.add(PlayerRef.of(player));
			return true;
		}

		return false;
	}

	public void tick() {
		if (this.ticksRemaining > 0) {
			this.ticksRemaining -= 1;

			if (this.ticksRemaining == 0) {
				this.showResults();
			}
		}
	}

	private void showResults() {
		PlayerIterable players = this.phase.getGameSpace().getPlayers();

		players.showTitle(this.getTitle(), this.getSubtitle(), STAY_TICKS, FADE_OUT_TICKS, FADE_IN_TICKS);
		players.playSound(this.getSound());
	}

	private Text getTitle() {
		return this.isContinue() ? CONTINUE_TITLE : ELIMINATION_TITLE;
	}

	private Text getSubtitle() {
		if (this.isContinue()) {
			return CONTINUE_SUBTITLE;
		}

		int size = this.eliminated.size();
		return size == 1 ? ELIMINATION_SUBTITLE_SINGULAR : createText("elimination.subtitle", ELIMINATION_COLOR, false, size);
	}

	private SoundEvent getSound() {
		return this.isContinue() ? CONTINUE_SOUND : ELIMINATION_SOUND;
	}

	private boolean isContinue() {
		return this.eliminated.isEmpty();
	}

	@Override
	public String toString() {
		return "EliminationCollector{phase=" + this.phase + ", eliminated=" + this.eliminated + ", ticksRemaining=" + this.ticksRemaining + "}";
	}

	private static Text createText(String keySuffix, Formatting formatting, boolean title, Object... args) {
		return Text.translatable("text.deathswap.elimination_collector." + keySuffix, args).styled(style -> {
			return style.withFormatting(formatting).withBold(title);
		});
	}
}
