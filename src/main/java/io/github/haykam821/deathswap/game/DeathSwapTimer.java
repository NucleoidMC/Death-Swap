package io.github.haykam821.deathswap.game;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.common.collect.Iterables;

import io.github.haykam821.deathswap.game.phase.DeathSwapActivePhase;
import net.minecraft.SharedConstants;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.BossBarWidget;

public class DeathSwapTimer {
	private static final BossEvent.BossBarOverlay STYLE = BossEvent.BossBarOverlay.PROGRESS;
	private static final BossEvent.BossBarColor NO_SWAP_COLOR = BossEvent.BossBarColor.GREEN;
	private static final BossEvent.BossBarColor WARNING_COLOR = BossEvent.BossBarColor.YELLOW;
	private static final ChatFormatting NO_SWAP_FORMATTING = ChatFormatting.GREEN;
	private static final ChatFormatting WARNING_FORMATTING = ChatFormatting.YELLOW;
	private static final Component NO_SWAP_TITLE = Component.translatable("text.deathswap.timer.no_swap");

	private static final DecimalFormat MINUTES_FORMAT = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.ROOT));
	private final DeathSwapActivePhase phase;
	private final BossBarWidget widget;

	private int swapTicks;
	private int warningTicks;

	public DeathSwapTimer(DeathSwapActivePhase phase, GlobalWidgets widgets) {
		this.phase = phase;

		this.swapTicks = this.phase.getConfig().getInitialSwapTicks();
		this.warningTicks = this.swapTicks;

		this.widget = widgets.addBossBar(this.getBarTitle(this.getWarning(), WARNING_FORMATTING), WARNING_COLOR, STYLE);
		this.widget.setProgress(0);
	}

	public void tick() {
		this.swapTicks -= 1;
		if (this.swapTicks == 0) {
			this.swap();
			this.updateNoSwapBar();
		} else if (this.swapTicks < this.warningTicks && this.swapTicks % SharedConstants.TICKS_PER_SECOND == 0) {
			this.updateWarningBar();
		}
	}

	private void swap() {
		// Collect new positions for each player
		List<SwapData> swapData = new ArrayList<>(this.phase.getPlayers().size());

		ServerPlayer previousPlayer = Iterables.getLast(this.phase.getPlayers());
		swapData.add(SwapData.from(previousPlayer));
		
		for (ServerPlayer player : this.phase.getPlayers()) {
			// Ensure positions are off by one
			if (player != previousPlayer) {
				swapData.add(SwapData.from(player));
			}
		}

		// Teleport players to their new positions
		int index = 0;
		for (ServerPlayer player : this.phase.getPlayers()) {
			swapData.get(index).apply(player);

			Component message = Component.translatable("text.deathswap.timer.swap", previousPlayer.getDisplayName()).withStyle(NO_SWAP_FORMATTING);
			player.sendSystemMessage(message, true);

			previousPlayer = player;
			index += 1;
		} 

		this.swapTicks = this.phase.getConfig().getSwapTicks();
		this.warningTicks = this.phase.getConfig().getSwapWarningTicks();

		this.phase.getEliminationCollector().start();
	}

	private void updateNoSwapBar() {
		this.widget.setStyle(NO_SWAP_COLOR, STYLE);
		this.widget.setProgress(1);
		this.setBarTitle(NO_SWAP_TITLE, NO_SWAP_FORMATTING);
	}

	private void updateWarningBar() {
		this.widget.setStyle(WARNING_COLOR, STYLE);
		this.widget.setProgress((this.warningTicks - this.swapTicks) / (float) this.warningTicks);

		this.setBarTitle(this.getWarning(), WARNING_FORMATTING);
	}

	private Component getWarning() {
		int seconds = this.swapTicks / SharedConstants.TICKS_PER_SECOND;

		if (seconds >= 60) {
			double minutes = seconds / 60d;

			if (minutes > 1.05) {
				return Component.translatable("text.deathswap.timer.warning.minutes", MINUTES_FORMAT.format(minutes));
			} else {
				return Component.translatable("text.deathswap.timer.warning.minute");
			}
		} else if (seconds == 1) {
			return Component.translatable("text.deathswap.timer.warning.second");
		} else {
			return Component.translatable("text.deathswap.timer.warning.seconds", seconds);
		}
	}

	private void setBarTitle(Component customText, ChatFormatting formatting) {
		this.widget.setTitle(this.getBarTitle(customText, formatting));
	}

	private Component getBarTitle(Component customText, ChatFormatting formatting) {
		Component gameName = Component.translatable("gameType.deathswap.death_swap").withStyle(ChatFormatting.BOLD);
		return Component.empty().append(gameName).append(" - ").append(customText).withStyle(formatting);
	}
}
