package io.github.haykam821.deathswap.game;

import com.google.common.collect.Iterables;

import io.github.haykam821.deathswap.game.phase.DeathSwapActivePhase;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.widget.BossBarWidget;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

public class DeathSwapTimer {
	private static final BossBar.Style STYLE = BossBar.Style.PROGRESS;
	private static final BossBar.Color NO_SWAP_COLOR = BossBar.Color.GREEN;
	private static final BossBar.Color WARNING_COLOR = BossBar.Color.YELLOW;
	private static final Formatting NO_SWAP_FORMATTING = Formatting.GREEN;
	private static final Formatting WARNING_FORMATTING = Formatting.YELLOW;
	private static final Text NO_SWAP_TITLE = new TranslatableText("text.deathswap.timer.no_swap");

	private final DeathSwapActivePhase phase;
	private final BossBarWidget widget;
	private int swapTicks;

	public DeathSwapTimer(DeathSwapActivePhase phase, GlobalWidgets widgets) {
		this.phase = phase;
		this.swapTicks = this.phase.getConfig().getInitialSwapTicks();

		this.widget = widgets.addBossBar(this.getBarTitle(NO_SWAP_TITLE, NO_SWAP_FORMATTING), NO_SWAP_COLOR, STYLE);
		this.phase.getGameSpace().addResource(this.widget);
	}

	public void tick() {
		this.swapTicks -= 1;
		if (this.swapTicks == 0) {
			this.swap();
			this.updateNoSwapBar();
		} else if (this.swapTicks < this.phase.getConfig().getSwapWarningTicks() && this.swapTicks % 20 == 0) {
			this.updateWarningBar();
		}
	}

	private void swap() {
		ServerPlayerEntity previousPlayer = Iterables.getLast(this.phase.getPlayers());
		double previousX = previousPlayer.getX();
		double previousY = previousPlayer.getY();
		double previousZ = previousPlayer.getZ();
		
		for (ServerPlayerEntity player : this.phase.getPlayers()) {
			player.teleport(previousX, previousY, previousZ);

			Text message = new TranslatableText("text.deathswap.timer.swap", previousPlayer.getDisplayName()).formatted(NO_SWAP_FORMATTING);
			player.sendMessage(message, true);

			previousPlayer = player;
			previousX = player.getX();
			previousY = player.getY();
			previousZ = player.getZ();
		} 

		this.swapTicks = this.phase.getConfig().getSwapTicks();
	}

	private void updateNoSwapBar() {
		this.widget.setStyle(NO_SWAP_COLOR, STYLE);
		this.widget.setProgress(1);
		this.setBarTitle(NO_SWAP_TITLE, NO_SWAP_FORMATTING);
	}

	private void updateWarningBar() {
		this.widget.setStyle(WARNING_COLOR, STYLE);

		int swapWarningTicks = this.phase.getConfig().getSwapWarningTicks();
		this.widget.setProgress((swapWarningTicks - this.swapTicks) / (float) swapWarningTicks);

		this.setBarTitle(new TranslatableText("text.deathswap.timer.warning", this.swapTicks / 20), WARNING_FORMATTING);
	}

	private void setBarTitle(Text customText, Formatting formatting) {
		this.widget.setTitle(this.getBarTitle(customText, formatting));
	}

	private Text getBarTitle(Text customText, Formatting formatting) {
		Text gameName = new TranslatableText("gameType.deathswap.death_swap").formatted(Formatting.BOLD);
		return new LiteralText("").append(gameName).append(" - ").append(customText).formatted(formatting);
	}
}
