// https://github.com/molo-pl/runelite-plugins/tree/life-saving-jewellery

package com.simonboy77;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.NPC;
import net.runelite.api.InventoryID;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

@Slf4j
@PluginDescriptor(
	name = "Survival Chance"
)
public class SurvivalChancePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SurvivalChanceConfig config;

	@Inject
	private InfoBoxManager infoBoxManager;

	private PlayerState playerState;
	//private ChanceInfoBox escapeInfoBox;
	//private ChanceInfoBox phoenixInfoBox;

	@Override
	protected void startUp() throws Exception
	{
		this.playerState = new PlayerState(this.client, this.config);

		//this.escapeInfoBox = new ChanceInfoBox(this.client, this, this.config, this.playerState,
				//HitResult.RESULT_ESCAPE, this.getInfoBoxIcon(HitResult.RESULT_ESCAPE));
		//this.phoenixInfoBox = new ChanceInfoBox(this.client, this, this.config, this.playerState,
				//HitResult.USED_PHOENIX, this.getInfoBoxIcon(HitResult.USED_PHOENIX));

		//this.infoBoxManager.addInfoBox();

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
				case HitResult.RESULT_DEATH: { filePath = "icons/death.png"; } break;
				case HitResult.USED_PHOENIX:
				{
					if(this.playerState.isWearingPhoenix()) {
						filePath = "icons/phoenix.png";
					}
					else {
						filePath = "icons/phoenix_warning.png";
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
	public void onGameTick(GameTick gameTick)
	{
		if (this.playerState.isInCombat()) {
			this.playerState.updateOpponents();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		this.playerState.statChanged(statChanged.getSkill());
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		// Check if changed container is equipment
		if(event.getItemContainer().getId() == InventoryID.EQUIPMENT.getId())
		{
			boolean wasWearingEscapeItem = this.playerState.isWearingEscapeItem();
			boolean wasWearingPhoenix = this.playerState.isWearingPhoenix();

			this.playerState.updateEquipment(event.getItemContainer());

			if(wasWearingEscapeItem != this.playerState.isWearingEscapeItem()) {
				this.updateInfoBox(HitResult.RESULT_ESCAPE);
			}

			if(wasWearingPhoenix != this.playerState.isWearingPhoenix()) {
				this.updateInfoBox(HitResult.USED_PHOENIX);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if(configChanged.getKey().equals("hitTurns")) {
			this.playerState.calcSurvivalChance();
		}
		else if(configChanged.getKey().equals("altEscapeIcon")) {
			this.updateInfoBox(HitResult.RESULT_ESCAPE);
		}
	}

	@Provides
	SurvivalChanceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SurvivalChanceConfig.class);
	}
}