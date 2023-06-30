// https://github.com/molo-pl/runelite-plugins/tree/life-saving-jewellery

package com.simonboy77;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

// TESTING
import net.runelite.api.events.GameStateChanged;


@Slf4j
@PluginDescriptor(
	name = "Survival Chance",
	description = "Calculates the chance of survival, escape and death within a given amount of seconds",
	tags = {"combat", "hardcore", "survival","escape","ring of life"}
)
public class SurvivalChancePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SurvivalChanceConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	private PlayerState playerState;

	@Override
	protected void startUp() throws Exception
	{
		this.playerState = new PlayerState(this.client, this.config, this.itemManager);

		for(int hitResultId = HitResult.RESULT_SURVIVE; hitResultId < HitResult.RESULT_AMOUNT; ++hitResultId) {
			this.infoBoxManager.addInfoBox(new ChanceInfoBox(this.client, this, this.config,
					this.playerState, hitResultId, this.getInfoBoxIcon(hitResultId)));
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		this.infoBoxManager.removeIf(t -> t instanceof ChanceInfoBox);
	}

	private BufferedImage getInfoBoxIcon(int resultId)
	{
		String filePath = "";

		try
		{
			switch(resultId)
			{
				case HitResult.RESULT_SURVIVE: { filePath = "icons/survival.png"; } break;
				case HitResult.RESULT_DEATH: { filePath = "icons/death.png"; } break;

				case HitResult.RESULT_ESCAPE:
				{
					if(this.playerState.isWearingEscapeItem()) {
						if(this.config.altEscapeIcon()) {
							filePath = "icons/escape_alt.png";
						}
						else {
							filePath = "icons/escape.png";
						}
					}
					else {
						if(this.config.altEscapeIcon()) {
							filePath = "icons/escape_warning_alt.png";
						}
						else {
							filePath = "icons/escape_warning.png";
						}
					}

				} break;

				case HitResult.USED_PHOENIX:
				{
					if(this.playerState.isWearingPhoenix()) {
						filePath = "icons/phoenix.png";
					}
					else {
						filePath = "icons/phoenix_warning.png";
					}
				} break;

				case HitResult.USED_REDEMPTION:
				{
					if(this.playerState.isRedemptionActive()) {
						filePath = "icons/redemption.png";
					}
					else {
						filePath = "icons/redemption_warning.png";
					}
				} break;
			}

			return ImageIO.read(getClass().getClassLoader().getResource(filePath));
		}
		catch(IOException e)
		{
			log.info("Failed loading buffered image at " + filePath);
		}

		return null;
	}

	private void updateInfoBox(int resultId)
	{
		for(InfoBox infoBox : this.infoBoxManager.getInfoBoxes())
		{
			if(infoBox instanceof ChanceInfoBox)
			{
				if(((ChanceInfoBox)infoBox).getResultId() == resultId) {
					infoBox.setImage(this.getInfoBoxIcon(resultId));
					infoBox.setTooltip(infoBox.getName());
					this.infoBoxManager.updateInfoBoxImage(infoBox);
					break;
				}
			}
		}
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		// All that matters is who is attacking the player
		if(event.getTarget() == client.getLocalPlayer()) {
			this.playerState.addOpponent((NPC)event.getSource());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (this.playerState.isInCombat()) {
			this.playerState.updateOpponents();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		this.playerState.statChanged(event.getSkill());
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		// Check if changed container is equipment
		if(event.getItemContainer().getId() == InventoryID.EQUIPMENT.getId())
		{
			boolean wasWearingEscapeItem = this.playerState.isWearingEscapeItem();
			boolean wasWearingPhoenix = this.playerState.isWearingPhoenix();

			this.playerState.equipmentChanged(event.getItemContainer());

			if(wasWearingEscapeItem != this.playerState.isWearingEscapeItem()) {
				this.updateInfoBox(HitResult.RESULT_ESCAPE);
			}

			if(wasWearingPhoenix != this.playerState.isWearingPhoenix()) {
				this.updateInfoBox(HitResult.USED_PHOENIX);
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		this.playerState.varbitChanged(event.getVarbitId(), event.getVarpId());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		String key = configChanged.getKey();

		if(key.equals("secondsOfCombat")) {
			this.playerState.calcSurvivalChance();
		}
		else if(key.equals("altEscapeIcon")) {
			this.updateInfoBox(HitResult.RESULT_ESCAPE);
		}
	}

	// TESTING
	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN ||
			gameStateChanged.getGameState() == GameState.CONNECTION_LOST)
		{
			log.info("disconnected at tick " + this.client.getTickCount());
		}
	}

	@Provides
	SurvivalChanceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SurvivalChanceConfig.class);
	}
}
