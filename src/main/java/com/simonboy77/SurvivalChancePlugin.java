// https://github.com/molo-pl/runelite-plugins/tree/life-saving-jewellery
// https://github.com/donth77/loot-lookup-plugin/blob/master/src/main/java/com/lootlookup/osrswiki/WikiScraper.java

package com.simonboy77;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import net.runelite.client.ui.overlay.OverlayManager;

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
	private OverlayManager overlayManager;

	@Inject
	private SurvivalChanceOverlay overlay;

	@Inject
	private SurvivalChanceConfig config;

	private PlayerState playerState;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		playerState = new PlayerState(client);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
	}

	private void log(String text)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
	}

	@Subscribe
	public void onInteractingChanged(InteractingChanged event)
	{
		// (healthRatio == 0) == opponent died , (healthRatio == -1) == npc has no health
		/*if (event.getSource() == client.getLocalPlayer())  {
			if(event.getTarget().getHealthRatio() > 0) {
				playerState.addOpponent(event.getTarget());
				playerState.calcSurvivalChance(4, 3);
			}
		}*/

		// All that matters is who is attacking the player
		if(event.getTarget() == client.getLocalPlayer())
		{
			// NOTE: healthRatio/Scale are only visible after the player has attacked
			// TODO: Check name/id to see if its in the monster.json file
			boolean isLegitOpponent = true;
			log("Player is interacted with");

			if(isLegitOpponent) {
				playerState.addOpponent(event.getSource());
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (playerState.opponents.length > 0) {
			playerState.updateOpponents();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		playerState.statChanged(statChanged.getSkill());
	}

	/*@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", counter + " onMenuEntryAdded", null);
		counter++;
	}*/

	// This gets called after login
	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		// Check if changed container is equipment
		if(event.getItemContainer().getId() == InventoryID.EQUIPMENT.getId())
		{
			playerState.wearingRingOfLife = event.getItemContainer().contains(RING_OF_LIFE);
			playerState.wearingDefenceCape = event.getItemContainer().contains(DEFENCE_CAPE);
			playerState.wearingPhoenixNecklace = event.getItemContainer().contains(PHOENIX_NECKLACE);
		}
	}

	@Provides
	SurvivalChanceConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SurvivalChanceConfig.class);
	}
}
