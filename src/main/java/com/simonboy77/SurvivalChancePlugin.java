// https://github.com/molo-pl/runelite-plugins/tree/life-saving-jewellery

package com.simonboy77;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import net.runelite.api.Client;
import net.runelite.api.Skill;
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
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.game.SkillIconManager;

import static net.runelite.api.ItemID.RING_OF_LIFE;
import static net.runelite.api.ItemID.DEFENCE_CAPE;
import static net.runelite.api.ItemID.PHOENIX_NECKLACE;

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

	@Inject
	private SkillIconManager skillIconManager;

	private PlayerState playerState;

	@Override
	protected void startUp() throws Exception
	{
		this.playerState = new PlayerState(this.client, this.config);

		for(int hitResultId = HitResult.RESULT_SURVIVE; hitResultId < HitResult.RESULT_AMOUNT; ++hitResultId) {
			this.infoBoxManager.addInfoBox(new SurvivalChanceInfoBox(this.client, this, this.config,
					this.playerState, hitResultId, this.getInfoBoxIcon(hitResultId)));
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		this.infoBoxManager.removeIf(t -> t instanceof SurvivalChanceInfoBox);
	}

	private BufferedImage getInfoBoxIcon(int resultId)
	{
		String filePath = "";

		try
		{
			switch(resultId)
			{
				case HitResult.RESULT_SURVIVE: { filePath = "icons/survival.png"; } break;
				case HitResult.RESULT_ESCAPE: { filePath = "icons/escape.png"; } break;
				case HitResult.RESULT_DEATH: { filePath = "icons/death.png"; } break;
				case HitResult.USED_PHOENIX: { filePath = "icons/phoenix.png"; } break;
			}

			return ImageIO.read(getClass().getClassLoader().getResource(filePath));
		}
		catch(IOException e)
		{
			log.info("Failed loading buffered image at " + filePath);
		}

		return null;
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
			this.playerState.wearingRingOfLife = event.getItemContainer().contains(RING_OF_LIFE);
			this.playerState.wearingDefenceCape = event.getItemContainer().contains(DEFENCE_CAPE);
			this.playerState.wearingPhoenixNecklace = event.getItemContainer().contains(PHOENIX_NECKLACE);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if(configChanged.getKey().equals("hitTurns"))
		{
			log.info("Changed hitTurns");
			this.playerState.calcSurvivalChance();
		}
	}

	@Provides
	SurvivalChanceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SurvivalChanceConfig.class);
	}
}
